package org.example.compdis_p2p.client;
import org.example.compdis_p2p.server.Server;
import javafx.application.Application;
import javafx.stage.Stage;
import org.example.compdis_p2p.server.DataBaseController;
import java.rmi.Naming;



import java.io.IOException;

public class ClientApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        DataBaseController dataBaseController = new DataBaseController();
        dataBaseController.conectarBD();
        dataBaseController.insertarDatos();
        dataBaseController.pruebaConsulta();
        // Guarda la URL del objeto remoto
        String registryURL = "rmi://localhost:1099/server";
        // Usa el metodo Naming.lookup() para obtener el objeto remoto y lo castea al tipo adecuado
        try {
            Server objeto = (Server) Naming.lookup(registryURL);
        }catch (Exception error){
            // En caso de error, simplemente mostrarlo por pantalla
            System.err.printf("[ERROR] %s: %s\n", error.getClass().getName(), error.getMessage());
            error.printStackTrace();
            // Y salir con código no exitoso
            System.exit(1);
        }
      /*  FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("Inicio.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Programa de comunicación P2P!");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
       */
    }

    public static void main(String[] args) {
        launch();
    }
}