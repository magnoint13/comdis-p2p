package org.comdis.p2p.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.comdis.p2p.exceptions.AlreadyExistsException;
import org.comdis.p2p.exceptions.AuthException;

import java.io.IOException;


class InicioController {

    @FXML
    public AnchorPane Leftpanel;
    @FXML
    public AnchorPane Rightpanel;
    @FXML
    public ImageView imageView;
    @FXML
    public TextField TxtUser;
    @FXML
    public TextField TxtPassword; // TODO: no se si hay PasswordField
    @FXML
    public Label TxtAviso;

    private ClientCallbackImpl client;

    // PELIGRO DE NULLPOINTER: importante establecer el cliente antes de usar la clase
    public void setClientRef(ClientCallbackImpl client) {
        this.client = client;
    }

    // La idea era cargar una imagen, pero su puta madre
    public void loadImage() {
        Image image = new Image(getClass().getResource("/org/comdis/p2p/client/resources/images/logo.png").toExternalForm());

        // Crear un ImageView para mostrarla
        imageView = new ImageView(image);
    }

    /** Inicio de sesion */
    @FXML
    public void logIn(ActionEvent actionEvent) throws IOException {
        if (TxtUser.getText().isEmpty() || TxtPassword.getText().isEmpty()) {
            TxtAviso.setVisible(true);
            return;
        }

        try {
            client.connect(TxtUser.getText(), TxtPassword.getText());
            startMainWindow();

        } catch (AuthException e) {
            // TODO: no se muestra TxtAviso?
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error de autenticación");
            alert.setHeaderText("Usuario o contraseña incorrectos ");
            alert.setContentText("Por favor revise sus credenciales o cree un usuario nuevo");
            alert.showAndWait();
        }
    }

    /** Crear nuevo usuario */
    @FXML
    public void registerNewUser(ActionEvent actionEvent) throws IOException {
        if (TxtUser.getText().isEmpty() || TxtPassword.getText().isEmpty()) {
            TxtAviso.setVisible(true);
            return;
        }
        try {
            client.createUserAndConnect(TxtUser.getText(), TxtPassword.getText());
            startMainWindow();

        } catch (AlreadyExistsException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al registrarse");
            alert.setHeaderText("Usuario ya existente");
            alert.setContentText("No se puede crear un usuario que ya existe. Seleccione otro nombre de usuario.");
            alert.showAndWait();
        }
    }

    private void startMainWindow() throws IOException {
        // Cargar el FXML para la nueva ventana
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/comdis/p2p/client/MainWindow.fxml"));
        Parent root = fxmlLoader.load();

        // Crear una nueva escena con el FXML cargado
        Scene scene = new Scene(root, 600, 400);

        MainWindowController controller = fxmlLoader.getController();
        controller.setClientRef(client);

        // Obtener el stage actual y cambiar la escena
        Stage stage = (Stage) TxtUser.getScene().getWindow();
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        // TODO: dado que el cliente se cierra en otro sitio, esto ya no hace falta
        /*
        stage.setOnCloseRequest(event -> {
            // Consumir el evento para evitar que se cierre inmediatamente
            event.consume();
            try {
                server.disconnect(client);
                System.exit(0);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        */
    }
}