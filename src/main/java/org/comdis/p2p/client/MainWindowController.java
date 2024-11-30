package org.comdis.p2p.client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.comdis.p2p.RemoteClient;
import org.comdis.p2p.exceptions.*;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;


// TODO: notificar al cliente cuando esta online
public class MainWindowController {

    // Lista de chats (ChatView) que contienen los mensajes
    private ObservableList<Node> chatLists;

    // Listas de elementos
    @FXML private ListView<String> pendingRequests;
    @FXML private ListView<String> searchResult;
    @FXML private ListView<String> contacts;
    @FXML private ListView<String> friendsList;

    // Text input
    @FXML private TextField inputMsg;
    @FXML private TextField inputSearchUsers;
    @FXML private PasswordField inputNewPassword2;
    @FXML private PasswordField inputNewPassword1;
    @FXML private PasswordField inputOldPassword;

    // Elementos de la GUI que se actualizan dinamicamente
    @FXML private Label inputFailed;
    @FXML private SplitPane splitPane;
    @FXML private StackPane chatPane;
    @FXML private VBox vBox;
    @FXML private Button btnSend;
    @FXML private Label chatName;
    @FXML private HBox hBox;

    @FXML
    public void initialize() {
        ClientImpl.getInstance().setMainWindowController(this);

        //Ajustar la zona de los chats
        chatPane.setVisible(false);

        inputMsg.setPromptText("Escribe un mensaje");
        inputMsg.setVisible(false);
        btnSend.setVisible(false);

        //Se alarga el campo del mensaje
        HBox.setHgrow(inputMsg, Priority.ALWAYS);

        //Se alarga el stackpane dentro del vBox
        VBox.setVgrow(chatPane, Priority.ALWAYS);

        // Notificar de los amigos ya conectados y guardar lista de amigos
        Collection<RemoteClient> friendsOnline = ClientImpl.getInstance().getFriendsOnline();
        if (!friendsOnline.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            builder.append("Usuarios: ");
            for (RemoteClient friend : friendsOnline) {
                builder.append(friend.getUsername());
                builder.append(' ');
            }
            createAlert("INFORMATION","Contactos","Amigos conectados:",builder.toString());

            // Añadimos amigos a la GUI
            contacts.getItems().clear();
            for(RemoteClient friend : friendsOnline){
                contacts.getItems().add(friend.getUsername());
            }
        }

        // Notificar de las peticiones de amistad pendientes
        Collection<String> pendingRequests = ClientImpl.getInstance().getPendingRequests();
        if(pendingRequests != null){
            if (!pendingRequests.isEmpty()) {
                StringBuilder builder = new StringBuilder();
                builder.append("Usuarios: ");
                for (String solicitud : pendingRequests) {
                    builder.append(solicitud);
                    builder.append(' ');
                }
                createAlert("INFORMATION","Solicitudes pendientes","Tiene solicitudes de amistad pendientes por responder",builder.toString());
                // Mostrar las solicitudes pendientes
                this.pendingRequests.getItems().clear();
                this.pendingRequests.getItems().addAll(ClientImpl.getInstance().getPendingRequests());
            }
        }
    }


    public void addContact(String username) {
        Platform.runLater(() -> {
            contacts.getItems().add(username);
        });
    }

    public void removeContact(String username) {
        Platform.runLater(() -> {
            contacts.getItems().remove(username);
        });
    }

    public void addPendingRequest(String username) {
        Platform.runLater(() -> {
            pendingRequests.getItems().add(username);
        });
    }

    public void removePendingRequest(String username) {
        Platform.runLater(() -> {
            pendingRequests.getItems().remove(username);
        });
    }

    public void addFriend(String username) {
        Platform.runLater(() -> {
            friendsList.getItems().add(username);
        });
    }

    public void removeFriend(String username) {
        Platform.runLater(() -> {
            friendsList.getItems().remove(username);
        });
    }

    // ==== CHATS ======================================================================================================


    public void deleteChat(String friend) {
        Platform.runLater(() -> {
            for (Node n : chatLists) {
                if (n instanceof ChatView chat && chat.fromUser(friend)) {
                    chatPane.getChildren().remove(chat);
                    chatName.setText("Selecione un chat");
                    break;
                }
            }
        });
    }


