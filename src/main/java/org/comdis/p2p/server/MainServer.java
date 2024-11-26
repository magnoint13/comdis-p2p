package org.comdis.p2p.server;

import org.comdis.p2p.exceptions.PtpException;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MainServer {
    private static final int REGISTRY_PORT = 1099;
    public static final String REGISTRY_URL = "rmi://localhost:" + REGISTRY_PORT + "/server";
    private static final String DATABASE_FILE = "P2P.db";
    private static final String DATABASE_SCRIPT = "BD.sql";

    public static void main(String[] args) {
        try {
            startRegistry(REGISTRY_PORT);
            Server server = new Server(DATABASE_FILE, DATABASE_SCRIPT);
            Naming.rebind(REGISTRY_URL, server);
        } catch (Exception error) {
            // En caso de error, simplemente mostrarlo por pantalla
            PtpException.logError(error);

            // Y salir con código no exitoso
            System.exit(1);
        }
    }

    private static void startRegistry(int port) throws RemoteException {
        try {
            // Tratamos de obtener el registro y listar su contenido.
            // Si no está creado, esto lanza una excepción.
            Registry registry = LocateRegistry.getRegistry(port);
            registry.list();
        } catch (RemoteException _) {
            // Crear el registro en caso de que no exista.
            LocateRegistry.createRegistry(port);
            System.out.println("Registro de Java RMI creado");
        }
    }
}
