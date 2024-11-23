package org.example.compdis_p2p.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.compdis_p2p.server.ServerInterface;

import java.rmi.RemoteException;
import java.util.Collection;

public class MainWindowControlller {


    private ClientInterface client;

    private ServerInterface server;

    @FXML
    public ListView UsersList;
    @FXML
    public TextField TxtSearchUsers;
    @FXML
    public javafx.scene.layout.BorderPane BorderPane;
    @FXML
    public ListView Contacts;
    @FXML
    public HBox ChatInfo;
    @FXML
    public TextArea TxtMensaje;
    @FXML
    public Button BtnSend;
    @FXML
    public ScrollPane ChatPane;
    @FXML
    public VBox Chat;

    public void setClient(ClientInterface client) {
        this.client = client;
    }

    public void setServer(ServerInterface server) {
        this.server = server;
    }

    public void iniciar() throws RemoteException {
        BorderPane.setVisible(false);
        //ChatInfo.setVisible(false);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Contactos");
        alert.setHeaderText("Amigos conectados:");
        alert.setContentText(client.getFriendsOnline().toString());


        //Amigos
        Contacts.getItems().clear();
        Contacts.getItems().addAll(client.getFriendsOnline());
        javafx.collections.ObservableList<String> items = javafx.collections.FXCollections.observableArrayList();
        for (ClientInterface c : client.getFriendsOnline()) {
            items.add(c.getUsername());
        }
        Contacts.setItems(items);

        //Peticiones de amistad
        if(!client.getPendingRequests().isEmpty()){
            Alert alert2 = new Alert(Alert.AlertType.INFORMATION);
            alert2.setTitle("Solicitudes pendientes");
            alert2.setHeaderText("Tiene solicitudes de amistad pendientes por responder ");
            alert2.showAndWait();
            //new Alert(Alert.AlertType.INFORMATION, "Tiene solicitudes de amistad pendientes").show();
            //quiero probar de esta forma mas tarde, no borrar please
        }
    }

    @FXML
    public void sendMessage(ActionEvent actionEvent) {
       // server.
    }

    @FXML
    public void openChat(MouseEvent mouseEvent) {
        // Obtener el ítem seleccionado del ListView
        String selectedItem = (String) Contacts.getSelectionModel().getSelectedItem();

        System.out.println("Has seleccionado el chat con " + selectedItem);
    }

    @FXML
    public void addFriend(ActionEvent actionEvent) throws RemoteException {
        server.sendFriendRequest(client,(String) UsersList.getSelectionModel().getSelectedItem());
    }

    @FXML
    public void searchUsers(ActionEvent actionEvent) throws RemoteException {
        if (TxtSearchUsers.getText().isEmpty()) {
            return;
        }else{
            Collection<ClientInterface> result = server.searchClientsByName(TxtSearchUsers.getText());
            UsersList.getItems().clear();
            javafx.collections.ObservableList<String> items = javafx.collections.FXCollections.observableArrayList();
            for (ClientInterface c : result) {
                if (!c.getUsername().equals(client.getUsername())) { //TODO: hacer en SQL esta comprobacion?? ES no encontrare a ti mismo basicamente
                    items.add(c.getUsername());
                    System.out.println("Has encontrado al usuario " + c.getUsername());
                }
            }
            UsersList.setItems(items);
        }
    }

    //Nada, metodo inncecesario por ahora, ignorar
    @FXML
    public void printsSelectedUser(MouseEvent mouseEvent) {
        String selectedItem = (String) UsersList.getSelectionModel().getSelectedItem();
        System.out.println("Has seleccionado a " + selectedItem);
    }

    //TODO: es igual que otro meotodo que no me acuero coño, lo hago yo xd
    @FXML
    public void acceptFriendRequest(ActionEvent actionEvent) {

    }
}
