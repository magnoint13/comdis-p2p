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
import org.comdis.p2p.ClientCallback;
import org.comdis.p2p.ServerInterface;
import org.comdis.p2p.exceptions.AlreadyExistsException;
import org.comdis.p2p.exceptions.AuthException;

import java.io.IOException;
import java.rmi.RemoteException;


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
    public TextField TxtPassword;
    @FXML
    public Label TxtAviso;

    private ServerInterface server;

    public void setServer(ServerInterface server) {
        this.server = server;
    }


    //La idea era cargar una imagen, pero su puta madre
    public void loadImage() {
        Image image = new Image(getClass().getResource("/org/example/compdis_p2p/client/resources/images/logo.png").toExternalForm());

        // Crear un ImageView para mostrarla
        imageView = new ImageView(image);
    }

    @FXML
    public void Singin(ActionEvent actionEvent) throws IOException {
        if (TxtUser.getText().isEmpty() || TxtPassword.getText().isEmpty()) {
            TxtAviso.setVisible(true);
        } else {
            ClientCallbackImpl client = new ClientCallbackImpl(TxtUser.getText(), TxtPassword.getText());
            try {
                server.connect(client);
                startMainWindow(client, server);
            } catch (AuthException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error de autenticación");
                alert.setHeaderText("Usuario o contraseña incorrectos ");
                alert.setContentText("Por favor revise sus credenciales o cree un usuario nuevo");
                alert.showAndWait();
            }
        }
    }

    @FXML
    public void Register(ActionEvent actionEvent) throws IOException {
        if (TxtUser.getText().isEmpty() || TxtPassword.getText().isEmpty()) {
            TxtAviso.setVisible(true);
        } else {
            ClientCallbackImpl client = new ClientCallbackImpl(TxtUser.getText(), TxtPassword.getText());
            try {
                server.addUser(client);
                server.connect(client);
                startMainWindow(client, server);
            } catch (AlreadyExistsException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error al resgistrarse");
                alert.setHeaderText("Usuario ya existente ");
                alert.setContentText("Por favor escoga un nombre de usuario diferente");
                alert.showAndWait();
            } catch (AuthException e) {
                //No hace na
            }

        }
    }

    private void startMainWindow(ClientCallback client, ServerInterface server) throws IOException {
        // Cargar el FXML para la nueva ventana
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/comdis/p2p/client/MainWindow.fxml"));
        Parent root = fxmlLoader.load();

        // Crear una nueva escena con el FXML cargado
        Scene scene = new Scene(root, 600, 400);

        MainWindowControlller controller = fxmlLoader.getController();

        controller.setClient(client);
        controller.setServer(server);
        controller.iniciar();

        // Obtener el stage actual y cambiar la escena
        Stage stage = (Stage) TxtUser.getScene().getWindow();
        stage.setScene(scene);
        stage.setResizable(false);

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
        stage.show();
    }
}