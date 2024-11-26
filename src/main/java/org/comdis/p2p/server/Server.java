package org.comdis.p2p.server;

import org.comdis.p2p.ClientCallback;
import org.comdis.p2p.ClientPtp;
import org.comdis.p2p.RemoteClient;
import org.comdis.p2p.ServerInterface;
import org.comdis.p2p.exceptions.AlreadyExistsException;
import org.comdis.p2p.exceptions.AuthException;
import org.comdis.p2p.exceptions.NotFoundException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clase Servidor
 */
public class Server extends UnicastRemoteObject implements ServerInterface {

    // El servidor manejará cada peticion en un hilo diferente (Java RMI),
    // por lo que el estado compartido (este HashMap) debe ser Thread-Safe.
    //
    // Según la documentacion, no se permiten claves `null`, lo que permite
    // usar `get` y ver si el resultado es `null` para ver si hubo exito.
    private final ConcurrentHashMap<String, Client> connectedClients;

    private final DataBaseController database;

    public Server(String databaseFile, String creationString) throws SQLException, IOException {
        super();
        database = new DataBaseController(databaseFile, creationString);
        connectedClients = new ConcurrentHashMap<>();
    }

    // ==== CONECTARSE/DESCONECTARSE ===================================================================================

    @Override
    public RemoteClient connect(String username, String password, ClientCallback callbacks, ClientPtp msgCallback) throws AuthException, RemoteException {
        // Comprobar si ya estaba conectado
        Client newClient = connectedClients.get(username);
        if (newClient != null) {
            return newClient.getHandle();
        }

        // Lanza AuthException si el usuario no es valido
        database.checkUserCredentials(username, password);

        // De lo contrario se crea
        System.out.printf("Cliente conectado: %s\n", username);
        newClient = new Client(username, callbacks, msgCallback);

        // Notificar a sus amigos de que alguien se ha conectado
        ArrayList<String> friends = database.getFriends(username);
        ArrayList<RemoteClient> onlineFriends = new ArrayList<>();

        System.out.printf("Amigos online: %s\n", friends);
        for (String friendUsername : friends) {
            Client friend = connectedClients.get(friendUsername);

            // Si este amigo esta online se añade a la lista y se le
            // envia una notificacion.
            if (friend != null) {
                onlineFriends.add(friend.getHandle());
                friend.friendConnected(newClient.getHandle());
                System.out.println("\t" + friend.getUsername());
            }
        }

        // Enviar al nuevo cliente quienes están conectados
        newClient.friendsOnline(onlineFriends);
        // También notificar de las solicitudes de amistad
        newClient.friendRequests(database.getPendingRequests(username));

        // Registrar al cliente como online
        // Debe ser lo último para que no se envien notificaciones repetidas
        connectedClients.put(username, newClient);
        return newClient.getHandle();
    }

    @Override
    public void disconnect(RemoteClient remoteClient) throws RemoteException {
        Client client = connectedClients.get(remoteClient.getUsername());

        // Si no estaba conectado, no hacer nada
        if (client == null) {
            return;
        }

        // TODO: esto no se si esto realmente comprueba que es el mismo objeto
        //       o si se puede trampear de alguna forma. Si funciona, cambiar el printf
        //       por AuthException.
        if (!client.getHandle().equals(remoteClient)) {
            System.out.printf(
                    "Cliente \"%s\" no coincide con el almacenado en el servidor\n",
                    remoteClient.getUsername()
            );
        }

        // Quitar de la lista de usuarios online
        connectedClients.remove(client.getUsername());
        System.out.printf("Cliente desconectado: %s\nNotificando a sus amigos:\n", client.getUsername());

        // Notificar a sus amigos de que se ha desconectado
        Collection<String> friends = database.getFriends(client.getUsername());
        for (String friendUsername : friends) {
            Client friend = connectedClients.get(friendUsername);

            // Si este amigo esta online se añade a la lista y se le
            // envia una notificacion.
            if (friend != null) {
                friend.friendDisconnected(client.getHandle());
                System.out.println("\t" + friend.getUsername());
            }
        }
    }

    // ==== CREAR/BORRAR USUARIOS ======================================================================================

    @Override
    public void createUser(String username, String password) throws RemoteException, AlreadyExistsException {
        database.addUser(username, password);
    }

    @Override
    public void deleteUser(String username, String password) throws RemoteException, AuthException {
        database.deleteUser(username, password);
    }

    // ==== BUSCAR USUARIOS ============================================================================================

    @Override
    public Collection<String> searchUsernames(String username) throws RemoteException {
        return database.searchUsernames(username);
    }

    // ==== PETICIONES DE AMISTAD ======================================================================================

    @Override
    public void sendFriendRequest(RemoteClient from, String to) throws NotFoundException, AlreadyExistsException, RemoteException {
        // Almacenar la peticion en la BD para que constancia de ello
        database.sendFriendRequest(from.getUsername(), to);

        // Si el receptor esta online, ya se le notifica
        Client receiver = connectedClients.get(to);
        if (receiver != null) {
            receiver.newFriendRequest(from.getUsername());
        }
    }

    @Override
    public void acceptFriendRequest(RemoteClient accepts, String originalSender) throws NotFoundException, RemoteException, AlreadyExistsException {
        database.acceptFriendRequest(accepts.getUsername(), originalSender);
        // TODO: notificar a originalSender que se ha aceptado
    }

    @Override
    public void cancelFriendRequest(RemoteClient cancels, String other) throws NotFoundException, RemoteException {
        database.cancelFriendRequest(cancels.getUsername(), other);
        // TODO: solo notificar a originalSender si no es el quien la cancela
    }
}
