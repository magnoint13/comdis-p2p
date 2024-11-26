package org.comdis.p2p.client;

import org.comdis.p2p.ClientCallback;
import org.comdis.p2p.ClientPtp;
import org.comdis.p2p.RemoteClient;
import org.comdis.p2p.ServerInterface;
import org.comdis.p2p.exceptions.AlreadyExistsException;
import org.comdis.p2p.exceptions.AuthException;
import org.comdis.p2p.exceptions.NotFoundException;
import org.comdis.p2p.exceptions.PtpException;
import org.comdis.p2p.server.MainServer;

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
class ClientCallbackImpl extends UnicastRemoteObject implements ClientCallback, ClientPtp, AutoCloseable {

    // ==== SINGLETON ==================================================================================================

    private static final ClientCallbackImpl instance;

    static {
        try {
            instance = new ClientCallbackImpl();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ClientCallbackImpl getInstance() {
        return instance;
    }

    // ==== CONSTRUCTOR ================================================================================================

    private transient ServerInterface server;
    private transient RemoteClient handle;

    private transient ConcurrentHashMap<String, RemoteClient> friendsOnline;
    private transient Collection<String> usersPendingRequests;

    private ClientCallbackImpl() throws RemoteException, MalformedURLException, NotBoundException {
        super();

        // Se necesita obtener la referencia al servidor ahora, porque hay peticiones
        // (como crear usuario) que no requieren de que este conectado.
        // TODO: recibir de la GUI la URL?
        server = (ServerInterface) Naming.lookup(MainServer.REGISTRY_URL);
        friendsOnline = new ConcurrentHashMap<>();

        // TODO: puede que no funcione. Para que la JVM termine, se tiene que quitar el objeto remoto,
        //       que se hace precisamente en el close que se ejecuta a continuacion.
        // Como al final de la ejecucion se necesita cerrar la conexion, se configura un shutdownHook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                ClientCallbackImpl.getInstance().close();
            } catch (RemoteException e) {
                PtpException.logError(e);
                System.exit(1);
            }
        }));
    }

    // ==== CONECTAR Y DESCONECTAR =====================================================================================

    public void connect(String username, String password) throws AuthException {
        try {
            handle = server.connect(username, password, this, this);
        } catch (RemoteException e) {
            // TODO: quizas mejor especializar las excepciones y hacer un mejor tratamiento de errores
            PtpException.logError(e);
        }
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
        UnicastRemoteObject.unexportObject(this, true);
        server = null;
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

    /** Nota: lanza NullPointerException cuando no esta conectado */
    public void deleteUser(String password) throws AuthException, RemoteException {
        server.deleteUser(handle.getUsername(), password);
        // TODO: no estoy muy seguro de esto
        close();
    }

    public Collection<String> searchUsernames(String username) throws RemoteException {
        return server.searchUsernames(handle, username);
    }

    public void sendFriendRequest(String to) throws AlreadyExistsException, NotFoundException, RemoteException {
        server.sendFriendRequest(handle, to);
    }

    public void acceptFriendRequest(String from) throws AlreadyExistsException, NotFoundException, RemoteException {
        server.acceptFriendRequest(handle, from);
    }

    public void cancelFriendRequest(String other) throws NotFoundException, RemoteException {
        server.cancelFriendRequest(handle, other);
    }

    // ==== ENVIAR MENSAJE =============================================================================================

    public void sendMessage(RemoteClient friendHandle, String msg) throws RemoteException {
        friendHandle.message(msg);
    }

    public void sendMessage(String friend, String msg) throws RemoteException, NotFoundException {
        RemoteClient friendHandle = friendsOnline.get(friend);
        if (friendHandle == null) {
            throw new NotFoundException("El usuario \"%s\" no esta online".formatted(friend));
        }

        sendMessage(friendHandle, msg);
    }

    // =================================================================================================================
    // ==== CALLBACKS ==================================================================================================
    // =================================================================================================================

    // ==== MENSAJES ===================================================================================================

    @Override
    public void message(RemoteClient sender, String message) throws RemoteException {
        System.out.printf("Mensaje de %s: %s\n", sender.getUsername(), message);
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
        friendsOnline.put(friend.getUsername(), friend);
    }

    @Override
    public void friendDisconnected(RemoteClient friend) throws RemoteException {
        System.out.printf("Amigo offline: %s\n", friend.getUsername());
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
        usersPendingRequests.add(username);
    }
}
