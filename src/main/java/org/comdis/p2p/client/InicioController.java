package org.comdis.p2p.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.comdis.p2p.exceptions.AlreadyExistsException;
import org.comdis.p2p.exceptions.AuthException;
import org.comdis.p2p.exceptions.PtpException;

import java.io.IOException;


public class InicioController {

    @FXML
    public ImageView imageView;
    @FXML
    public TextField username;
    @FXML
    public PasswordField password;
    @FXML
    public Label txtError;

    @FXML
    public void initialize() {
        // TODO: ni idea de porque no funciona
        imageView.setImage(new Image(getClass().getResource("logo.png").toExternalForm()));
        txtError.setVisible(false);
    }

    /**
     * Inicio de sesion
     */
    @FXML
    public void logIn(ActionEvent ignoredActionEvent) throws IOException {
        if (username.getText().isEmpty() || password.getText().isEmpty()) {
            txtError.setVisible(true);
            txtError.setText("Introduce usuario y contraseña");
            return;
        }

        try {
            ClientImpl.getInstance().connect(username.getText(), password.getText());
            startMainWindow();

        } catch (AuthException e) {
            txtError.setVisible(true);
            txtError.setText("Usuario o contraseña incorrectos");

        } catch (AlreadyExistsException e) {
            // Es imposible que ya esté conectado, si se acaba de iniciar
            // Se muestra por la consola en caso de que haya algo mal
            PtpException.logError(e);
        }
    }

    /**
     * Crear nuevo usuario
     */
    @FXML
    public void registerNewUser(ActionEvent ignoredActionEvent) throws IOException {
        if (username.getText().isEmpty() || password.getText().isEmpty()) {
            txtError.setVisible(true);
            txtError.setText("Introduce usuario y contraseña");
            return;
        }
        try {
            ClientImpl.getInstance().createUserAndConnect(username.getText(), password.getText());
            startMainWindow();

        } catch (AlreadyExistsException e) {
            txtError.setVisible(true);
            txtError.setText("Ese usuario ya existe");
        }
    }

    private void startMainWindow() throws IOException {
        // Cargar el FXML para la nueva ventana
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MainWindow.fxml"));
        Parent root = fxmlLoader.load();

        // Crear una nueva escena con el FXML cargado
        Scene scene = new Scene(root, 600, 400);

        // Obtener el stage actual y cambiar la escena
        Stage stage = (Stage) username.getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }
}