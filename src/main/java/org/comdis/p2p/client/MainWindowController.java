package org.comdis.p2p.client;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.comdis.p2p.RemoteClient;
import org.comdis.p2p.exceptions.AlreadyExistsException;
import org.comdis.p2p.exceptions.NotFoundException;
import org.comdis.p2p.exceptions.PtpException;

import java.rmi.RemoteException;
import java.util.Collection;


//TODO: NOTIFICAR AL CLIENTE CUANDO ESTA ONLINE
public class MainWindowController {

    // Objetos de la interfaz
    @FXML
    private ListView<String> PendingRequests;
    @FXML
    private ListView<String> UsersList;
    @FXML
    private ListView<RemoteClient> Contacts;
    @FXML
    private TextField TxtSearchUsers;
    @FXML
    private javafx.scene.layout.BorderPane BorderPane;
    @FXML
    private HBox ChatInfo;
    @FXML
    private TextArea TxtMensaje;
    @FXML
    private Button BtnSend;
    @FXML
    private ScrollPane ChatPane;
    @FXML
    private VBox Chat;

    @FXML
    public void initialize() {
        BorderPane.setVisible(false);

        // Notificar de los amigos ya conectados
        Collection<RemoteClient> friendsOnline = ClientImpl.getInstance().getFriendsOnline();
        if (!friendsOnline.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Contactos");
            alert.setHeaderText("Amigos conectados:");

            StringBuilder builder = new StringBuilder();

            for (RemoteClient friend : friendsOnline) {
                builder.append(friend.getUsername());
                builder.append('\n');
            }

            alert.setContentText(builder.toString());
        }

        // Mostrar los amigos
        // TODO: el cliente tiene copias repetidas de esto (en ClientImpl y aqui)
        Contacts.getItems().clear();
        Contacts.getItems().addAll(friendsOnline);

        // Notificar de las peticiones de amistad pendientes
        Collection<String> pendingRequests = ClientImpl.getInstance().getPendingRequests();
        if (!pendingRequests.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Solicitudes pendientes");
            alert.setHeaderText("Tiene solicitudes de amistad pendientes por responder");

            StringBuilder builder = new StringBuilder();

            for (RemoteClient friend : friendsOnline) {
                builder.append(friend.getUsername());
                builder.append('\n');
            }

            alert.setContentText(builder.toString());

            alert.showAndWait();

            // TODO: quiero probar de esta forma mas tarde, no borrar please
            //new Alert(Alert.AlertType.INFORMATION, "Tiene solicitudes de amistad pendientes").show();

            // Mostrar las solicitudes pendientes
            PendingRequests.getItems().clear();
            PendingRequests.getItems().addAll(ClientImpl.getInstance().getPendingRequests());
        }
    }

    // ==== CHATS ======================================================================================================

    @FXML
    public void sendMessage(ActionEvent actionEvent) {
        System.out.println("Enviar mensaje");
    }

    @FXML
    public void openChat(MouseEvent mouseEvent) {
        // Obtener el ítem seleccionado del ListView
        String selectedItem = Contacts.getSelectionModel().getSelectedItem().getUsername();
        System.out.println("Has seleccionado el chat con " + selectedItem);
    }

    // ==== BUSCAR AMIGOS ==============================================================================================

    @FXML
    public void searchUsers(ActionEvent actionEvent) {
        if (TxtSearchUsers.getText().isEmpty()) {
            return;
        }

        try {
            Collection<String> result = ClientImpl.getInstance().searchUsernames(TxtSearchUsers.getText());
            UsersList.getItems().clear();
            ObservableList<String> items = FXCollections.observableArrayList();
            items.addAll(result);
            UsersList.setItems(items);

            // Debug
            System.out.println("Búsqueda de usuarios:");
            for (String r : result) {
                System.out.println("\t" + r);
            }
        } catch (RemoteException error) {
            // TODO: alert?
            PtpException.logError(error);
        }
    }

    // ==== SOLICITUDES DE AMISTAD =====================================================================================

    @FXML
    public void sendFriendRequest(ActionEvent actionEvent) {
        String other = UsersList.getSelectionModel().getSelectedItem();
        try {
            ClientImpl.getInstance().sendFriendRequest(other);

        } catch (AlreadyExistsException e) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Solicitud existente");
            alert.setHeaderText("Ya hay una solicitud pendiente");
            alert.setContentText("Espere a la respuesta del otro usuario");
            alert.showAndWait();
        } catch (NotFoundException e) {
            // TODO: este alert se reutiliza en varios sitios. Guardarlo como atributo y aqui solo hacer showAndWait
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Usuario desconocido");
            alert.setHeaderText("Usuario desconocido");
            alert.setContentText("No se ha encontrado al usuario de ID \"%s\"".formatted(other));
            alert.showAndWait();
        } catch (RemoteException e) {
            // TODO: alert?
            PtpException.logError(e);
        }
    }

    @FXML
    public void acceptFriendRequest(ActionEvent actionEvent) {
        String newFriend = PendingRequests.getSelectionModel().getSelectedItem();

        try {
            ClientImpl.getInstance().acceptFriendRequest(newFriend);
            System.out.println("Has aceptado la solicitud de " + newFriend);
            PendingRequests.getItems().remove(newFriend);

        } catch (AlreadyExistsException e) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Ya existe");
            alert.setHeaderText("Ya existe");
            alert.setContentText("Tu amistad con \"%s\" ya ha sido establecida".formatted(newFriend));
            alert.showAndWait();

            // Si estaba selecionado es que no se habia borrado de antes
            PendingRequests.getItems().remove(newFriend);
        } catch (NotFoundException e) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Usuario desconocido");
            alert.setHeaderText("Usuario desconocido");
            alert.setContentText("No se ha encontrado al usuario de ID \"%s\"".formatted(newFriend));
            alert.showAndWait();

        } catch (RemoteException e) {
            // TODO: alert?
            PtpException.logError(e);
        }
    }

    // ==== OTROS ======================================================================================================

    @FXML
    public void printRequestSelected(MouseEvent mouseEvent) {
        String selectedItem = UsersList.getSelectionModel().getSelectedItem();
        System.out.println("Has seleccionado la solicitud de " + selectedItem);
    }

    //Nada, metodo inncecesario por ahora, ignorar
    @FXML
    public void printsSelectedUser(MouseEvent mouseEvent) {
        String selectedItem = UsersList.getSelectionModel().getSelectedItem();
        System.out.println("Has seleccionado a " + selectedItem);
    }
}
