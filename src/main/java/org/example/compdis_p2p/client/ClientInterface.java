package org.example.compdis_p2p.client;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

// TODO: metodo ping que permita al servidor determinar si todavia esta online
// puede que alguno de los clientes termine con errores y no realicen el disconnect
public interface ClientInterface extends Remote {
    /**
     * `this` envia un mensaje a `client` de contenido `message`
     */
    void message(ClientInterface client, String message) throws RemoteException;

    /**
     * Se notifica a `this` que `friend` se acaba de conectar
     */
    void notificationFriendConnected(ClientInterface friend) throws RemoteException;

    /**
     * Cuando un cliente se conecta, recibe quienes de sus amigos están conectados
     */
    void setFriendsOnline(Collection<ClientInterface> friendsOnline) throws RemoteException;

    String getUsername() throws RemoteException;

    String getPassword() throws RemoteException;
}
