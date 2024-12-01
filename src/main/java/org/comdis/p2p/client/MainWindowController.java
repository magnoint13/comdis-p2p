package org.comdis.p2p.client;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
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

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Optional;


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

        // Configurar el CellFactory para poner los colores de forma apropiada
        lstContacts.setCellFactory(lv -> new ListCell<>() {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                // Actualizar solo si no es vacio
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                // Poner el texto correspondiente
                setText(item);
                setStyle("-fx-cursor:hand;");

                // Si el mensaje no está leido, se pone un color especial
                if (hasUnreadMessage(item)) {
                    setStyle(getStyle() + "-fx-background-color:darkgreen;");
                    return;
                }

                // Si el chat es el abierto actualmente, queda en rojo
                if (openedChat != null && openedChat.fromUser(item)) {
                    setStyle(getStyle() + "-fx-background-color:red;");
                    return;
                }

                // En caso contrario, se ponen dos variantes de verde que se alternan
                String color = (getIndex() % 2 == 0) ? "#32cd32" : "#35b735";
                setStyle(getStyle() + "-fx-background-color:%s;".formatted(color));
            }
        });

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
        if (pendingRequests != null && !pendingRequests.isEmpty()) {
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
        // Configurar el contenido del Alert
        Platform.runLater(() -> {
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
                        // Evitar un error con la alerta que se ejecuta una vez eliminado el usuario
                        if (primaryStage != null) {
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
                    }
                };

                alertStage.widthProperty().addListener(listener);
                alertStage.heightProperty().addListener(listener);
            });
            alert.showAndWait();
        });
    }

    // Metodo privado puesto que solo puede ser llamado desde el hilo principal
    private Optional<ButtonType> createConfirmationAlert(String title, String header, String content){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
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
                    // Evitar un error con la alerta que se ejecuta una vez eliminado el usuario
                    if (primaryStage != null) {
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
                }
            };
            alertStage.widthProperty().addListener(listener);
            alertStage.heightProperty().addListener(listener);
        });
        return alert.showAndWait();
    }

    private boolean hasUnreadMessage(String cell) {
        for (Node n : chatDisplay.getChildren()) {
            if (n instanceof ChatView chatView && chatView.isUnread() && chatView.fromUser(cell)) {
                return true;
            }
        }
        return false;
    }

    // ==== CHATS ======================================================================================================

    @FXML
    public void openChat() {
        chatPane.setVisible(true);
        inputMsg.setDisable(false);

        String selectedUser = lstContacts.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            return;
        }

        lblChatName.setText(selectedUser);

        // Buscar el chat deseado por la interfaz
        ChatView selected = null;
        for (Node n : chatDisplay.getChildren()) {
            if (n instanceof ChatView chatView && chatView.fromUser(selectedUser)) {
                selected = chatView;
                break;
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
        // Se marca como no leído
        selected.setUnread(false);
        // Se actualiza la lista
        lstContacts.refresh();

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
            // Si no es el chat abierto se marca como no leído
            if (openedChat != selected){
                selected.setUnread(true);
                lstContacts.refresh();
            }
        });
    }

    @FXML
    public void sendMessage() {
        String msg = inputMsg.getText().trim();
        if (msg.isEmpty()) {
            return;
        }

        try {
            String receiver = lstContacts.getSelectionModel().getSelectedItem();
            if (receiver == null) {
                return;
            }

            // Enviar el mensaje
            ClientImpl.getInstance().sendMessage(receiver, msg);

            // Mostrar en la GUI que el mensaje enviado
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
        if (other == null) {
            return;
        }

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
            pendingRequests.getItems().remove(newFriend);
        } catch (AlreadyExistsException e) {
            createAlert(
                    Alert.AlertType.INFORMATION,
                    "Ya existe",
                    "Ya existe",
                    e.getMessage()
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
            // Se borra de la lista, por algun motivo no se habia borrado antes
            pendingRequests.getItems().remove(newFriend);
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

        if (newFriend == null) {
            return;
        }

        acceptFriendRequest(newFriend);
    }

    @FXML
    public void rejectFriendRequest(ActionEvent ignoredEvent) {
        String rejected = pendingRequests.getSelectionModel().getSelectedItem();

        if (rejected == null) {
            return;
        }

        try {
            ClientImpl.getInstance().cancelFriendRequest(rejected);
            pendingRequests.getItems().remove(rejected);
        } catch (RemoteException e) {
            PtpException.logError(e);
            createAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Error inesperado",
                    "Mensaje de error: %s".formatted(e.getMessage())
            );
        } catch (NotFoundException e) {
            createAlert(
                    Alert.AlertType.INFORMATION,
                    "Usuario desconocido",
                    "Usuario desconocido",
                    "No se ha encontrado al usuario de ID \"%s\"".formatted(rejected)
            );
        }
    }

    @FXML
    public void getFriends(Event ignoredEvent) {
        try {
            Collection<String> result = ClientImpl.getInstance().getFriends();
            if (result != null) {
                if (!result.isEmpty()) {
                    lstFriends.getItems().clear();
                    lstFriends.getItems().addAll(ClientImpl.getInstance().getFriends());
                }
            }
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
    public void deleteFriendship(ActionEvent ignoredActionEvent) {
        String friend = lstFriends.getSelectionModel().getSelectedItem();
        if (friend == null) {
            return;
        }

        try {
            ClientImpl.getInstance().deleteFriendship(friend);
            removeFriend(friend);
            removeContact(friend);
        } catch (NotFoundException e) {
            createAlert(
                    Alert.AlertType.INFORMATION,
                    "Usuario desconocido",
                    "Usuario desconocido",
                    "No se ha encontrado al usuario de ID \"%s\"".formatted(ClientImpl.getInstance().getUsername())
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

    // ==== AJUSTES ======================================================================================================

    @FXML
    public void changePassword(ActionEvent ignoredActionEvent) {
        if (
                inputNewPassword1.getText().isEmpty()
                        || inputNewPassword2.getText().isEmpty()
                        || inputOldPassword.getText().isEmpty()
        ) {
            lblInputFailed.setVisible(true);
            lblInputFailed.setText("Debe rellenar todos los campos");
            return;
        }

        if (!inputNewPassword1.getText().equals(inputNewPassword2.getText())) {
            lblInputFailed.setText("Las contraseñas introducidas no coinciden");
            lblInputFailed.setVisible(true);
            return;
        }

        lblInputFailed.setVisible(false);

        try {
            // Realizar la peticion al servidor
            ClientImpl.getInstance().changePassword(
                    inputOldPassword.getText(),
                    inputNewPassword1.getText()
            );

            // Notificar que la operacion fue correcta
            createAlert(
                    Alert.AlertType.INFORMATION,
                    "Operación exitosa",
                    "Contraseña actualizada correctamente",
                    ""
            );

            // Limpiar los campos de texto
            inputOldPassword.setText("");
            inputNewPassword1.setText("");
            inputNewPassword2.setText("");

        } catch (AuthException e) {
            lblInputFailed.setText("La contraseña es invalida");
            lblInputFailed.setVisible(true);
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
    public void deleteUser(ActionEvent ignoredActionEvent) {
        lblInputFailed.setVisible(false);
        if (inputOldPassword.getText().isEmpty()) {
            lblInputFailed.setVisible(true);
            lblInputFailed.setText("Introduzca su contraseña para poder borrar la cuenta");
            return;
        }

        // Mostrar el cuadro de diálogo y esperar la respuesta del usuario
        Optional<ButtonType> result = createConfirmationAlert(
                "Borrar usuario",
                "¿Está seguro de que desea borrar su cuenta?",
                "Responda, por favor."
        );

        // Procesar la respuesta del usuario
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            ClientImpl.getInstance().deleteUser(inputOldPassword.getText());

            // Abrir la ventana de inicio
            ClientApp.launchInicio((Stage) lblUsername.getScene().getWindow());

            createAlert(
                    Alert.AlertType.INFORMATION,
                    "Operación exitosa",
                    "Usuario eliminado correctamente",
                    "Lamentamos que haya decidido cerrar su cuenta. Le esperamos de vuelta :)"
            );
        } catch (AuthException e) {
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
        } catch (IOException e) {
            PtpException.logError(e);
        }
    }

    @FXML
    public void logOut(ActionEvent ignoredActionEvent) {
        try {
            // Abrir la ventana de inicio
            ClientImpl.getInstance().disconnect();
            ClientApp.launchInicio((Stage) lblUsername.getScene().getWindow());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
