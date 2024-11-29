package org.comdis.p2p.client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import org.comdis.p2p.RemoteClient;
import org.comdis.p2p.exceptions.*;

import java.rmi.RemoteException;
import java.util.Collection;


// TODO: notificar al cliente cuando esta online
public class MainWindowController {

    // Lista de chats (ChatView) que contienen los mensajes
    private ObservableList<Node> chatLists;

    // Listas de elementos
    @FXML private ListView<String> pendingRequests;
    @FXML private ListView<String> searchResult;
    @FXML private ListView<String> contacts;

    // Text input
    @FXML private TextField inputMsg;
    @FXML private TextField inputSearchUsers;
    @FXML private PasswordField inputNewPassword2;
    @FXML private PasswordField inputNewPassword1;
    @FXML private PasswordField inputOldPassword;

    // Elementos de la GUI que se actualizan dinamicamente
    @FXML private Label inputFailed;
    @FXML private SplitPane splitPane;
    @FXML private VBox chatPane;

    @FXML
    public void initialize() {
        ClientImpl.getInstance().setMainWindowController(this);

        // Notificar de los amigos ya conectados
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


    // ==== CHATS ======================================================================================================

    @FXML
    public void sendMessage() {
        System.out.println("Enviar mensaje");
    }

    @FXML
    public void openChat(MouseEvent mouseEvent) {
        // Obtener el ítem seleccionado del ListView
        String selectedItem = contacts.getSelectionModel().getSelectedItem();
        System.out.println("Has seleccionado el chat con " + selectedItem);

        // Si es la primera vez que se habre un chat, se borra el mensaje de aviso
        if (chatLists == null) {
            chatPane.getChildren().clear();

            Label lblChatName = new Label(selectedItem);
            lblChatName.setId("lblChatName"); // Para el CSS
            chatPane.getChildren().add(lblChatName);

            StackPane stackPane = new StackPane();
            chatLists = stackPane.getChildren();
            chatPane.setVgrow(stackPane, Priority.ALWAYS);

            chatLists.add(new ChatView(contacts.getItems().get(0)));

            HBox hbox = new HBox();
            inputMsg = new TextField();
            inputMsg.setId("inputMsg");
            inputMsg.setPromptText("Escribe un mensaje");
            hbox.setHgrow(inputMsg, Priority.ALWAYS);

            Button btnSend = new Button("Enviar");
            btnSend.setOnAction((ActionEvent action) -> {
                sendMessage();
            });
            hbox.getChildren().add(btnSend);
        } else {
            ChatView selected = null;
            for (Node n : chatLists) {
                if (n instanceof ChatView chatView && chatView.fromUser(selectedItem)) {
                    // TODO: mostrar el chatview en el stackpane
                    selected = chatView;
                    break;
                }
            }
            if (selected == null) {
                chatLists.add(new ChatView(selectedItem));
            } else {
                chatPane.getChildren().remove(selected);
                chatPane.getChildren().add(selected);
            }
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
            createAlert("INFORMATION","Solicitud existente","Ya hay una solicitud pendiente","Espere a la respuesta del otro usuario");
        } catch (NotFoundException e) {
            createAlert("INFORMATION","Usuario desconocido","Usuario desconocido","No se ha encontrado al usuario de ID \"%s\"".formatted(other));
        } catch (RemoteException e) {
            // TODO: alert?
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
            // TODO: alert?
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

    //Nada, metodo inncecesario por ahora, ignorar
    @FXML
    public void printsSelectedUser(MouseEvent mouseEvent) {
        String selectedItem = searchResult.getSelectionModel().getSelectedItem();
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
