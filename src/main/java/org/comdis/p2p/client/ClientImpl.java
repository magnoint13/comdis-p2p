package org.comdis.p2p.client;

import javafx.scene.control.Alert;
import org.comdis.p2p.ClientCallback;
import org.comdis.p2p.ClientPtp;
import org.comdis.p2p.RemoteClient;
import org.comdis.p2p.ServerInterface;
import org.comdis.p2p.exceptions.*;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstraccion del cliente: encapsula RemoteClient y ServerInterface para gestionar todas las
 * interacciones con otros clientes, el servidor y las notificaciones recibidas. Todos sus campos
 * son transient para que no se envien.
 * <br><br>
 * Se trata de un singleton para que todas las GUIs puedan acceder a él.
 * <br><br>
 * Ninguna clase deberia interactuar con el servidor sin pasar por esta clase.
 */
class ClientImpl extends UnicastRemoteObject implements ClientCallback, ClientPtp, AutoCloseable {

    // ==== SINGLETON ==================================================================================================

    private static ClientImpl instance;

    private ClientImpl() throws RemoteException {
        super();
        friendsOnline = new ConcurrentHashMap<>();
    }

    public static ClientImpl create() throws RemoteException {
        if (instance == null) {
            instance = new ClientImpl();
        }

        return instance;
    }

    public static ClientImpl getInstance() {
        return instance;
    }

    // ==== ATRIBUTOS ==================================================================================================

    // Amigos conectados
    private final transient ConcurrentHashMap<String, RemoteClient> friendsOnline;
    // Peticiones de amistad pendientes
    private transient Collection<String> usersPendingRequests;
    // Mensajes todavia no mostrados en la interfaz
    private transient ConcurrentHashMap<String, ArrayList<String>> pendingMessages;

    // Objetos remotos
    private transient RemoteClient handle;
    private transient ServerInterface server;

    // Para poder actualizar cambios en la GUI
    private transient MainWindowController controller;

    // ==== SETTERS ====================================================================================================

    public void setController(MainWindowController mainWindowController) {
        if (mainWindowController == null) {
            return;
        }

        this.controller = mainWindowController;

        // Si no hay mensajes que actualizar, no se hace nada
        if (pendingMessages == null) {
            return;
        }

        // Dado que se ha establecido ya el controlador, se le pueden enviar los mensajes pendientes
        for (String username : pendingMessages.keySet()) {
            for (String msg : pendingMessages.get(username)) {
                controller.receiveMessage(username, msg);
            }
        }

        // Ahora no es necesario guardar esta informacion
        pendingMessages = null;
    }

    // ==== GETTERS ====================================================================================================

    public Collection<RemoteClient> getFriendsOnline() {
        return friendsOnline.values();
    }

    public Collection<String> getPendingRequests() {
        return usersPendingRequests;
    }

    public String getUsername() {
        return handle.getUsername();
    }

    public boolean isOnline() {
        return handle != null;
    }

    public boolean isOnline(String friendUsername) {
        return friendsOnline.get(friendUsername) != null;
    }

    // ==== CONECTAR Y DESCONECTAR =====================================================================================

    public void serverConnect(String url) throws MalformedURLException, NotBoundException, RemoteException {
        server = (ServerInterface) Naming.lookup(url);
    }

    public void connect(String username, String password) throws AuthException, AlreadyExistsException, RemoteException {
        disconnect();
        handle = server.connect(username, password, this, this);
    }

    public void disconnect() throws RemoteException {
        if (isOnline()) {
            server.disconnect(handle);
            handle = null;
        }
    }

    @Override
    public void close() throws RemoteException {
        disconnect();

        // Quitar el objeto remoto para que la JVM pueda terminar
        if (server != null) {
            UnicastRemoteObject.unexportObject(this, true);
            server = null;
        }
    }

    // ==== PETICIONES AL SERVIDOR =====================================================================================

    public void createUserAndConnect(String username, String password) throws AlreadyExistsException, RemoteException {
        try {
            server.createUser(username, password);
            connect(username, password);
        } catch (AuthException error) {
            // No tiene sentido que de error de autenticacion
            PtpException.logError(error);
        }
    }

    /**
     * Nota: lanza NullPointerException cuando no esta conectado
     */
    public void deleteUser(String password) throws AuthException, RemoteException {
        server.deleteUser(handle, password);
        // El servidor ya lo desconecta por mi, pero tengo que invalidar la referencia
        handle = null;
    }

    public Collection<String> searchUsernames(String username) throws RemoteException {
        return server.searchUsernames(handle, username);
    }

