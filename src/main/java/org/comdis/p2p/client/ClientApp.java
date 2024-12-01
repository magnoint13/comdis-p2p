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

import java.io.IOException;
import java.net.URL;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Clase principal de la GUI del cliente. Abre la ventana de inicio de sesión y gestiona el cliente
 */
public class ClientApp extends Application {
    private static final String TITLE = "!Programa de comunicación P2P!";
    private static final int INIT_WINDOW_WIDTH = 650;
    private static final int INIT_WINDOW_HEIGHT = 400;

    private static final int MAIN_WINDOW_WIDTH = 800;
    private static final int MAIN_WINDOW_HEIGHT = 600;

    public static void main(String[] args) {
        launch();
    }

    public static void launchInicio(Stage stage) throws IOException {
        // Cargar la estructura de la nueva escena
        FXMLLoader fxmlLoader = new FXMLLoader(ClientApp.class.getResource("Inicio.fxml"));

        // Crear la nueva escena
        Scene scene = new Scene(fxmlLoader.load(), INIT_WINDOW_WIDTH, INIT_WINDOW_HEIGHT);

        // Añadir los estilos a la escena
        URL css = ClientApp.class.getResource("InicioStyles.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        } else {
            System.err.println("[ERROR] No se pudo cargar el archivo \"InicioStyles.css\"");
        }

        // Configurar el título
        stage.setTitle(TITLE);
        stage.setScene(scene);
        stage.show();
    }

    public static void launchMainWindow(Stage stage) throws IOException {
        // Cargar el FXML para la nueva ventana
        FXMLLoader fxmlLoader = new FXMLLoader(ClientApp.class.getResource("MainWindow.fxml"));

        // Crear una nueva escena con el FXML cargado
        Scene scene = new Scene(fxmlLoader.load(), MAIN_WINDOW_WIDTH, MAIN_WINDOW_HEIGHT);

        // Añadir los estilos a la escena
        URL css = ClientApp.class.getResource("MainStyles.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        } else {
            System.err.println("[ERROR] No se pudo cargar el archivo \"MainStyles.css\"");
        }

        // Cambiar la escena
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void start(Stage stage) {

        ClientImpl client;
        try {
            // Crear el cliente la primera vez
            // Esta clase será la encargada de liberarlo cuando el programa termine
            client = ClientImpl.create();
            client.serverConnect(MainServer.REGISTRY_URL);

            // Iniciar la interfaz
            launchInicio(stage);

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
