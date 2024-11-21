package org.example.compdis_p2p;

import javax.naming.AuthenticationException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface ServidorInterface extends Remote {

    ArrayList<Client> getFriends(Client client) throws NoOnlineException , RemoteException;

    void connect(Client client) throws AuthenticationException, RemoteException;

    void disconnect(Client client) throws RemoteException;

    void registerClient(Client client) throws RemoteException;

}
