package org.comdis.p2p.server;

import org.comdis.p2p.ClientCallback;
import org.comdis.p2p.ClientPtp;
import org.comdis.p2p.RemoteClient;
import org.comdis.p2p.ServerInterface;
import org.comdis.p2p.exceptions.AlreadyExistsException;
import org.comdis.p2p.exceptions.AuthException;
import org.comdis.p2p.exceptions.NotFoundException;
import org.comdis.p2p.exceptions.PetitionFromOtherExistsException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clase Servidor
 * <br><br>
 * Implementa la interfaz remota `ServerInterface`.
 */
class Server extends UnicastRemoteObject implements ServerInterface {

    // El servidor manejará cada peticion en un hilo diferente (Java RMI),
    // por lo que el estado compartido (este HashMap) debe ser Thread-Safe.
    //
    // Según la documentacion, no se permiten claves `null`, lo que permite
    // usar `get` y ver si el resultado es `null` para ver dicho elemento
    // existe en la estructura de datos.
    private final ConcurrentHashMap<String, Client> connectedClients;

    private final DataBaseController database;

    public Server(String databaseFile) throws SQLException, IOException {
        super();
        database = new DataBaseController(databaseFile);
        connectedClients = new ConcurrentHashMap<>();
    }

    // ==== CONECTARSE/DESCONECTARSE ===================================================================================

    @Override
    public RemoteClient connect(String username, String password, ClientCallback callbacks, ClientPtp msgCallback) throws AuthException, RemoteException, AlreadyExistsException {
        // Comprobar si ya estaba conectado
        Client newClient = connectedClients.get(username);
        if (newClient != null) {
            throw new AlreadyExistsException("El usuario \"%s\" ya esta conectado".formatted(username));
        }

        // Lanza AuthException si el usuario no es valido
        database.checkUserCredentials(username, password);

        // De lo contrario se crea
        System.out.printf("Cliente conectado: %s\n", username);
        newClient = new Client(username, callbacks, msgCallback);

        // Notificar a sus amigos de que alguien se ha conectado
        ArrayList<String> friends = database.getFriends(username);
        ArrayList<RemoteClient> onlineFriends = new ArrayList<>();

        // Debug only
        System.out.println("Amigos:");
        for (String friendUsername : friends) {
            System.out.printf("\t%s\n", friendUsername);
        }

        System.out.println("Amigos online:");
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
        System.out.println(); // Línea en blanco

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
            System.out.printf("El usuario %s no estaba conectado, no se puede desconectar\n", remoteClient.getUsername());
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
        System.out.println(); // Línea en blanco
    }

    // ==== CREAR/BORRAR USUARIOS ======================================================================================

    @Override
    public void createUser(String username, String password) throws RemoteException, AlreadyExistsException {
        database.addUser(username, password);
        System.out.println("Inscripcion de nuevo cliente: " + username);
    }

    @Override
    public void deleteUser(RemoteClient client, String password) throws RemoteException, AuthException {
        database.deleteUser(client.getUsername(), password);
        System.out.println("Borrar cliente: " + client.getUsername());
        // Importante desconectarlo despues de haberlo borrado,
        // porque sino el sera desconectado sin haberlo borrado.
        disconnect(client);
    }

    // ==== BUSCAR USUARIOS ============================================================================================

    @Override
    public Collection<String> searchUsernames(RemoteClient searcher, String username) throws RemoteException {
        System.out.printf("El cliente %s busca usuarios de query \"%s\"\n", searcher.getUsername(), username);
        return database.searchUsernames(username, searcher.getUsername());
    }

    // ==== PETICIONES DE AMISTAD ======================================================================================

    @Override
    public void sendFriendRequest(RemoteClient from, String to) throws NotFoundException, AlreadyExistsException, RemoteException, PetitionFromOtherExistsException {
        // Almacenar la peticion en la BD para que constancia de ello
        if (database.pendingFromOtherExists(to, from.getUsername())) {
            System.out.printf("Aviso de peticiones de amistad cruzadas entre \"%s\" y \"%s\"\n", from.getUsername(), to);
            throw new PetitionFromOtherExistsException("Ya existe una solictud pendiente del usuario " + to + ", por ende se acepta esa solicitud automáticamente");
        }

        database.sendFriendRequest(from.getUsername(), to);
        System.out.printf("Peticion de amistad de \"%s\" a \"%s\"\n", from.getUsername(), to);

        // Si el receptor esta online, ya se le notifica
        Client receiver = connectedClients.get(to);
        if (receiver != null) {
            receiver.newFriendRequest(from.getUsername());
        }
    }

    @Override
    public void acceptFriendRequest(RemoteClient accepts, String originalSender) throws NotFoundException, RemoteException, AlreadyExistsException {
        database.acceptFriendRequest(accepts.getUsername(), originalSender);
        System.out.printf("Cliente \"%s\" acepta la peticion de amistad de \"%s\"\n", accepts.getUsername(), originalSender);

        // Si los usuarios están conectados, entonces se les notifica que están online
        // De esta forma, podran empezar a mandarse mensajes
        Client clientA = connectedClients.get(accepts.getUsername());
        Client clientB = connectedClients.get(originalSender);

        if (clientA != null && clientB != null) {
            clientA.friendConnected(clientB.getHandle());
            clientB.friendConnected(clientA.getHandle());
        }
    }

    @Override
    public void cancelFriendRequest(RemoteClient cancels, String other) throws NotFoundException, RemoteException {
        database.cancelFriendRequest(cancels.getUsername(), other);
        System.out.printf("Cliente \"%s\" cancela la peticion de amistad de \"%s\"\n", cancels.getUsername(), other);
    }

    @Override
    public void deleteFriendship(RemoteClient client, String other) throws NotFoundException, RemoteException {
        database.deleteFriendship(client, other);
        System.out.printf("Cliente \"%s\" borra su amistad con \"%s\"\n", client.getUsername(), other);

        Client formerFriend = connectedClients.get(other);
        if (formerFriend != null) {
            formerFriend.friendshipFinished(client.getUsername());
        }
    }

    // ==== OTROS ======================================================================================================

    @Override
    public void changePassword(RemoteClient client, String oldPassword, String newPassword) throws AuthException, RemoteException {
        database.changePassword(client.getUsername(), oldPassword, newPassword);
        System.out.println("Cambia su contraseña: " + client.getUsername());
    }

    @Override
    public Collection<String> getFriends(RemoteClient client) throws RemoteException {
        return database.getFriends(client.getUsername());
    }
}
