package org.comdis.p2p.client;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.comdis.p2p.RemoteClient;
import org.comdis.p2p.exceptions.*;
import javafx.beans.value.ChangeListener;

import java.rmi.RemoteException;
import java.util.Collection;


public class MainWindowController {
    // Listas de elementos
    @FXML private ListView<String> pendingRequests;  // Peticiones de amistad
    @FXML private ListView<String> lstSearchResult;  // Resultados de búsqueda de usuarios
    @FXML private ListView<String> lstContacts;      // Amigos online
    @FXML private ListView<String> lstFriends;       // Lista de amigos

    // Text input
    @FXML private TextField inputMsg;
    @FXML private TextField inputSearchUsers;
    @FXML private PasswordField inputNewPassword2;
    @FXML private PasswordField inputNewPassword1;
    @FXML private PasswordField inputOldPassword;

    // Mensaje de error (pestaña de ajustes)
    @FXML private Label lblInputFailed;

    // Muestra el nombre del usuario conectado
    @FXML private Label lblUsername;

    // Elementos del chat
    @FXML private VBox chatPane;          // Contiene StackPane chatDisplay y la zona donde introducir el mensaje
    @FXML private StackPane chatDisplay;  // Lista de chats
    @FXML private Label lblChatName;      // Indica el nombre del usuario del chat
    private ChatView openedChat;          // Chat actualmente activo (debe estar por delante siempre)

    @FXML
    public void initialize() {
        // Indicar al cliente que actualice esta GUI
        ClientImpl.getInstance().setController(this);

        // Añadir un listener de seleccion para abrir el chat correspondiente
        // (Esto no se puede hacer mediante FXML)
        lstContacts.getSelectionModel().selectedItemProperty().addListener(
            (observableValue, oldValue, newValue) -> openChat()
        );

        // Poner el nombre de usuario
        lblUsername.setText("Amigos de %s".formatted(ClientImpl.getInstance().getUsername()));

        // Notificar de los amigos ya conectados y guardar lista de amigos
        Collection<RemoteClient> friendsOnline = ClientImpl.getInstance().getFriendsOnline();
        if (friendsOnline != null && !friendsOnline.isEmpty()) {
            lstContacts.getItems().clear();

            StringBuilder builder = new StringBuilder();
            for (RemoteClient friend : friendsOnline) {
                builder.append(friend.getUsername());
                builder.append('\n');

                // Ya de paso, añadimos amigos a la GUI
                lstContacts.getItems().add(friend.getUsername());
            }

            createAlert(
                    Alert.AlertType.INFORMATION,
                    "Amigos",
                    "Amigos conectados:",
                    builder.toString()
            );
        }

        // Notificar de las peticiones de amistad pendientes
        Collection<String> pendingRequests = ClientImpl.getInstance().getPendingRequests();
        if(pendingRequests != null && !pendingRequests.isEmpty()){
            StringBuilder builder = new StringBuilder();
            for (String solicitud : pendingRequests) {
                builder.append(solicitud);
                builder.append('\n');
            }

            createAlert(
                    Alert.AlertType.INFORMATION,
                    "Solicitudes pendientes",
                    "Tiene solicitudes de amistad pendientes por responder",
                    builder.toString()
            );

            // Mostrar las solicitudes pendientes
            this.pendingRequests.getItems().clear();
            this.pendingRequests.getItems().addAll(ClientImpl.getInstance().getPendingRequests());
        }
    }

    // ==== METODOS PARA ACTUALIZAR LA GUI =============================================================================

    // NOTA: estas funciones se ejecutan desde ClientImpl, probablemente otro hilo.
    // Por eso, es necesario el Platform.runLater.

    public void addContact(String username) {
        Platform.runLater(() -> lstContacts.getItems().add(username));
    }

    public void removeContact(String username) {
        Platform.runLater(() -> {
            // Quita la seleccion para evitar que se cambie a cualquier otro chat
            // De esta forma, el usuario puede seguir viendo los mensajes hasta que seleccione otro chat
            // Entonces, se borraran
            lstContacts.getSelectionModel().clearSelection();
            System.out.println(lstContacts.getSelectionModel().getSelectedItem());

            lstContacts.getItems().remove(username);

            // Si tenia el chat abierto, correspondiente al contacto que se quita
            if (openedChat != null && openedChat.fromUser(username)) {
                // Dejarlo abierto, pero marcarlo como desconectado
                lblChatName.setText(username + " (desconectado)");

                // Tampoco se permiten enviar mensajes
                inputMsg.setText("");
                inputMsg.setDisable(true);
            }
        });
    }

