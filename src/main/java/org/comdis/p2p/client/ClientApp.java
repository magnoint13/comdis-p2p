package org.comdis.p2p.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.comdis.p2p.ServerInterface;
import org.comdis.p2p.exceptions.PtpException;
import org.comdis.p2p.server.MainServer;

import java.rmi.ConnectException;
import java.rmi.Naming;

/** Clase principal de la GUI del cliente. Abre la ventana de inicio de sesion y gestiona el cliente */
public class ClientApp extends Application {
    private static final String TITLE = "Programa de comunicacion P2P!";
    private static final int WINDOW_WIDTH = 600;
    private static final int WINDOW_HEIGHT = 400;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) {
        try {
            // Iniciar la interfaz
            // TODO: si funciona con poner Inicio.fxml no haria falta la ruta completa en las otras clases
            FXMLLoader fxmlLoader = new FXMLLoader(ClientApp.class.getResource("Inicio.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), WINDOW_WIDTH, WINDOW_HEIGHT);
            stage.setTitle(TITLE);
            stage.setScene(scene);
            stage.setResizable(false);

            // Obtener una referencia al servidor para el controller
            ServerInterface server = (ServerInterface) Naming.lookup(MainServer.REGISTRY_URL);
            InicioController controller = fxmlLoader.getController();
            controller.TxtAviso.setVisible(false);

            stage.show();

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