package org.example.compdis_p2p.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import org.example.compdis_p2p.AuthException;
import org.example.compdis_p2p.server.ServerInterface;

import javax.naming.AuthenticationException;
import java.rmi.RemoteException;


public class InicioController {

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
    @FXML
    public Button BtnSignin;

    private ServerInterface server;

    public void setServer(ServerInterface server) {
        this.server = server;
    }


    //La idea era cargar una imagen, pero su puta madre
    public void loadImage(){
        Image image = new Image(getClass().getResource("/org/example/compdis_p2p/client/resources/images/logo.png").toExternalForm());

        // Crear un ImageView para mostrarla
        imageView = new ImageView(image);
    }

    @FXML
    public void Singin(ActionEvent actionEvent) throws RemoteException {
        if (TxtUser.getText().isEmpty() || TxtPassword.getText().isEmpty()) {
            TxtAviso.setVisible(true);
        }else{
            Client client = new Client(TxtUser.getText(),TxtPassword.getText());
            try {
                server.connect(client);
            } catch (AuthException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error de autenticación");
                alert.setHeaderText("Usuario o contraseña incorrectos ");
                alert.setContentText("Por favor revise sus credenciales o cree un usuario nuevo");
                alert.showAndWait();
            }
        }
    }
}