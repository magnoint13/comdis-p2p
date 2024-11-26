package org.comdis.p2p.client;

import org.comdis.p2p.ClientCallback;
import org.comdis.p2p.ClientPtp;
import org.comdis.p2p.RemoteClient;
import org.comdis.p2p.ServerInterface;
import org.comdis.p2p.exceptions.PtpException;
import org.comdis.p2p.server.MainServer;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;

class ClientCallbackImpl extends UnicastRemoteObject implements ClientCallback, ClientPtp, AutoCloseable {

    private final String username;
    private final String password;
    private ServerInterface server;
    private RemoteClient handle;
    private Collection<RemoteClient> friendsOnline;
    private Collection<String> usersPendingRequests;

    public ClientCallbackImpl(String username, String password) throws RemoteException {
        this.server = null;
        this.username = username;
        this.password = password;
        this.friendsOnline = new ArrayList<>();

        // TODO: hacer esto publico para controlar mejor cuando conectarse?
        connect();
    }

    // ==== CONECTAR Y DESCONECTAR =====================================================================================

    private void connect() {
        try {
            // TODO: recibir de la GUI la URL?
            server = (ServerInterface) Naming.lookup(MainServer.REGISTRY_URL);
            handle = server.connect(username, password, this, this);
        } catch (Exception error) {
            // TODO: quizas mejor especializar las excepciones y hacer un mejor tratamiento de errores
            PtpException.logError(error);
        }
    }

    @Override
    public void close() {
        try {
            server.disconnect(handle);

            // Quitar el objeto remoto para que la JVM pueda terminar
            UnicastRemoteObject.unexportObject(this, true);

        } catch (RemoteException error) {
            PtpException.logError(error);
        }
    }

    // ==== GETTERS ====================================================================================================

    public Collection<RemoteClient> getFriendsOnline() {
        return friendsOnline;
    }

    public Collection<String> getPendingRequests() {
        return usersPendingRequests;
    }

    // ==== MENSAJES ===================================================================================================

    @Override
    public void message(RemoteClient sender, String message) throws RemoteException {
        System.out.printf("Mensaje de %s: %s\n", sender.getUsername(), message);
    }

    // ==== NOTIFICACIONES DE AMIGOS ONLINE ============================================================================

    @Override
    public void friendsOnline(Collection<RemoteClient> friendsOnline) throws RemoteException {
        this.friendsOnline = friendsOnline;

        System.out.println("Amigos online:");
        for (RemoteClient friend : friendsOnline) {
            System.out.println('\t' + friend.getUsername());
        }
    }

    @Override
    public void friendConnected(RemoteClient friend) throws RemoteException {
        System.out.printf("Amigo online: %s\n", friend.getUsername());
        friendsOnline.add(handle);
    }

    @Override
    public void friendDisconnected(RemoteClient friend) throws RemoteException {
        System.out.printf("Amigo offline: %s\n", friend.getUsername());
        friendsOnline.remove(handle);
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

    // ==== NOTIFICACIONES DE AMIGOS ONLINE ============================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        ClientCallbackImpl other = (ClientCallbackImpl) obj;
        return other.username.equals(this.username);
    }
}
