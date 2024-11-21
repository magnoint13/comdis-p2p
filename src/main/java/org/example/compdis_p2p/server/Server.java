package org.example.compdis_p2p.server;
import org.example.compdis_p2p.AuthException;
import org.example.compdis_p2p.client.ClientInterface;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.Vector;

/**
 * Clase Servidor
 */
public class Server extends UnicastRemoteObject implements ServerInterface {

    // TODO: es esta la mejor estructura de datos para esto?
    private final Vector<ClientInterface> connectedClients;

    private final DataBaseController dataBaseController;

    public Server(DataBaseController dataBaseController) throws RemoteException {
        super();
        this.dataBaseController = dataBaseController;
        connectedClients = new Vector<>();
    }

    @Override
    public Collection<ClientInterface> getConnectedClients() throws RemoteException {
        return connectedClients;
    }

    @Override
    public void connect(ClientInterface client) throws AuthException, RemoteException {
        if (connectedClients.contains(client)) {
            return;
        }

        // TODO: chequear credenciales
        dataBaseController.checkUser(client);

        for (ClientInterface c : connectedClients) {
            // Notificar al resto de clientes de que alguien se ha conectado
            c.receiveConnectNotification(client);

            // Y notificar al nuevo que quienes están conectados
            client.receiveConnectNotification(c);
        }

        // Registrar al cliente como online
        // Debe ser lo último para que no se envien notificaciones repetidas
        connectedClients.add(client);
    }

    @Override
    public void disconnect(ClientInterface client) throws RemoteException {
        // Si no estaba conectado, no hacer nada
        if (!connectedClients.contains(client)) {
            return;
        }

        //
        connectedClients.remove(client);
    }

    @Override
    public void registerClient(ClientInterface client) throws RemoteException {

    }
}
