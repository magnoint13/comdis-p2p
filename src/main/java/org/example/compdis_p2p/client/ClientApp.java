package org.example.compdis_p2p.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.example.compdis_p2p.PtpException;
import org.example.compdis_p2p.server.MainServer;
import org.example.compdis_p2p.server.ServerInterface;

import java.rmi.ConnectException;
import java.rmi.Naming;

public class ClientApp extends Application {
    private static final int WINDOW_WIDTH = 500;
    private static final int WINDOW_HEIGHT = 500;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) {
        try {
            // Iniciar la interfaz
            FXMLLoader fxmlLoader = new FXMLLoader(ClientApp.class.getResource("Inicio.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), WINDOW_WIDTH, WINDOW_HEIGHT);
            stage.setTitle("Programa de comunicación P2P!");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();

            // Obtener una referencia al servidor para el controller
            ServerInterface server = (ServerInterface) Naming.lookup(MainServer.REGISTRY_URL);
            InicioController controller = fxmlLoader.getController();
            controller.setServer(server);
            controller.TxtAviso.setVisible(false);

        } catch (ConnectException error) {
            new Alert(Alert.AlertType.ERROR, "No se pudo conectar al servidor").showAndWait();
            System.exit(1);

        } catch (Exception error) {
            // Mostrar el error por pantalla
            PtpException.logError(error);

            // Mostrar al usuario un error "amigable"
            new Alert(Alert.AlertType.ERROR, "Ha sucedido un error inesperado").showAndWait();

            // Y salir con codigo no exitoso
            System.exit(1);
        }
    }
}