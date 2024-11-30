package org.comdis.p2p.client;

import javafx.application.Platform;
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

    private final transient ConcurrentHashMap<String, RemoteClient> friendsOnline;
    private transient Collection<String> usersPendingRequests;

    // Objetos remotos
    private transient RemoteClient handle;
    private transient ServerInterface server;

    // Para poder actualizar cambios en la GUI
    private transient MainWindowController mainWindowController;

    // ==== SETTERS ====================================================================================================

    public void setMainWindowController(MainWindowController mainWindowController) {
        this.mainWindowController = mainWindowController;
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

    // ==== CONECTAR Y DESCONECTAR =====================================================================================

    public void serverConnect(String url) throws MalformedURLException, NotBoundException, RemoteException {
        server = (ServerInterface) Naming.lookup(url);
    }

    public void connect(String username, String password) throws AuthException, AlreadyExistsException, RemoteException {
        if (isOnline()) {
            throw new AlreadyExistsException("El usuario ya esta conectado como \"%s\"".formatted(handle.getUsername()));
        }

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
    public void deleteUser(String username, String password) throws AuthException, RemoteException {
        server.deleteUser(username, password);
        disconnect();
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
        server.deleteFriendship(handle,to);
    }

    public Collection<String> getFriends() throws NotFoundException, RemoteException{
        return server.getFriends(handle);
    }



    // ==== ENVIAR MENSAJE =============================================================================================

    public void sendMessage(RemoteClient friendHandle,RemoteClient sender, String msg) throws RemoteException {
        friendHandle.sendMessage(sender, msg);
    }

    public void sendMessage(String friend, String msg) throws RemoteException, NotFoundException {
        RemoteClient friendHandle = friendsOnline.get(friend);
        // TODO: a lo mejor lanzar un mensaje mas especifico, puede no existir el usuario simplemente
        //       El problema es que para saber si existe realmente, hace falta consultar al server.
        //       Creo que para un mensaje de error es innecesario.
        if (friendHandle == null) {
            throw new NotFoundException("El usuario \"%s\" no esta online".formatted(friend));
        }

        sendMessage(friendHandle,handle, msg);
    }

    // =================================================================================================================
    // ==== CALLBACKS ==================================================================================================
    // =================================================================================================================

    // ==== MENSAJES ===================================================================================================

    @Override
    public void message(RemoteClient sender, String message) throws RemoteException {
        System.out.printf("Mensaje de %s: %s\n", sender.getUsername(), message);
        Platform.runLater(() -> {
            mainWindowController.receiveMessage(sender.getUsername(),message);
        });
    }

    // ==== NOTIFICACIONES DE AMIGOS ONLINE ============================================================================

    @Override
    public void friendsOnline(Collection<RemoteClient> friendsOnline) throws RemoteException {
        this.friendsOnline.clear();

        System.out.println("Amigos online:");
        for (RemoteClient friend : friendsOnline) {
            this.friendsOnline.put(friend.getUsername(), friend);
            System.out.println('\t' + friend.getUsername());
        }
    }

    @Override
    public void friendConnected(RemoteClient friend) throws RemoteException {
        System.out.printf("Amigo online: %s\n", friend.getUsername());
        mainWindowController.createAlert("INFORMATION","Amigo conectado","Se ha conectado un amigo","Usuario: " + friend.getUsername());
        mainWindowController.addContact(friend.getUsername());
        friendsOnline.put(friend.getUsername(), friend);
    }

    @Override
    public void friendDisconnected(RemoteClient friend) throws RemoteException {
        System.out.printf("Amigo offline: %s\n", friend.getUsername());
        mainWindowController.createAlert("INFORMATION","Amigo desconectado","Se ha desconectado un amigo","Usuario: " + friend.getUsername());
        mainWindowController.removeContact(friend.getUsername());
        mainWindowController.deleteChat(friend.getUsername());
        friendsOnline.remove(friend.getUsername());
    }

    @Override
    public void friendshipFinished(RemoteClient friend) throws RemoteException {
        System.out.printf("Amistad con %s finalizada\n", friend.getUsername());
        mainWindowController.createAlert("INFORMATION","Amistad finalizada","Se ha finalizado una amistad","Usuario: " + friend.getUsername());
        mainWindowController.removeContact(friend.getUsername());
        mainWindowController.deleteChat(friend.getUsername());
        mainWindowController.removeFriend(friend.getUsername());
        friendsOnline.remove(friend.getUsername());
    }

    // ==== NOTIFICACIONES DE PETICIONES DE AMISTAD ====================================================================

    @Override
    public void friendRequests(Collection<String> requests) throws RemoteException {
        usersPendingRequests = requests;
        System.out.println("Peticiones de amistad: ");
        for (String username : requests) {
            System.out.println('\t' + username);
        }
    }

    @Override
    public void newFriendRequest(String username) throws RemoteException {
        System.out.printf("Nueva peticion de amistad: %s\n", username);
        mainWindowController.createAlert("INFORMATION","Nueva solicitud amistad","Ha recibido una solicitud de amistad nueva","Usuario: " + username);
        mainWindowController.addPendingRequest(username);
        usersPendingRequests.add(username);
    }

    // ==== AJUSTES ====================================================================================================

    @Override
    public void changePassword(String username,String oldpassword, String newpasword) throws AuthException, RemoteException{
        server.changePassword(username,oldpassword,newpasword);
    }

}