    public void sendFriendRequest(String to) throws AlreadyExistsException, NotFoundException, RemoteException , PetitionFromOtherExistsException {
        server.sendFriendRequest(handle, to);
    }

    public void acceptFriendRequest(String from) throws AlreadyExistsException, NotFoundException, RemoteException {
        server.acceptFriendRequest(handle, from);
    }

    public void cancelFriendRequest(String other) throws NotFoundException, RemoteException {
        server.cancelFriendRequest(handle, other);
    }

    public void deleteFriendship(String to) throws NotFoundException, RemoteException {
        server.deleteFriendship(handle, to);
    }

    public Collection<String> getFriends() throws RemoteException {
        return server.getFriends(handle);
    }

    public void changePassword(String oldPassword, String newPassword) throws AuthException, RemoteException {
        server.changePassword(handle, oldPassword, newPassword);
    }

    // ==== ENVIAR MENSAJE =============================================================================================

    public void sendMessage(String friend, String msg) throws RemoteException, NotFoundException {
        RemoteClient friendHandle = friendsOnline.get(friend);
        if (friendHandle == null) {
            throw new NotFoundException("El usuario \"%s\" no esta online".formatted(friend));
        }

        friendHandle.sendMessage(handle, msg);
    }

    // =================================================================================================================
    // ==== CALLBACKS ==================================================================================================
    // =================================================================================================================

    // ==== MENSAJES ===================================================================================================

    @Override
    public void message(RemoteClient sender, String message) throws RemoteException {
        if (controller != null) {
            controller.receiveMessage(sender.getUsername(), message);
            return;
        }

        // Como existe la posibilidad de recibir un mensaje y el controlador
        // todavia no esta configurado, se almacenan temporalmente en un mapa.
        if (pendingMessages == null) {
            pendingMessages = new ConcurrentHashMap<>();
        }

        ArrayList<String> messages = pendingMessages.get(sender.getUsername());

        if (messages == null) {
            // Añadir una nueva entrada con el usuario y solo un mensaje
            messages = new ArrayList<>();
            messages.add(message);
            pendingMessages.put(sender.getUsername(), messages);
        } else {
            // Añadir el mensaje al usuario correspondiente
            messages.add(message);
        }
    }

    // ==== NOTIFICACIONES DE AMIGOS ONLINE ============================================================================

    @Override
    public void friendsOnline(Collection<RemoteClient> friendsOnline) throws RemoteException {
        this.friendsOnline.clear();
        for (RemoteClient friend : friendsOnline) {
            this.friendsOnline.put(friend.getUsername(), friend);
        }
    }

    @Override
    public void friendConnected(RemoteClient friend) throws RemoteException {
        friendsOnline.put(friend.getUsername(), friend);

        if (controller != null) {
            controller.createAlert(
                    Alert.AlertType.INFORMATION,
                    "Amigo conectado",
                    "Se ha conectado un amigo",
                    "Usuario: " + friend.getUsername());
            controller.addContact(friend.getUsername());
        }
    }

    @Override
    public void friendDisconnected(RemoteClient friend) throws RemoteException {
        friendsOnline.remove(friend.getUsername());

        if (controller != null) {
            controller.createAlert(
                    Alert.AlertType.INFORMATION,
                    "Amigo desconectado",
                    "Se ha desconectado un amigo",
                    "Usuario: " + friend.getUsername()
            );
            controller.removeContact(friend.getUsername());
        }
    }

    @Override
    public void friendshipFinished(String formerFriend) throws RemoteException {
        friendsOnline.remove(formerFriend);

        if (controller != null) {
            controller.createAlert(
                    Alert.AlertType.INFORMATION,
                    "Amistad finalizada",
                    "Se ha finalizado una amistad",
                    "Usuario: " + formerFriend
            );

            controller.removeContact(formerFriend);
            controller.removeFriend(formerFriend);
        }
    }

    // ==== NOTIFICACIONES DE PETICIONES DE AMISTAD ====================================================================

    @Override
    public void friendRequests(Collection<String> requests) throws RemoteException {
        usersPendingRequests = requests;
    }

    @Override
    public void newFriendRequest(String username) throws RemoteException {
        usersPendingRequests.add(username);

        if (controller != null) {
            controller.createAlert(
                    Alert.AlertType.INFORMATION,
                    "Nueva solicitud amistad",
                    "Ha recibido una solicitud de amistad nueva",
                    "Usuario: " + username
            );
            controller.addPendingRequest(username);
        }
    }
}
