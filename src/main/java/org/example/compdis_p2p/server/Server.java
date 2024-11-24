package org.example.compdis_p2p.server;

import org.example.compdis_p2p.AlreadyExistsException;
import org.example.compdis_p2p.AuthException;
import org.example.compdis_p2p.client.ClientInterface;
import org.example.compdis_p2p.client.ClientPtp;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

/**
 * Clase Servidor
 */
public class Server extends UnicastRemoteObject implements ServerInterface {

    // TODO: es esta la mejor estructura de datos para esto?
    private final Vector<ClientInterface> connectedClients;

    private final DataBaseController database;

    public Server(String databaseFile, String creationString) throws SQLException, IOException {
        super();
        database = new DataBaseController(databaseFile, creationString);
        connectedClients = new Vector<>();
    }

    @Override
    public void connect(ClientInterface newClient) throws AuthException, RemoteException {
        if (connectedClients.contains(newClient)) {
            return;
        }

        // Lanza AuthException si el usuario no es valido
        database.checkUser(newClient);

        System.out.printf("Cliente conectado: %s\n", newClient.getUsername());

        // Notificar a sus amigos de que alguien se ha conectado
        Collection<ClientInterface> friends = database.getFriends(newClient);
        Collection<ClientInterface> onlineFriends = intersection(connectedClients, friends);

        System.out.printf("Amigos: %s\n", friends);
        for (ClientInterface f : onlineFriends) {
            f.notificationFriendConnected(newClient);
        }

        // Enviar al nuevo cliente quienes están conectados
        newClient.setFriendsOnline(onlineFriends);

        newClient.notifyPendingRequests(database.getPendingRequests(newClient));

        // Registrar al cliente como online
        // Debe ser lo último para que no se envien notificaciones repetidas
        connectedClients.add(newClient);
    }

    @Override
    public void disconnect(ClientInterface client) throws RemoteException {
        // Si no estaba conectado, no hacer nada
        if (!connectedClients.contains(client)) {
            return;
        }

        // Quitar de la lista de usuarios online
        connectedClients.remove(client);

        // Notificar a sus amigos de que se ha desconectado
        Collection<ClientInterface> friends = database.getFriends(client);
        Collection<ClientInterface> onlineFriends = intersection(connectedClients, friends);

        for (ClientInterface f : onlineFriends) {
            f.notificationFriendDisconnected(client);
        }

        System.out.printf("Cliente desconectado: %s\n", client.getUsername());
    }

    @Override
    public void addUser(ClientInterface client) throws RemoteException, AlreadyExistsException {
        database.addUser(client);
    }

    @Override
    public ClientPtp getClient(String name) throws RemoteException {
        for (ClientInterface c : connectedClients) {
            if (c.getUsername().equals(name)) {
                return (ClientPtp) c;
            }
        }

        return null;
    }

    @Override
    public Collection<ClientPtp> searchClientsByName(String name) throws RemoteException {
        return database.searchClientsbyName(name);
    }

    @Override
    public void sendFriendRequest(ClientInterface client, String userName) throws RemoteException{
        database.sendFriendRequest(client,userName);
        client.notifyPendingRequests(database.getPendingRequests(client));
    }

    @Override
    public void createFriendship(ClientInterface client, String other) throws RemoteException{
        database.createFriendship(client,other);
    }

    @Override
    public boolean alreadyFriendRequest(ClientInterface client, String other) throws RemoteException{
        return database.alreadyFriendRequestSended(client,other);
    }

    @Override
    public boolean pendingRequestExisting(ClientInterface client, String other) throws RemoteException {
        return false;
    }

    private <T> Collection<T> intersection(Collection<T> list1, Collection<T> list2) {
        Collection<T> list = new ArrayList<T>();
        for (T t : list1) {
            if(list2.contains(t)) {
                list.add(t);
            }
        }
        return list;
    }
}
