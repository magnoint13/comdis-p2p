package org.comdis.p2p.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.comdis.p2p.exceptions.PtpException;
import org.comdis.p2p.server.MainServer;

import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Clase principal de la GUI del cliente. Abre la ventana de inicio de sesión y gestiona el cliente
 */
public class ClientApp extends Application {
    private static final String TITLE = "Programa de comunicación P2P!";
    private static final int WINDOW_WIDTH = 650;
    private static final int WINDOW_HEIGHT = 400;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) {

        ClientImpl client;
        try {
            // Crear el cliente la primera vez
            // Esta clase sera la encargada de liberarlo cuando el programa termine
            client = ClientImpl.create();
            // TODO: Obtener URL de la GUI
            client.serverConnect(MainServer.REGISTRY_URL);

            // Iniciar la interfaz
            FXMLLoader fxmlLoader = new FXMLLoader(ClientApp.class.getResource("Inicio.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), WINDOW_WIDTH, WINDOW_HEIGHT);
            scene.getStylesheets().add(getClass().getResource("InicioStyles.css").toExternalForm());
            stage.setTitle(TITLE);
            stage.setScene(scene);
            stage.show();

            // Centra la ventana en la pantalla
            // Se debe establecer despues de ejecutar el show
            Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
            stage.setX((primScreenBounds.getWidth() - stage.getWidth()) / 2);
            stage.setY((primScreenBounds.getHeight() - stage.getHeight()) / 2);

            // Asegurarse de que el cliente se desconecta antes de salir
            stage.setOnCloseRequest(event -> {
                try {
                    client.close();
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (ConnectException _) {
            new Alert(Alert.AlertType.ERROR, "No se pudo conectar al servidor").showAndWait();
            System.exit(1);

        } catch (NotBoundException error) {
            new Alert(Alert.AlertType.ERROR, "No se ha encontrado el servidor en \"%s\"".formatted(error.getMessage())).showAndWait();
            System.exit(1);

        } catch (Exception error) {
            // Mostrar el error por pantalla
            PtpException.logError(error);

            // Mostrar al usuario un error "amigable"
            new Alert(Alert.AlertType.ERROR, "Ha sucedido un error inesperado").showAndWait();

            // Y salir con código no exitoso
            System.exit(1);
        }
    }
}
