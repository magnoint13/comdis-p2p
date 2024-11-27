package org.comdis.p2p;

import org.comdis.p2p.exceptions.AlreadyExistsException;
import org.comdis.p2p.exceptions.AuthException;
import org.comdis.p2p.exceptions.NotFoundException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

public interface ServerInterface extends Remote {

    // ==== CREAR/BORRAR USUARIOS ======================================================================================

    /**
     * Inscribir un nuevo usuario en la BD.
     */
    void createUser(String username, String password)
            throws AlreadyExistsException, RemoteException;

    /**
     * Borra al usuario dado.
     * Como es una operación que requiere más seguridad se pide la contraseña.
     *
     * @throws AuthException Si el usuario o la contraseña no son válidos.
     */
    void deleteUser(String username, String password)
            throws AuthException, RemoteException;

    // ==== CONECTARSE/DESCONECTARSE ===================================================================================

    /**
     * Conectar el cliente, activando las callbacks dadas.
     *
     * @throws AuthException Si el usuario o la contraseña no son válidos.
     */
    RemoteClient connect(String username, String password, ClientCallback callbacks, ClientPtp msgCallback)
            throws AuthException, RemoteException;

    /**
     * Desconectar el cliente.
     * Si no se encuentra al usuario o no está online, se ignora.
     */
    void disconnect(RemoteClient client) throws RemoteException;

    // ==== PETICIONES DE AMISTAD ======================================================================================

    /**
     * `from` envia una petición de amistad a `to`.
     *
     * @throws NotFoundException      Si el nombre de usuario `to` no existe.
     * @throws AlreadyExistsException Si `from` y `to` ya son amigos o si la solitud ya existe.
     */
    void sendFriendRequest(RemoteClient from, String to)
            throws NotFoundException, AlreadyExistsException, RemoteException;

    /**
     * `accepts` acepta la peticion enviada por `originalSender`.
     *
     * @throws NotFoundException      Si no existe una peticion pendiente entre `client` y `other`.
     * @throws AlreadyExistsException Si la amistad ya existia de antes (no deberia suceder)
     */
    void acceptFriendRequest(RemoteClient accepts, String originalSender)
            throws NotFoundException, AlreadyExistsException, RemoteException;

    /**
     * Si lo llama el emisor de la peticion, esta se cancela.
     * Si lo llama el receptor de la peticion, esta se rechaza.
     *
     * @throws NotFoundException Si no existe una peticion pendiente entre `client` y `other`.
     */
    void cancelFriendRequest(RemoteClient client, String other)
            throws NotFoundException, RemoteException;

    // NOTA: creo que no hace falta, dado que el cliente se entera a traves de las notificaciones
    /**
     * Obtiene el estado de una peticion de amistad.
     * @throws NotFoundException Si la peticion no existe
     */
    /*FriendStatus getFriendshipStatus(RemoteClient client, String other)
            throws NotFoundException, RemoteException;*/

    // TODO: eliminar amigo (recordar notificar a cada uno que esta offline)

    // ==== BUSCAR USUARIOS ============================================================================================

    /**
     * Buscar clientes por coincidencias de nombre, para agregar amigos.
     * `searcher` es la referencia del cliente que realiza la búsqueda.
     */
    Collection<String> searchUsernames(RemoteClient searcher, String username) throws RemoteException;

    // TODO: posibilidad para verificar la identidad de un usuario:
    //       El server comprueba si esta online y si contraseña es correcta
    //       Supuestamente, equals de dos objetos remotos tambien funcionaria
}
