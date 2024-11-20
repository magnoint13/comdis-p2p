package org.example.compdis_p2p;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class BaseDatosController {

    private Connection con;


    public Connection getConnection() {
        return con;
    }

    public void conectarBD(){
        // URL base de datos
        String url = "jdbc:sqlite:P2P.db";
        try{
            con = DriverManager.getConnection(url);
            System.out.println("Connection to SQLite has been established.");

            // Crear las tablas si el archivo .db es nuevo
            String sqlUsuarios = "CREATE TABLE IF NOT EXISTS Usuarios (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "nombreUsuario TEXT NOT NULL," +
                    "clave TEXT NOT NULL" +
                    ");";

            String sqlAmigos = "CREATE TABLE IF NOT EXISTS Amigos (" +
                    "id1 INTEGER NOT NULL," +
                    "id2 INTEGER NOT NULL," +
                    "PRIMARY KEY (id1, id2)," +
                    "FOREIGN KEY (id1) REFERENCES Usuarios(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (id2) REFERENCES Usuarios(id) ON DELETE CASCADE" +
                    ");";

            String sqlSolicitudes = "CREATE TABLE IF NOT EXISTS Solicitudes (" +
                    "id_solicitud INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "id1 INTEGER NOT NULL," +
                    "id2 INTEGER NOT NULL," +
                    "estado TEXT DEFAULT 'pendiente'," +
                    "fecha_solicitud DATE DEFAULT CURRENT_DATE," +
                    "FOREIGN KEY (id1) REFERENCES Usuarios(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (id2) REFERENCES Usuarios(id) ON DELETE CASCADE" +
                    ");";


            // Ejecutar las consultas para crear tablas
            Statement stmt = con.createStatement();
            stmt.execute(sqlUsuarios);
            stmt.executeUpdate(sqlAmigos);
            stmt.executeUpdate(sqlSolicitudes);
            System.out.println("Tablas creadas correctamente.");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void insertarDatos(){
        try {
            String sql ="INSERT INTO Usuarios (nombreUsuario, clave) VALUES\n" +
                    "('alice', 'clave123'),\n" +
                    "('bob', 'clave456'),\n" +
                    "('charlie', 'clave789'),\n" +
                    "('david', 'clave000');\n" +
                    "\n" +
                    "INSERT INTO Solicitudes (id1, id2, estado, fecha_solicitud) VALUES\n" +
                    "(1, 2, 'pendiente', '2024-11-20'),\n" +
                    "(1, 3, 'pendiente', '2024-11-20'),\n" +
                    "(2, 4, 'aceptada', '2024-11-18');\n" +
                    "\n" +
                    "INSERT INTO Amigos (id1, id2) VALUES\n" +
                    "(1, 2),\n" +
                    "(1, 3),\n" +
                    "(2, 4);";
            var stmt = con.createStatement();
            stmt.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void pruebaConsulta(){
        try {
            String sql = "SELECT * FROM Usuarios";
            var stmt = con.createStatement();
            var rs = stmt.executeQuery(sql);
            while (rs.next()) {
                System.out.println(rs.getString("id") + " " + rs.getString("nombreUsuario") + " " + rs.getString("clave"));
            }


            sql = "SELECT * FROM Solicitudes";
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                System.out.println(rs.getString("id1") + " " + rs.getString("id2") + " " + rs.getString("estado"));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
