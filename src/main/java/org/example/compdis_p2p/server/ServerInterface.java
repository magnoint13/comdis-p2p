package org.example.compdis_p2p.server;

import org.example.compdis_p2p.NotOnlineException;
import org.example.compdis_p2p.AuthException;
import org.example.compdis_p2p.client.ClientInterface;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

public interface ServerInterface extends Remote {

    Collection<ClientInterface> getConnectedClients() throws RemoteException;

    void connect(ClientInterface client) throws AuthException, RemoteException;

    void disconnect(ClientInterface client) throws RemoteException;

    void registerClient(ClientInterface client) throws RemoteException;

}
