package org.comdis.p2p.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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
            ClientApp.launchMainWindow((Stage) username.getScene().getWindow());

        } catch (AuthException e) {
            txtError.setVisible(true);
            txtError.setText("Usuario o contraseña incorrectos");

        } catch (AlreadyExistsException e) {
            txtError.setVisible(true);
            txtError.setText("Ese usuario ya esta conectado");
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
            ClientApp.launchMainWindow((Stage) username.getScene().getWindow());

        } catch (AlreadyExistsException e) {
            txtError.setVisible(true);
            txtError.setText("Ese usuario ya existe");
        }
    }
}