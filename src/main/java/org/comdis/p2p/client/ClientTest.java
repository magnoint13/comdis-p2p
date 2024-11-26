package org.comdis.p2p.client;

import org.comdis.p2p.exceptions.AlreadyExistsException;
import org.comdis.p2p.exceptions.AuthException;
import org.comdis.p2p.exceptions.NotFoundException;
import org.comdis.p2p.exceptions.PtpException;

import java.rmi.RemoteException;

class ClientTest {
    // ==== ATRIBUTOS Y CONSTRUCTOR ====================================================================================

    public static void main(String[] args) throws RemoteException {

        System.out.print("Nombre: ");
        String username = System.console().readLine().trim();
        System.out.print("Contraseña: ");
        String password = System.console().readLine().trim();

        ClientCallbackImpl client = ClientCallbackImpl.getInstance();

        System.out.print("Crear nuevo? (s/N): ");
        String crear = System.console().readLine().trim();
        if (crear.charAt(0) == 's' || crear.charAt(0) == 'S') {
            try {
                client.createUserAndConnect(username, password);
                System.out.println("creado");
            } catch (AlreadyExistsException error) {
                System.out.println(error.getMessage());
                System.exit(1);
            }
        } else {
            try {
                client.connect(username, password);
            } catch (AuthException error) {
                System.out.println(error.getMessage());
                System.exit(1);
            }
        }

        System.out.print("Solicitud de amistad ? (s/N): ");
        String enviarSol = System.console().readLine().trim();
        if (enviarSol.charAt(0) == 's' || enviarSol.charAt(0) == 'S') {
            try {
                System.out.print("Nombre de usuario: ");
                String petUser = System.console().readLine().trim();
                client.sendFriendRequest(petUser);
            } catch (PtpException e) {
                System.out.println(e.getMessage());
            }
        }

        System.out.print("Mensaje a: ");
        String friendUsername = System.console().readLine().trim();
        System.out.print("Mensaje: ");
        String msg = System.console().readLine().trim();
        try {
            client.sendMessage(friendUsername, msg);
        } catch (NotFoundException error) {
            System.out.println(error.getMessage());
        }
    }
}
