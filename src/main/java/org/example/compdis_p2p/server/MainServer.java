package org.example.compdis_p2p.server;

import java.rmi.Naming;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;

public class MainServer {

    public static void main(String[] args){
        try {
            startRegistry(1099);
            Server server = new Server();
            String registryURL = "rmi://localhost:1099/server";
            Naming.rebind(registryURL,server);
            System.out.println("Servidor registrado");
        }catch (Exception error){
            // En caso de error, simplemente mostrarlo por pantalla
            System.err.printf("[ERROR] %s: %s\n", error.getClass().getName(), error.getMessage());
            error.printStackTrace();
            // Y salir con código no exitoso
            System.exit(1);
        }
    }

    private static void startRegistry(int puerto) throws RemoteException{
        try {
            // Obtiene el registro RMI en el puerto indicado
            Registry registro = LocateRegistry.getRegistry(puerto);
            // Devuelve los objetos registrados en ese puerto
            registro.list();
        } catch (RemoteException e) { // Si no hay un registro en el puerto se lanza una excepción
            System.out.println("No se ha podido encontrar un registro RMI en el puerto " + puerto+"\n");
            // Entonces crea un registro RMI en el puerto
            Registry registry = LocateRegistry.createRegistry(puerto);
            System.out.println("Servidor iniciado en el puerto " + puerto+"\n");
        }
    }



}
