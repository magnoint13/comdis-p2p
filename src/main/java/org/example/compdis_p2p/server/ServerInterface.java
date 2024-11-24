package org.example.compdis_p2p.server;

import org.example.compdis_p2p.AlreadyExistsException;
import org.example.compdis_p2p.AuthException;
import org.example.compdis_p2p.client.ClientInterface;
import org.example.compdis_p2p.client.ClientPtp;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

// TODO: enviar solicitud de amistad
// TODO: aceptar solicitud
// TODO: rechazar solicitud
public interface ServerInterface extends Remote {

    /**
     * Inscribir un nuevo usuario en la BD
     */
    void addUser(ClientInterface client) throws RemoteException, AlreadyExistsException;

    /**
     * Conectar el cliente. Comprobará sus credenciales.
     */
    void connect(ClientInterface client) throws AuthException, RemoteException;

    /**
     * Desconectar el cliente
     */
    void disconnect(ClientInterface client) throws RemoteException;

    // TODO: los siguientes metodos pueden ser peligrosos
    // Obtener referencia y luego conectarme como él, sin necesidad de conocer la contraseña
    ClientPtp getClient(String userName) throws RemoteException;

    /**
     * Buscar clientes por coincidencias de nombre, para agregar amigos
     */
    Collection<ClientPtp> searchClientsByName(String userName) throws RemoteException;

    void sendFriendRequest(ClientInterface client, String userName) throws RemoteException;

    /**
     * Inserta una amistad en la base de datos, cambia la solicitud existente a aceptada
     */
    void createFriendship(ClientInterface client, String selectedItem) throws RemoteException;

    /**
     * Comprueba si ya existe una solicitud pendiente
     */
    boolean alreadyFriendRequest(ClientInterface client, String other) throws RemoteException;

    /**
     * Comprueba si tiene una solicitud pendiente del otro usuario
     */
    boolean pendingRequestExisting(ClientInterface client, String other) throws RemoteException;
}
