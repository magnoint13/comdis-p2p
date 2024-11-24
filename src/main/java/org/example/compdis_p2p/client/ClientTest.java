package org.example.compdis_p2p.client;

import org.example.compdis_p2p.AlreadyExistsException;
import org.example.compdis_p2p.AuthException;
import org.example.compdis_p2p.server.MainServer;
import org.example.compdis_p2p.server.ServerInterface;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class ClientTest {
    public static void main(String[] args) throws RemoteException, MalformedURLException, NotBoundException {
        ServerInterface server = (ServerInterface) Naming.lookup(MainServer.REGISTRY_URL);

        System.out.print("Nombre: ");
        String name = System.console().readLine().trim();
        System.out.print("Contraseña: ");
        String password = System.console().readLine().trim();

        Client client = new Client(name, password);

        System.out.println("Crear nuevo? (s/N): ");
        String crear = System.console().readLine().trim();
        if (crear.charAt(0) == 's' || crear.charAt(0) == 'S') {
            try {
                server.addUser(client);
                System.out.println("creado");
            } catch (AlreadyExistsException error) {
                System.out.println(error.getMessage());
            }
        }

        try {
            server.connect(client);
            System.out.println("conectado");
        } catch (AuthException error) {
            System.out.println(error.getMessage());
            System.exit(1);
        }

        System.out.print("Mensaje a: ");
        String friend = System.console().readLine().trim();
        System.out.print("Mensaje: ");
        String msg = System.console().readLine().trim();

        ClientInterface other = (ClientInterface) server.getClient(friend); //HICE los casts asi cutre pa que no de error (solo estos)
        if (other != null) {
            client.message((ClientPtp) other, msg);
            System.out.println("enviado");
        } else {
            System.out.println("ese usuario no esta online");
        }

        server.disconnect(client);
        System.out.println("desconectado");
    }
}