    public void addPendingRequest(String username) {
        Platform.runLater(() -> pendingRequests.getItems().add(username));
    }

    public void removePendingRequest(String username) {
        Platform.runLater(() -> pendingRequests.getItems().remove(username));
    }

    public void addFriend(String username) {
        Platform.runLater(() -> lstFriends.getItems().add(username));
    }

    public void removeFriend(String username) {
        Platform.runLater(() -> lstFriends.getItems().remove(username));
    }

    public void createAlert(Alert.AlertType type, String title, String header, String content) {
        Platform.runLater(() -> {
            // Configurar el contenido del Alert
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);

            // Centrar el Alert en la ventana principal
            alert.setOnShown(ignoredDialogEvent -> {

                Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();

                // Listener para las propiedades de ancho y alto
                ChangeListener<Number> listener = new ChangeListener<>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                        // Si la ventana no se ha configurado aun, estos valores son NaN.
                        if (Double.isNaN(alertStage.getWidth()) || Double.isNaN(alertStage.getHeight())) {
                            return;
                        }

                        // Obtener las coordenadas de la ventana principal
                        Stage primaryStage = (Stage) lstContacts.getScene().getWindow();
                        final double x = primaryStage.getX();
                        final double y = primaryStage.getY();
                        final double width = primaryStage.getWidth();
                        final double height = primaryStage.getHeight();

                        // Calcular la posición del Alert para que esté centrado en la ventana principal
                        alertStage.setX(x + width / 2.0 - alertStage.getWidth() / 2.0);
                        alertStage.setY(y + height / 2.0 - alertStage.getHeight() / 2.0);

                        // Remover los listeners una vez que se haya centrado
                        alertStage.widthProperty().removeListener(this);
                        alertStage.heightProperty().removeListener(this);
                    }
                };

                alertStage.widthProperty().addListener(listener);
                alertStage.heightProperty().addListener(listener);
            });

