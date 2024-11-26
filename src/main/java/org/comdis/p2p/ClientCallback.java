package org.comdis.p2p;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

/**
 * Interfaz que define las notificaciones que envia el servido a un cliente.
 */
public interface ClientCallback extends Remote {
    // TODO: metodo ping que permita al servidor determinar si todavia esta online
    //       puede que alguno de los clientes termine con errores y no realicen el disconnect

    // ==== NOTIFICACIONES DE AMIGOS ONLINE ============================================================================

    /**
     * Cuando `this` se conecta, recibe quienes de sus amigos ya estaban conectados.
     */
    void friendsOnline(Collection<RemoteClient> friendsOnline) throws RemoteException;

    /**
     * Se notifica a `this` que `friend` se acaba de conectar.
     */
    void friendConnected(RemoteClient friend) throws RemoteException;

    /**
     * Se notifica a `this` que `friend` se acaba de desconectar
     */
    void friendDisconnected(RemoteClient friend) throws RemoteException;

    // ==== NOTIFICACIONES DE AMISTAD ==================================================================================

    /**
     * Peticiones de amistad pendientes de revisar.
     * Se reciben cuando `this` se conecta.
     */
    void friendRequests(Collection<String> requests) throws RemoteException;

    /**
     * Se notifica a `this` que `username` le acaba de enviar una solicitud de amistad.
     * Solo se dan mientras `this` está conectado.
     */
    void newFriendRequest(String username) throws RemoteException;

    // TODO: notificaciones de que una solicitud enviada se ha aceptado o rechazado
}
