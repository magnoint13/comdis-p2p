package org.example.compdis_p2p.client;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;

// TODO: unexportObject para que la JVM pueda terminar
public class Client extends UnicastRemoteObject implements ClientInterface, ClientPtp{

    private final String name;
    private final String password;
    private Collection<ClientPtp> friendsOnline;
    private Collection<String> usersPendingRequests;

    public Client(String name, String password) throws RemoteException {
        this.name = name;
        this.password = password;
        this.friendsOnline = new ArrayList<ClientPtp>();
    }

    @Override
    public Collection<ClientPtp> getFriendsOnline() throws RemoteException {
        return friendsOnline;
    }

    @Override
    public String getUsername() throws RemoteException {
        return name;
    }

    @Override
    public String getPassword() throws RemoteException {
        return password;
    }

    @Override
    public void message (ClientPtp sender, String message) throws RemoteException {
        System.out.println(sender.getUsername() + " le ha enviado el siguiente mensaje: " + message);
    }

    @Override
    public void setFriendsOnline(Collection<ClientInterface> friendsOnline) throws RemoteException {
        this.friendsOnline.clear();
        System.out.println("Amigos online:");
        for (ClientInterface friend : friendsOnline) {
            System.out.println('\t' + friend.getUsername());
            this.friendsOnline.add((ClientPtp) friend);
        }
    }

    @Override
    public void notificationFriendConnected(ClientInterface other) throws RemoteException {
        System.out.println("El cliente " + other.getUsername() + " se ha conectado");
        friendsOnline.add((ClientPtp) other);
    }

    @Override
    public void notificationFriendDisconnected(ClientInterface other) throws RemoteException {
        System.out.println("El cliente " + other.getUsername() + " se ha desconectado");
        friendsOnline.remove((ClientPtp) other);
    }

    @Override
    public void notifyPendingRequests(Collection<String> pendingRequests) throws RemoteException {
        usersPendingRequests = pendingRequests;
    }

    @Override
    public Collection<String> getPendingRequests() throws RemoteException {
        return usersPendingRequests;
    }

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

        try {
            Client other = (Client) obj;
            return other.getUsername().equals(this.getUsername());
        } catch (RemoteException error) {
            throw new RuntimeException(error);
        }
    }
}
