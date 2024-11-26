package org.comdis.p2p;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Representa las posibles interacciones que se pueden realizar con un cliente remoto.
 */
public interface ClientPtp extends Remote {
    /**
     * `this` recibe un mensaje a de `sender` de contenido `message`
     */
    void message(RemoteClient sender, String message) throws RemoteException;
}