    public void receiveMessage(String sender, String message) {
        if (chatLists == null) {
            chatLists = FXCollections.observableArrayList();
            chatPane.getChildren().clear();

            ChatView chat = new ChatView(sender,chatPane);

            chatLists.add(chat);
            chat.addReceivedMsg(message);

            chatPane.getChildren().add(chat);
        }else {
            ChatView selected = null;
            for (Node n : chatLists) {
                if (n instanceof ChatView chatView ) {
                    if (chatView.fromUser(sender)) {
                        selected = chatView;
                        break;
                    }
                }
            }
            if (selected == null) {
                ChatView chat = new ChatView(sender,chatPane);
                chat.addReceivedMsg(message);
                chatLists.add(chat);
                chatPane.getChildren().add(chat);
            }else{
                selected.addReceivedMsg(message);
            }
        }
        //DEBUG
        System.out.println("Contenido de chatPane: " + chatPane.getChildren());
    }

    @FXML
    public void sendMessage() {
        if (inputMsg.getText().isEmpty()) {
            return;
        }
        System.out.println("Enviar mensaje a " + contacts.getSelectionModel().getSelectedItem());
        for (Node n : chatLists) {
            if (n instanceof ChatView chat && chat.fromUser(contacts.getSelectionModel().getSelectedItem())) {
                chat.addSentMsg(inputMsg.getText());
                try {
                    ClientImpl.getInstance().sendMessage(chat.getUsername(),inputMsg.getText());
                }catch (RemoteException e){
                    PtpException.logError(e);
                }catch (NotFoundException e){
                    createAlert("ERROR","Usuario desconocido","Usuario no encontrado",e.getMessage());
                }
                break;
            }
        }
    }

