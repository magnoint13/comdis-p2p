package org.comdis.p2p.client;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import org.comdis.p2p.RemoteClient;
import org.comdis.p2p.exceptions.AlreadyExistsException;
import org.comdis.p2p.exceptions.NotFoundException;
import org.comdis.p2p.exceptions.PtpException;

import java.rmi.RemoteException;
import java.util.Collection;


// TODO: notificar al cliente cuando esta online
public class MainWindowController {

    @FXML private TextField inputSearchUsers;
    @FXML private TextField inputMsg;

    @FXML private ChatView chat;
    @FXML private SplitPane splitPane;
    @FXML private VBox emptyChatPane;
    @FXML private VBox chatPane;

    @FXML private ListView<String> pendingRequests;
    @FXML private ListView<String> searchResult;
    @FXML private ListView<RemoteClient> contacts;

    @FXML
    public void initialize() {
        // TODO: mostrar al seleccionar un chat. Tambien esconder emptyChatPane
        emptyChatPane.setVisible(false);

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
        contacts.getItems().clear();
        contacts.getItems().addAll(friendsOnline);
        contacts.getItems().addAll(
                new RemoteClient("test00", null),
                new RemoteClient("test01", null),
                new RemoteClient("test02", null),
                new RemoteClient("test03", null),
                new RemoteClient("test04", null),
                new RemoteClient("test05", null),
                new RemoteClient("test06", null),
                new RemoteClient("test07", null),
                new RemoteClient("test08", null),
                new RemoteClient("test09", null),
                new RemoteClient("test10", null));

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
            this.pendingRequests.getItems().clear();
            this.pendingRequests.getItems().addAll(ClientImpl.getInstance().getPendingRequests());
        }
    }

    // ==== CHATS ======================================================================================================

    @FXML
    public void sendMessage(ActionEvent actionEvent) {
        System.out.println("Enviar mensaje");
    }

    @FXML
    public void openChat(MouseEvent mouseEvent) {
        // TODO:
        // Obtener el ítem seleccionado del ListView
        String selectedItem = contacts.getSelectionModel().getSelectedItem().getUsername();
        System.out.println("Has seleccionado el chat con " + selectedItem);
    }

    // ==== BUSCAR AMIGOS ==============================================================================================

    @FXML
    public void searchUsers(ActionEvent actionEvent) {
        if (inputSearchUsers.getText().isEmpty()) {
            return;
        }

        try {
            Collection<String> result = ClientImpl.getInstance().searchUsernames(inputSearchUsers.getText());
            searchResult.getItems().clear();
            ObservableList<String> items = FXCollections.observableArrayList();
            items.addAll(result);
            searchResult.setItems(items);

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
        String other = searchResult.getSelectionModel().getSelectedItem();
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
        String newFriend = pendingRequests.getSelectionModel().getSelectedItem();

        try {
            ClientImpl.getInstance().acceptFriendRequest(newFriend);
            System.out.println("Has aceptado la solicitud de " + newFriend);
            pendingRequests.getItems().remove(newFriend);

        } catch (AlreadyExistsException e) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Ya existe");
            alert.setHeaderText("Ya existe");
            alert.setContentText("Tu amistad con \"%s\" ya ha sido establecida".formatted(newFriend));
            alert.showAndWait();

            // Si estaba selecionado es que no se habia borrado de antes
            pendingRequests.getItems().remove(newFriend);
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

    @FXML
    public void rejectFriendRequest(ActionEvent event) {
        // TODO
    }

    // ==== OTROS ======================================================================================================

    @FXML
    public void printRequestSelected(MouseEvent mouseEvent) {
        String selectedItem = searchResult.getSelectionModel().getSelectedItem();
        System.out.println("Has seleccionado la solicitud de " + selectedItem);
    }

    //Nada, metodo inncecesario por ahora, ignorar
    @FXML
    public void printsSelectedUser(MouseEvent mouseEvent) {
        String selectedItem = searchResult.getSelectionModel().getSelectedItem();
        System.out.println("Has seleccionado a " + selectedItem);
    }
}
