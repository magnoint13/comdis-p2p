package org.comdis.p2p.client;

import org.comdis.p2p.ClientCallback;
import org.comdis.p2p.ClientPtp;
import org.comdis.p2p.RemoteClient;
import org.comdis.p2p.ServerInterface;
import org.comdis.p2p.exceptions.AlreadyExistsException;
import org.comdis.p2p.exceptions.AuthException;
import org.comdis.p2p.exceptions.PtpException;
import org.comdis.p2p.server.MainServer;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;

class ClientTest implements ClientCallback, ClientPtp, AutoCloseable {
    private final String username;

    // ==== ATRIBUTOS Y CONSTRUCTOR ====================================================================================
    private final String password;
    public ServerInterface server;
    public RemoteClient handle;
    private Collection<RemoteClient> friendsOnline;
    private Collection<String> usersPendingRequests;
    public ClientTest(String username, String password) throws RemoteException {
        this.server = null;
        this.username = username;
        this.password = password;
        this.friendsOnline = new ArrayList<>();
    }

    public static void main(String[] args) throws RemoteException, MalformedURLException, NotBoundException {

        System.out.print("Nombre: ");
        String username = System.console().readLine().trim();
        System.out.print("Contraseña: ");
        String password = System.console().readLine().trim();

        try (ClientTest client = new ClientTest(username, password)) {

            System.out.println("Crear nuevo? (s/N): ");
            String crear = System.console().readLine().trim();
            if (crear.charAt(0) == 's' || crear.charAt(0) == 'S') {
                try {
                    client.server.createUser(username, password);
                    System.out.println("creado");
                } catch (AlreadyExistsException error) {
                    System.out.println(error.getMessage());
                }
            }

            try {
                client.connect();
            } catch (AuthException error) {
                System.out.println(error.getMessage());
                System.exit(1);
            }

            System.out.print("Mensaje a: ");
            String friendUsername = System.console().readLine().trim();
            System.out.print("Mensaje: ");
            String msg = System.console().readLine().trim();

            RemoteClient friend = client.getFriendByUsername(friendUsername);
            if (friend != null) {
                client.message(friend, msg);
                System.out.println("enviado");
            } else {
                System.out.println("ese usuario no esta online");
            }
        }
    }


    // ==== CONECTAR Y DESCONECTAR =====================================================================================

    private void connect() throws AuthException {
        try {
            // TODO: recibir de la GUI la URL?
            server = (ServerInterface) Naming.lookup(MainServer.REGISTRY_URL);
            handle = server.connect(username, password, this, this);
            System.out.println("conectado");
        } catch (AuthException error) {
            // Me interesa detectar este error desde main
            throw error;
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

            System.out.println("desconectado");

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

    public RemoteClient getFriendByUsername(String username) {
        for (RemoteClient friend : friendsOnline) {
            if (friend.getUsername().equals(username)) {
                return friend;
            }
        }

        return null;
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

        ClientTest other = (ClientTest) obj;
        return other.username.equals(this.username);
    }
}