    @FXML
    public void openChat(MouseEvent mouseEvent) {
        chatPane.setVisible(true);
        String selectedItem = contacts.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return;
        }
        System.out.println("Has seleccionado el chat con " + selectedItem);
        chatName.setText(selectedItem);
        btnSend.setVisible(true);
        inputMsg.setVisible(true);
        if (chatLists == null) { //Primer chat, sin haber recibido mensaje
            chatPane.getChildren().clear();

            chatLists = FXCollections.observableArrayList();


            ChatView chat = new ChatView(selectedItem,chatPane);

            chatLists.add(chat);
            chatPane.getChildren().add(chat);
            chat.toFront();
        }else{
            ChatView selected = null;
            for (Node n : chatLists) {
                if (n instanceof ChatView chatView ) {
                    if (chatView.fromUser(selectedItem)){
                        selected = chatView;
                        break;
                    }
                }
            }
            if (selected == null) { //El chat no existe, se crea uno nuevo
                ChatView chat = new ChatView(selectedItem,chatPane);


                chatLists.add(chat);
                chatPane.getChildren().add(chat);
                chat.toFront();
            }else{
                selected.toFront();
            }
            //DEBUG
            System.out.println("Contenido de chatPane: " + chatPane.getChildren());
        }
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
            createAlert("ERROR","Excepción remota","Ha ocurrido una excepción de RemoteException","");
            PtpException.logError(error);
        }
    }

    // ==== SOLICITUDES DE AMISTAD =====================================================================================

    @FXML
    public void sendFriendRequest(ActionEvent actionEvent) {
        String other = searchResult.getSelectionModel().getSelectedItem();
        try {
            ClientImpl.getInstance().sendFriendRequest(other);
            createAlert("INFORMATION","Solicitud enviada","Solicitud enviada correctamente","");
        } catch (AlreadyExistsException e) {
            createAlert("INFORMATION","Solicitud existente","Ya hay una solicitud pendiente","Espere a la respuesta del otro usuario");
        } catch (NotFoundException e) {
            createAlert("INFORMATION","Usuario desconocido","Usuario desconocido","No se ha encontrado al usuario de ID \"%s\"".formatted(other));
        } catch (RemoteException e) {
            createAlert("ERROR","Excepción remota","Ha ocurrido una excepción de RemoteException","");
            PtpException.logError(e);
        } catch (PetitionFromOtherExistsException e) {
            createAlert("INFORMATION","Solicitud existente","Existe ya una solicitud de amistad",e.getMessage());
            acceptFriendRequest(other);
        }
    }

    private void acceptFriendRequest(String newFriend) {
        try {
            ClientImpl.getInstance().acceptFriendRequest(newFriend);
            System.out.println("Has aceptado la solicitud de " + newFriend);
            pendingRequests.getItems().remove(newFriend);
        } catch (AlreadyExistsException e) {
            createAlert("INFORMATION","Ya existe","Ya existe","Tu amistad con \"%s\" ya ha sido establecida".formatted(newFriend));
            // Si estaba selecionado es que no se habia borrado de antes
            pendingRequests.getItems().remove(newFriend);
        } catch (NotFoundException e) {
            createAlert("INFORMATION","Usuario desconocido","Usuario desconocido","No se ha encontrado al usuario de ID \"%s\"".formatted(newFriend));
        } catch (RemoteException e) {
            createAlert("ERROR","Excepción remota","Ha ocurrido una excepción de RemoteException","");
            PtpException.logError(e);
        }

    }


    @FXML
    public void acceptFriendRequest(ActionEvent actionEvent) {
        String newFriend = pendingRequests.getSelectionModel().getSelectedItem();
        acceptFriendRequest(newFriend);
    }

    @FXML
    public void rejectFriendRequest(ActionEvent event) {
        String rejected = pendingRequests.getSelectionModel().getSelectedItem();
        try {
            ClientImpl.getInstance().cancelFriendRequest(rejected);
        } catch (RemoteException e) {
            PtpException.logError(e);
        }catch (NotFoundException e){
            createAlert("INFORMATION","Usuario desconocido","Usuario desconocido","No se ha encontrado al usuario de ID \"%s\"".formatted(rejected));
        }
    }

    @FXML
    public void getFriends(Event event) {
        try {
            Collection<String> result = ClientImpl.getInstance().getFriends();
            if(result != null) {
                if (!result.isEmpty()){
                    friendsList.getItems().clear();
                    friendsList.getItems().addAll(ClientImpl.getInstance().getFriends());
                }
            }
        }catch (RemoteException e){
            PtpException.logError(e);
        }catch (NotFoundException e){
            createAlert("INFORMATION","Usuario desconocido","Usuario desconocido","No se ha encontrado al usuario de ID \"%s\"".formatted(ClientImpl.getInstance().getUsername()));
        }
    }

    @FXML
    public void deleteFriendship(ActionEvent actionEvent) {
        String friend = friendsList.getSelectionModel().getSelectedItem();
        if (friend == null) {
            return;
        }
        try {
            ClientImpl.getInstance().deleteFriendship(friend);
            deleteChat(friend);
            removeFriend(friend);
            removeContact(friend);
        }catch (RemoteException e){
            PtpException.logError(e);
        }catch (NotFoundException e){
            createAlert("INFORMATION","Usuario desconocido","Usuario desconocido","No se ha encontrado al usuario de ID \"%s\"".formatted(friend));
        }
    }

    // ==== AJUSTES ======================================================================================================

    @FXML
    public void changePassword(ActionEvent actionEvent) {
        if(inputNewPassword1.getText().isEmpty() || inputNewPassword2.getText().isEmpty() || inputOldPassword.getText().isEmpty()){
            inputFailed.setVisible(true);
            inputFailed.setText("Debe rellenar todos los campos");
        }else{
            inputFailed.setVisible(false);
            if(inputNewPassword1.getText().equals(inputNewPassword2.getText())){
                try {
                    ClientImpl.getInstance().changePassword(ClientImpl.getInstance().getUsername(),inputOldPassword.getText(),inputNewPassword1.getText());
                    createAlert("INFORMATION","Operación exitosa","Contraseña actualizada correctamente","");
                }catch (AuthException e){
                    createAlert("ERROR","Credenciales inválidas",e.getMessage(),"Revise por favor si la contraseña es correcta");
                }catch (RemoteException e) {
                    PtpException.logError(e);
                }

            }else{
                inputFailed.setVisible(true);
                inputFailed.setText("Las contraseñas introducidas no coinciden");
            }
        }
    }

    // ==== OTROS ======================================================================================================

    @FXML
    public void printRequestSelected(MouseEvent mouseEvent) {
        String selectedItem = searchResult.getSelectionModel().getSelectedItem();
        System.out.println("Has seleccionado la solicitud de " + selectedItem);
    }

    @FXML
    public void printsSelectedUser(MouseEvent mouseEvent) {
        String selectedItem = searchResult.getSelectionModel().getSelectedItem();
        System.out.println("Has seleccionado a " + selectedItem);
    }

    @FXML
    public void printSelectedFriend(MouseEvent mouseEvent) {
        String selectedItem = friendsList.getSelectionModel().getSelectedItem();
        System.out.println("Has seleccionado a " + selectedItem);
    }

    public void createAlert(String type,String title, String header, String content) {
        String type_static = type.toUpperCase();
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.valueOf(type_static));
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });

    }


}
