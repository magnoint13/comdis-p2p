package org.example.compdis_p2p.client;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientPtp extends Remote {
    /**
     * `this` recibe un mensaje a de `sender` de contenido `message`
     */
    void message (ClientPtp sender, String message) throws RemoteException;

    String getUsername() throws RemoteException;
}
