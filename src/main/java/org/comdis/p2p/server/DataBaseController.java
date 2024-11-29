package org.comdis.p2p.server;

import org.comdis.p2p.FriendStatus;
import org.comdis.p2p.RemoteClient;
import org.comdis.p2p.exceptions.AlreadyExistsException;
import org.comdis.p2p.exceptions.AuthException;
import org.comdis.p2p.exceptions.NotFoundException;
import org.comdis.p2p.exceptions.PtpException;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;

// TODO: https://www.sqlite.org/threadsafe.html
class DataBaseController implements AutoCloseable {
    private static final int TABLE_COUNT = 3;

    private final Connection connection;

    public DataBaseController(String databaseFile, String creationScript) throws SQLException, IOException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile);
        System.out.println("Conexion con SQLite establecida");

        // Configuracion
        createBD(creationScript);
        enableForeignKeys();
    }

    // ==== CONFIGURACION ==============================================================================================

    private void createBD(String creationScript) throws IOException {
        try {
            // Iniciar transaccion
            connection.setAutoCommit(false);

            // Realizar una consulta para obtener el número de tablas
            try (Statement stmt = connection.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("""
                        select count(*) as count
                        from sqlite_schema
                        where type = 'table'
                        """)
                ) {
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
                        stmt.addBatch(cmdTrim);
                        System.out.println("==== Ejecutar ====\n" + cmdTrim);
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

    // ==== CREAR/BORRAR USUARIOS ======================================================================================

    public void addUser(String username, String password) throws AlreadyExistsException {
        try (PreparedStatement pst = connection.prepareStatement("""
                insert into Usuarios values (?, ?);
                """)
        ) {
            pst.setString(1, username);
            pst.setString(2, password);
            pst.executeUpdate();

        } catch (SQLException error) {
            // https://www.sqlite.org/rescode.html#constraint_primarykey
            if (error instanceof SQLiteException sqlError && sqlError.getResultCode().equals(SQLiteErrorCode.SQLITE_CONSTRAINT_PRIMARYKEY)) {
                throw new AlreadyExistsException("El usuario \"%s\" ya existe".formatted(username));
            } else {
                PtpException.logError(error);
            }
        }
    }

    public void deleteUser(String username, String password) throws AuthException {
        try (PreparedStatement pst = connection.prepareStatement("""
                delete from Usuarios
                where nombreUsuario = ? and clave = ?;
                """)
        ) {
            pst.setString(1, username);
            pst.setString(2, password);
            int rowsAffected = pst.executeUpdate();

            if (rowsAffected == 0) {
                throw new AuthException("El usuario no existe o la contraseña no es correcta");
            }

        } catch (SQLException error) {
            PtpException.logError(error);
        }
    }

    // ==== CONSULTAS ==================================================================================================

    public void checkUserCredentials(String username, String password) throws AuthException {
        try (PreparedStatement pst = connection.prepareStatement("""
                select count(*) as count
                from usuarios
                where nombreUsuario = ? and clave = ?;
                """)
        ) {
            pst.setString(1, username);
            pst.setString(2, password);

            try (ResultSet rs = pst.executeQuery()) {
                // Verifica que solo hay una coincidencia
                if (!rs.next() || rs.getInt("count") != 1) {
                    throw new AuthException("El usuario no existe o la contraseña no es correcta");
                }
            }

        } catch (SQLException error) {
            PtpException.logError(error);
        }
    }

    public boolean userExists(String username) {
        try (PreparedStatement pst = connection.prepareStatement("""
                select count(*) as count
                from Usuarios
                where nombreUsuario = ?;
                """)
        ) {
            pst.setString(1, username);

            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() && rs.getInt("count") == 1;
            }

        } catch (SQLException error) {
            PtpException.logError(error);
            return false;
        }
    }

    public boolean userExists(String username,String clave) {
        try (PreparedStatement pst = connection.prepareStatement("""
                select count(*) as count
                from Usuarios
                where nombreUsuario = ? and clave = ?;
                """)
        ) {
            pst.setString(1, username);
            pst.setString(2, clave);


            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() && rs.getInt("count") == 1;
            }

        } catch (SQLException error) {
            PtpException.logError(error);
            return false;
        }
    }

    public boolean PendindFromOtherExists(String from, String to) {
        try (PreparedStatement pst = connection.prepareStatement("""
                select count(*) as count
                from Solicitudes
                where nombreUsuario1 = ? and nombreUsuario2 = ? ;
                """)
        ) {
            pst.setString(1, from);
            pst.setString(2,to);

            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() && rs.getInt("count") == 1;
            }

        } catch (SQLException error) {
            PtpException.logError(error);
            return false;
        }
    }

    public boolean areFriends(String username1, String username2) {
        try (PreparedStatement pst = connection.prepareStatement("""
                select count(*) as count
                from Amigos
                where nombreUsuario1 = ?
                  and nombreUsuario2 = ?;
                """)
        ) {
            pst.setString(calculateFriendColumn(username1, username2), username1);
            pst.setString(calculateFriendColumn(username2, username1), username2);

            try (ResultSet rs = pst.executeQuery()) {
                return rs.next() && rs.getInt("count") == 1;
            }

        } catch (SQLException error) {
            PtpException.logError(error);
            return false;
        }
    }

    public ArrayList<String> getFriends(String username) {
        ArrayList<String> friends = new ArrayList<>();

        try (PreparedStatement pst = connection.prepareStatement("""
                select iif(
                    nombreUsuario = nombreUsuario1,
                    nombreUsuario2,
                    nombreUsuario1) as friend
                from Usuarios
                     join Amigos on (
                         nombreUsuario = nombreUsuario1 or
                         nombreUsuario = nombreUsuario2)
                where nombreUsuario = ?;
                """)
        ) {
            pst.setString(1, username);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    friends.add(rs.getString("friend"));
                }
            }
        } catch (SQLException error) {
            PtpException.logError(error);
        }

        return friends;
    }

    /**
     * Busca usuarios que contengan la subcadena `username` excepto aquellos que sean `exclude`
     */
    public ArrayList<String> searchUsernames(String username, String exclude) {
        ArrayList<String> result = new ArrayList<>();

        try (PreparedStatement pst = connection.prepareStatement("""
                select nombreUsuario as username
                from Usuarios
                where nombreUsuario like ?
                  and nombreUsuario <> ?
                """)
        ) {
            pst.setString(1, "%" + username + "%");
            pst.setString(2, exclude);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString("username"));
                }
            }
        } catch (SQLException error) {
            PtpException.logError(error);
        }
        return result;
    }

    // ==== PETICIONES DE AMISTAD ======================================================================================

    public void sendFriendRequest(String from, String to) throws NotFoundException, AlreadyExistsException {
        try {
            // Primero, comprobar si el usuario `to` existe
            // Se asume que `from` es correcto, dado que fue el quién envío el mensaje
            if (!userExists(to)) {
                throw new NotFoundException("El usuario \"%s\" no existe".formatted(to));
            }

            if (areFriends(from, to)) {
                throw new AlreadyExistsException("Los usuarios \"%s\" y \"%s\" ya son amigos".formatted(from, to));
            }

            // Realizar la insercion
            try (PreparedStatement pst = connection.prepareStatement("""
                    insert into Solicitudes (emisor, receptor) values (?, ?);
                    """)
            ) {
                pst.setString(1, from);
                pst.setString(2, to);
                pst.executeUpdate();
            }
        } catch (SQLException error) {
            // https://www.sqlite.org/rescode.html#constraint_primarykey
            if (error instanceof SQLiteException sqlError && sqlError.getResultCode().equals(SQLiteErrorCode.SQLITE_CONSTRAINT_PRIMARYKEY)) {
                throw new AlreadyExistsException("Ya existe la solicitud entre el usuario \"%s\" y \"%s\"".formatted(from, to));
            } else {
                PtpException.logError(error);
            }
        }
    }

    public void acceptFriendRequest(String accepts, String originalSender) throws NotFoundException, AlreadyExistsException {
        try {
            // Iniciar una transaccion
            connection.setAutoCommit(false);

            // Primero, comprobar si el usuario `originalSender` existe
            // Se asume que `accepts` es correcto, dado que fue el quién envío el mensaje
            if (!userExists(originalSender)) {
                rollback();
                throw new NotFoundException("El usuario \"%s\" no existe".formatted(originalSender));
            }

            // Luego, actualizar el estado de la solicitud
            // TODO: borrar la solicitud en su lugar?
            try (PreparedStatement pst = connection.prepareStatement("""
                    update Solicitudes
                    set estado = ? -- aceptada
                    where emisor = ?
                      and receptor = ?
                      and estado = ?; -- pendiente
                    """)
            ) {
                pst.setString(1, FriendStatus.ACCEPTED.toString());
                pst.setString(2, originalSender);
                pst.setString(3, accepts);
                pst.setString(4, FriendStatus.PENDING.toString());

                int affectedRows = pst.executeUpdate();
                if (affectedRows == 0) {
                    rollback();
                    throw new NotFoundException(
                            "No se ha encontrado una solicitud de amistad pendiente entre \"%s\" y \"%s\""
                                    .formatted(originalSender, accepts)
                    );
                }
            }

            // Finalmente, crear la amistad
            try (PreparedStatement pst = connection.prepareStatement("""
                    insert into Amigos (nombreUsuario1, nombreUsuario2)
                    values (?, ?);
                    """)
            ) {
                pst.setString(calculateFriendColumn(accepts, originalSender), accepts);
                pst.setString(calculateFriendColumn(originalSender, accepts), originalSender);
                pst.executeUpdate();
            }

            // Completar la transaccion
            connection.commit();

        } catch (SQLException error) {
            rollback();

            // https://www.sqlite.org/rescode.html#constraint_primarykey
            if (error instanceof SQLiteException sqlError && sqlError.getResultCode().equals(SQLiteErrorCode.SQLITE_CONSTRAINT_PRIMARYKEY)) {
                throw new AlreadyExistsException("Ya existe la amistad entre el usuario \"%s\" y \"%s\"".formatted(originalSender, accepts));
            } else {
                PtpException.logError(error);
            }
        } finally {
            resetAutoCommit();
        }
    }

    public void cancelFriendRequest(String cancels, String other) throws NotFoundException {
        try (PreparedStatement pst = connection.prepareStatement("""
                update Solicitudes
                set estado = ? -- cancelada
                where (emisor = ? and receptor = ?)
                   or (receptor = ? and emisor = ?)
                  and estado = ?; -- pendiente
                """)
        ) {
            pst.setString(1, FriendStatus.REJECTED.toString());
            pst.setString(2, cancels);
            pst.setString(3, other);
            pst.setString(4, cancels);
            pst.setString(5, other);
            pst.setString(6, FriendStatus.PENDING.toString());

            int affectedRows = pst.executeUpdate();
            if (affectedRows == 0) {
                rollback();
                throw new NotFoundException(
                        "No se ha encontrado una solicitud de amistad pendiente entre \"%s\" y \"%s\""
                                .formatted(cancels, other)
                );
            }
        } catch (SQLException error) {
            PtpException.logError(error);
        }
    }

    /**
     * Peticiones pendientes de aceptar (que me han enviado)
     */
    public ArrayList<String> getPendingRequests(String username) {
        ArrayList<String> requests = new ArrayList<>();

        try (PreparedStatement pst = connection.prepareStatement("""
                select emisor
                from Solicitudes
                where receptor = ?
                  and estado = ?; -- pendiente
                """)
        ) {
            pst.setString(1, username);
            pst.setString(2, FriendStatus.PENDING.toString());

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    requests.add(rs.getString("emisor"));
                }
            }
        } catch (SQLException error) {
            PtpException.logError(error);
        }

        return requests;
    }

    // TODO: eliminar amigo

    // ==== AJUSTES ================================================================================================

    public void changePassword(String username, String oldpassword, String newpassword) throws AuthException {
        if (!userExists(username,oldpassword)) {
            throw new AuthException("Ha ocurrido un error al intentar cambiar la clave.La contraseña es incorrecta");
        }
        try (PreparedStatement pst = connection.prepareStatement("""
                update Usuarios
                set clave = ?
                where nombreUsuario = ?;
                """)
        ) {
            pst.setString(1, newpassword);
            pst.setString(2, username);
            pst.executeUpdate();
        } catch (SQLException error) {
            PtpException.logError(error);
        }
    }

    // ==== COMODIDADES ================================================================================================

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

    /**
     * Como en SQLite no se pueden implementar triggers, es necesario disponer de un mecanismo
     * que asegure que no se pueden dar las tuplas (A, B) y (B, A) simultaneamente en la tabla
     * de amigos.
     * <br><br>
     * Para arreglarlo, se insertan por orden alfabetico. Esta funcion determina la columna de
     * cada donde se insertara `toInsert` frente a su amigo `compare`.
     */
    private int calculateFriendColumn(String toInsert, String compare) {
        return toInsert.compareToIgnoreCase(compare) > 0 ? 1 : 2;
    }



}
