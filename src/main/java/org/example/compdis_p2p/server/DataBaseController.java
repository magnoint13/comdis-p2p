package org.example.compdis_p2p.server;

import org.example.compdis_p2p.AlreadyExistsException;
import org.example.compdis_p2p.AuthException;
import org.example.compdis_p2p.PtpException;
import org.example.compdis_p2p.client.Client;
import org.example.compdis_p2p.client.ClientInterface;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;

// TODO: https://www.sqlite.org/threadsafe.html
// TODO: añadir amigo
//       al insertar, hacerlo en orden para evitar que se repita
//       Es decir, evitar tuplas ('marcos', 'pepe') y ('pepe', 'marcos')
// TODO: eliminar amigo
public class DataBaseController implements AutoCloseable {
    private static final int TABLE_COUNT = 5;

    private final Connection connection;

    public DataBaseController(String databaseFile, String creationScript) throws SQLException, IOException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile);
        System.out.println("Conexion con SQLite establecida");

        // Configuracion
        createBD(creationScript);
        enableForeignKeys();
    }

    private void createBD(String creationScript) throws IOException {
        try {
            // Iniciar transaccion
            connection.setAutoCommit(false);

            // Realizar una consulta para obtener el número de tablas
            try (Statement stmt = connection.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("select count(*) as count from sqlite_schema;")) {
                    if (rs.next() && rs.getInt("count") == TABLE_COUNT) {
                        connection.commit();
                        return;
                    }
                }
            }

            try (Statement stmt = connection.createStatement()) {
                // Si el numero de tablas no es el adecuado, se ejecuta el script de creacion
                String script = new String(Files.readAllBytes(Paths.get(creationScript)));

                // Dividir las sentencias en caso de múltiples queries
                String[] sentencias = script.split(";");

                for (String cmd : sentencias) {
                    String cmdTrim = cmd.trim();

                    if (!cmdTrim.isEmpty() && !cmdTrim.startsWith("--")) {
                        stmt.addBatch(cmd);
                        System.out.println("Ejecutado: " + cmd);
                    }
                }

                // Ejecutar todas las sentencias de golpe
                stmt.executeBatch();
                connection.commit();
            }

        } catch (SQLException error) {
            rollback();
            PtpException.logError(error);
        } finally {
            resetAutoCommit();
        }
    }

    private void enableForeignKeys() throws SQLException {
        // Por defecto SQLite 3 no comprueba las restricciones de clave foránea.
        // Esto se puede cambiar, pero se debe ejecutar lo siguiente para cada conexion.
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("pragma foreign_keys = on;");
        }
    }

    public void checkUser(ClientInterface client) throws AuthException, RemoteException {
        String smt = """
                select count(*) as count
                from usuarios
                where nombreUsuario = ? and clave = ?;
                """;

        try (PreparedStatement pst = connection.prepareStatement(smt)) {
            pst.setString(1, client.getUsername());
            pst.setString(2, client.getPassword());

            try (ResultSet rs = pst.executeQuery()) {
                // Verifica que solo hay una coincidencia
                if (!rs.next() || rs.getInt("count") != 1) {
                    throw new AuthException("El usuario no existe la contraseña no es correcta");
                }
            }

        } catch (SQLException error) {
            PtpException.logError(error);
        }
    }

    public Collection<ClientInterface> getFriends(ClientInterface client) throws RemoteException {
        ArrayList<ClientInterface> friends = new ArrayList<>();
        String smt = """
                select nombreUsuario2 as friend
                from Usuarios
                     join Amigos on (nombreUsuario = nombreUsuario1)
                where nombreUsuario = ?;
                """;
        try (PreparedStatement pst = connection.prepareStatement(smt)) {
            pst.setString(1, client.getUsername());

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    ClientInterface friend = new Client(rs.getString("friend"), null);
                    friends.add(friend);
                }
            }
        } catch (SQLException error) {
            PtpException.logError(error);
        }

        return friends;
    }

    public void addUser(ClientInterface client) throws RemoteException, AlreadyExistsException {
        try (PreparedStatement pst = connection.prepareStatement("insert into Usuarios values (?, ?);")) {
            pst.setString(1, client.getUsername());
            pst.setString(2, client.getPassword());
            pst.executeUpdate();

        } catch (SQLException error) {
            // https://www.sqlite.org/rescode.html#constraint_primarykey
            if (error instanceof SQLiteException sqlError && sqlError.getResultCode().equals(SQLiteErrorCode.SQLITE_CONSTRAINT_PRIMARYKEY)) {
                // TODO: al realizar el getter, se esta tramitando otra peticion a traves de la red que es innecesaria
                throw new AlreadyExistsException("El usuario \"%s\" ya existe".formatted(client.getUsername()));
            } else {
                PtpException.logError(error);
            }
        }
    }

    public Collection<ClientInterface> searchClientsbyName(String name) throws RemoteException {
        ArrayList<ClientInterface> result = new ArrayList<>();
        String smt = """
                select nombreUsuario as username, clave as password
                from Usuarios
                where nombreUsuario like ?
                """;
        try (PreparedStatement pst = connection.prepareStatement(smt)) {
            pst.setString(1, "%" + name + "%");
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    ClientInterface friend = new Client(rs.getString("username"), rs.getString("password"));
                    result.add(friend);
                }
            }
        } catch (SQLException error) {
            PtpException.logError(error);
        }
        return result;
    }

    public void sendFriendRequest(ClientInterface client, String userName) throws RemoteException {
        //TODO: problemas con el auto increment, no se porque pone null al atributo
        try (PreparedStatement pst = connection.prepareStatement("""
                                                                insert into Solicitudes (nombreUsuario1, nombreUsuario2)
                                                                VALUES (?,?);
                                                                """)) {
            pst.setString(1, client.getUsername()); //Emisor
            pst.setString(2, userName);             //Receptor
            pst.executeUpdate();
        } catch (SQLException error) {
            PtpException.logError(error);
        }
    }

    public Collection<String> getPendingRequests(ClientInterface client) throws RemoteException {
        ArrayList<String> requests = new ArrayList<>();
        String smt = """
                select nombreUsuario1 as other
                from Solicitudes
                where nombreUsuario2 = ? and estado = 'pendiente';
                """;
        try (PreparedStatement pst = connection.prepareStatement(smt)) {
            pst.setString(1, client.getUsername());
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    requests.add(rs.getString("other"));
                }
            }
        } catch (SQLException error) {
            PtpException.logError(error);
        }

        return requests;
    }

    @Override
    public void close() {
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException error) {
            PtpException.logError(error);
        }
    }

    private void resetAutoCommit() {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException error) {
            PtpException.logError(error);
        }
    }

    private void rollback() {
        try {
            connection.rollback();
        } catch (SQLException error) {
            PtpException.logError(error);
        }
    }
}
