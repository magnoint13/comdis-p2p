package org.comdis.p2p;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Objects;

/**
 * Representa un cliente que no está en la máquina actual.
 * <br><br>
 * Es esencialmente un wrapper de un ClientPtp que incluye metadatos (nombre de usuario).
 * <br><br>
 * Esta clase existe principalmente para evitarse llamadas a `getUsername()`, que en
 * realidad tramitan la llamada a traves de la red, lo que puede ser especialmente
 * lento si se intenta usar en `contains` y similares. En su lugar, se almacena dicho
 * String en la memoria de esta máquina.
 */
public class RemoteClient implements Serializable {
    private final String username;
    private final ClientPtp handle;

    public RemoteClient(String username, ClientPtp handle) {
        this.username = username;
        this.handle = handle;
    }

    public String getUsername() {
        return username;
    }

    public void sendMessage(RemoteClient sender, String msg) throws RemoteException {
        handle.message(sender, msg);
    }

    @Override
    public String toString() {
        return username;
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

        RemoteClient other = (RemoteClient) obj;
        return other.username.equals(this.username);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(username);
    }
}