            alert.showAndWait();
        });
    }

    // ==== CHATS ======================================================================================================

    @FXML
    public void openChat() {
        chatPane.setVisible(true);

        String selectedUser = lstContacts.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            return;
        }

        System.out.println("Has seleccionado el chat con " + selectedUser);
        lblChatName.setText(selectedUser);

        // Buscar el chat deseado por la interfaz
        ChatView selected = null;
        for (Node n : chatDisplay.getChildren()) {
            if (n instanceof ChatView chatView ) {
                if (chatView.fromUser(selectedUser)){
                    selected = chatView;
                    break;
                }
            }
        }

        if (selected == null) {
            // El chat no existe, se crea uno nuevo
            selected = new ChatView(selectedUser);
            VBox.setVgrow(selected, Priority.ALWAYS);
            // Se añade a la interfaz
            chatDisplay.getChildren().add(selected);
        }

        selected.toFront();

        // Ver si el chat anterior era de alguien desconectado
        // En ese caso, será necesario borrarlo
        if (openedChat != null && !ClientImpl.getInstance().isOnline(openedChat.getUsername())) {
            chatDisplay.getChildren().remove(openedChat);

            // Volver a activar el campo de escribir mensajes
            inputMsg.setDisable(false);
        }

        // Ahora ya se puede actualizar el chat actual
        openedChat = selected;
    }

    public void receiveMessage(String sender, String message) {
        Platform.runLater(() -> {
            // Buscar el ChatView al que añadir el mensaje
            ChatView selected = null;
            for (Node n : chatDisplay.getChildren()) {
                if (n instanceof ChatView chatView && chatView.fromUser(sender)) {
                    selected = chatView;
                    break;
                }
            }

            if (selected == null) {
                // Si no se encontró, se crea un nuevo ChatView para el nuevo mensaje
                selected = new ChatView(sender);
                // Se añade al resto de la interfaz
                chatDisplay.getChildren().add(selected);

                // Mantener el chat activo por encima
                if (openedChat != null) {
                    openedChat.toFront();
                }
            }

            // Finalmente, se le añade el mensaje
            selected.addReceivedMsg(message);
        });
    }

    @FXML
    public void sendMessage() {
        if (inputMsg.getText().isEmpty()) {
            return;
        }

        try {
            String msg = inputMsg.getText();
            String receiver = lstContacts.getSelectionModel().getSelectedItem();

            if (receiver == null) {
                return;
            }

            // Enviar el mensaje
            ClientImpl.getInstance().sendMessage(receiver, msg);

            // Mostrar en la GUI que el mensaje enviado
            System.out.println("Enviar mensaje a " + receiver);
            openedChat.addSentMsg(msg);

        } catch (NotFoundException e) {
            // Es imposible que se lance NotFoundException, dado que el usuario
            // lo ha seleccionado en la interfaz.
            PtpException.logError(e);
        } catch (RemoteException e) {
            PtpException.logError(e);
            createAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Error inesperado",
                    "Mensaje de error: %s".formatted(e.getMessage())
            );
        }

        // Limpiar el TextField para el siguiente mensaje
        inputMsg.setText("");
    }

    // ==== BUSCAR AMIGOS ==============================================================================================

    @FXML
    public void searchUsers(ActionEvent ignoredActionEvent) {
        if (inputSearchUsers.getText().isEmpty()) {
            return;
        }

        try {
            Collection<String> result = ClientImpl.getInstance().searchUsernames(inputSearchUsers.getText());
            lstSearchResult.getItems().clear();
            ObservableList<String> items = FXCollections.observableArrayList();
            items.addAll(result);
            lstSearchResult.setItems(items);

        } catch (RemoteException e) {
            PtpException.logError(e);
            createAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Error inesperado",
                    "Mensaje de error: %s".formatted(e.getMessage())
            );
        }
    }

    // ==== SOLICITUDES DE AMISTAD =====================================================================================

    @FXML
    public void sendFriendRequest(ActionEvent ignoredActionEvent) {
        String other = lstSearchResult.getSelectionModel().getSelectedItem();
        try {
            ClientImpl.getInstance().sendFriendRequest(other);
            createAlert(
                    Alert.AlertType.INFORMATION,
                    "Operacion exitosa",
                    "Solicitud enviada",
                    "Se ha enviado una solicitud de amistad al usuario \"%s\"".formatted(other)
            );
        } catch (AlreadyExistsException e) {
            createAlert(
                    Alert.AlertType.INFORMATION,
                    "Solicitud existente",
                    "Ya hay una solicitud pendiente",
                    "Espere a la respuesta del otro usuario"
            );
        } catch (NotFoundException e) {
            createAlert(
                    Alert.AlertType.INFORMATION,
                    "Usuario desconocido",
                    "Usuario desconocido",
                    "No se ha encontrado al usuario de ID \"%s\"".formatted(other));
        } catch (PetitionFromOtherExistsException e) {
            createAlert(
                    Alert.AlertType.INFORMATION,
                    "Solicitud existente",
                    "Existe ya una solicitud de amistad",
                    e.getMessage()
            );
            acceptFriendRequest(other);
        } catch (RemoteException e) {
            PtpException.logError(e);
            createAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Error inesperado",
                    "Mensaje de error: %s".formatted(e.getMessage())
            );
        }
    }

    private void acceptFriendRequest(String newFriend) {
        try {
            ClientImpl.getInstance().acceptFriendRequest(newFriend);
            System.out.println("Has aceptado la solicitud de " + newFriend);
            pendingRequests.getItems().remove(newFriend);
        } catch (AlreadyExistsException e) {
            createAlert(
                    Alert.AlertType.INFORMATION,
                    "Ya existe",
                    "Ya existe",
                    "Tu amistad con \"%s\" ya ha sido establecida".formatted(newFriend)
            );
            // Si estaba selecionado es que no se habia borrado de antes
            pendingRequests.getItems().remove(newFriend);
        } catch (NotFoundException e) {
            createAlert(
                    Alert.AlertType.INFORMATION,
                    "Usuario desconocido",
                    "Usuario desconocido",
                    "No se ha encontrado al usuario de ID \"%s\"".formatted(newFriend)
            );
        } catch (RemoteException e) {
            PtpException.logError(e);
            createAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Error inesperado",
                    "Mensaje de error: %s".formatted(e.getMessage())
            );
        }

    }

    @FXML
    public void acceptFriendRequest(ActionEvent ignoredActionEvent) {
        String newFriend = pendingRequests.getSelectionModel().getSelectedItem();
        acceptFriendRequest(newFriend);
    }

    @FXML
    public void rejectFriendRequest(ActionEvent ignoredEvent) {
        String rejected = pendingRequests.getSelectionModel().getSelectedItem();
        try {
            ClientImpl.getInstance().cancelFriendRequest(rejected);
        } catch (RemoteException e) {
            PtpException.logError(e);
            createAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Error inesperado",
                    "Mensaje de error: %s".formatted(e.getMessage())
            );
        }catch (NotFoundException e){
            createAlert(
                    Alert.AlertType.INFORMATION,
                    "Usuario desconocido",
                    "Usuario desconocido",
                    "No se ha encontrado al usuario de ID \"%s\"".formatted(rejected)
            );
        }
    }

    @FXML
    public void getFriends(Event event) {
        try {
            Collection<String> result = ClientImpl.getInstance().getFriends();
            if(result != null) {
                if (!result.isEmpty()){
                    lstFriends.getItems().clear();
                    lstFriends.getItems().addAll(ClientImpl.getInstance().getFriends());
                }
            }
        } catch (RemoteException e){
            PtpException.logError(e);
            createAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Error inesperado",
                    "Mensaje de error: %s".formatted(e.getMessage())
            );
        } catch (NotFoundException e){
            createAlert(
                    Alert.AlertType.INFORMATION,
                    "Usuario desconocido",
                    "Usuario desconocido",
                    "No se ha encontrado al usuario de ID \"%s\"".formatted(ClientImpl.getInstance().getUsername())
            );
        }
    }

    @FXML
    public void deleteFriendship(ActionEvent actionEvent) {
        String friend = lstFriends.getSelectionModel().getSelectedItem();
        if (friend == null) {
            return;
        }

        try {
            ClientImpl.getInstance().deleteFriendship(friend);
            removeFriend(friend);
            removeContact(friend);
        } catch (NotFoundException e){
            createAlert(
                    Alert.AlertType.INFORMATION,
                    "Usuario desconocido",
                    "Usuario desconocido",
                    "No se ha encontrado al usuario de ID \"%s\"".formatted(ClientImpl.getInstance().getUsername())
            );
        } catch (RemoteException e){
            PtpException.logError(e);
            createAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Error inesperado",
                    "Mensaje de error: %s".formatted(e.getMessage())
            );
        }
    }

    // ==== AJUSTES ======================================================================================================

    @FXML
    public void changePassword(ActionEvent ignoredActionEvent) {
        if (
                inputNewPassword1.getText().isEmpty()
                || inputNewPassword2.getText().isEmpty()
                || inputOldPassword.getText().isEmpty()
        ){
            lblInputFailed.setVisible(true);
            lblInputFailed.setText("Debe rellenar todos los campos");
            return;
        }

        lblInputFailed.setVisible(false);

        if (!inputNewPassword1.getText().equals(inputNewPassword2.getText())) {
            lblInputFailed.setText("Las contraseñas introducidas no coinciden");
            lblInputFailed.setVisible(true);
            return;
        }

        try {
            ClientImpl.getInstance().changePassword(
                    ClientImpl.getInstance().getUsername(),
                    inputOldPassword.getText(),
                    inputNewPassword1.getText()
            );

            createAlert(
                    Alert.AlertType.INFORMATION,
                    "Operación exitosa",
                    "Contraseña actualizada correctamente",
                    ""
            );
        } catch (AuthException e){
            createAlert(
                    Alert.AlertType.ERROR,
                    "Credenciales inválidas",
                    e.getMessage(),
                    "Revise por favor si la contraseña es correcta"
            );
        } catch (RemoteException e) {
            PtpException.logError(e);
            createAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Error inesperado",
                    "Mensaje de error: %s".formatted(e.getMessage())
            );
        }
    }
}
