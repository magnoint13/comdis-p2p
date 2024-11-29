package org.comdis.p2p.server;

import org.comdis.p2p.ClientCallback;
import org.comdis.p2p.ClientPtp;
import org.comdis.p2p.RemoteClient;
import org.comdis.p2p.exceptions.AuthException;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Objects;

/**
 * Representación de un cliente para el servidor (clase privada para este paquete).
 * <br><br>
 * Es esencialmente un wrapper de un ClientCallback que incluye metadatos (nombre de usuario).
 * <br><br>
 * Esta clase existe principalmente para evitarse llamadas a `getUsername()`, que en
 * realidad tramitan la llamada a traves de la red, lo que puede ser especialmente
 * lento si se intenta usar en `contains` y similares. En su lugar, se almacena dicho
 * String en la memoria de esta máquina.
 * <br><br>
 * Implementa la interfaz para que de un error de compilación cuando falte algún metodo.
 *
 * @see RemoteClient
 * Versión analoga para que los clientes solo puedan enviarse mensajes
 */
class Client implements ClientCallback {

    // El servidor necesita tener un manejador para enviarle a otros clientes
    // Se usa también para almacenar el nombre de usuario correspondiente
    private final RemoteClient handle;
    // Referencia a donde enviar las notificaciones
    private final ClientCallback callbacks;

    public Client(String username, ClientCallback callbacks, ClientPtp handle) {
        this.callbacks = callbacks;
        this.handle = new RemoteClient(username, handle);
    }

    public String getUsername() {
        return handle.getUsername();
    }

    public RemoteClient getHandle() {
        return handle;
    }

    // ==== IMPLEMENTAR LA INTERFAZ ====================================================================================

    @Override
    public void friendsOnline(Collection<RemoteClient> friendsOnline) throws RemoteException {
        callbacks.friendsOnline(friendsOnline);
    }

    @Override
    public void friendConnected(RemoteClient friend) throws RemoteException {
        callbacks.friendConnected(friend);
    }

    @Override
    public void friendDisconnected(RemoteClient friend) throws RemoteException {
        callbacks.friendDisconnected(friend);
    }

    @Override
    public void friendRequests(Collection<String> requests) throws RemoteException {
        callbacks.friendRequests(requests);
    }

    @Override
    public void newFriendRequest(String username) throws RemoteException {
        callbacks.newFriendRequest(username);
    }

    @Override
    public void changePassword(String username, String oldpassword, String newpasword) throws AuthException , RemoteException{
        callbacks.changePassword(username, oldpassword, newpasword);
    }

    // ==== COMODIDADES ================================================================================================

    @Override
    public String toString() {
        return handle.getUsername();
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

        Client other = (Client) obj;
        return other.handle.getUsername().equals(this.handle.getUsername());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(handle.getUsername());
    }

}
