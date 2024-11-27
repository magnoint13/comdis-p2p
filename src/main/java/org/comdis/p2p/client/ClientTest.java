package org.comdis.p2p.client;

import org.comdis.p2p.RemoteClient;
import org.comdis.p2p.exceptions.AlreadyExistsException;
import org.comdis.p2p.exceptions.AuthException;
import org.comdis.p2p.exceptions.NotFoundException;
import java.rmi.RemoteException;

class ClientTest {
    public static void main(String[] args) throws RemoteException {
        boolean running = true;
        ClientCallbackImpl client = ClientCallbackImpl.getInstance();

        while (running) {
            switch (menu()) {
                // Conectar
                case 1 -> {
                    try {
                        String username = askString("Usuario: ");
                        String password = askString("Contraseña: ");
                        client.connect(username, password);
                    } catch (AuthException e) {
                        System.out.println("ERROR: " + e.getMessage());
                    }
                }

                // Desconectar
                case 2 -> client.disconnect();

                // Crear usuario
                case 3 -> {
                    try {
                        String username = askString("Usuario: ");
                        String password = askString("Contraseña: ");
                        client.createUserAndConnect(username, password);
                    } catch (AlreadyExistsException e) {
                        System.out.println("ERROR: " + e.getMessage());
                    }
                }

                // Borrar usuario
                case 4 -> {
                    try {
                        String password = askString("Contraseña: ");
                        String username = client.getUsername(); //Luego handle se hace null y peta por el disconnect
                        client.disconnect();
                        client.deleteUser(username,password);
                    } catch (AuthException e) {
                        System.out.println("ERROR: " + e.getMessage());
                    }
                }

                // Ver amigos online
                case 5 -> {
                    int i = 1;
                    for (RemoteClient username : client.getFriendsOnline()) {
                        System.out.printf("\t%d) %s\n", i, username.getUsername());
                        i += 1;
                    }
                }

                // Buscar usuarios
                case 6 -> {
                    String query = askString("Buscar: ");
                    int i = 1;
                    for (String username : client.searchUsernames(query)) {
                        System.out.printf("\t%d) %s\n", i, username);
                        i += 1;
                    }
                }

                // Enviar mensaje
                case 7 -> {
                    try {
                        String username = askString("Username: ");
                        String msg = askString("Mensaje: ");
                        client.sendMessage(username, msg);
                    } catch (NotFoundException e) {
                        System.out.println("ERROR: " + e.getMessage());
                    }
                }

                // Ver peticiones de amistad
                case 8 -> {
                    int i = 1;
                    for (String peticion : client.getPendingRequests()) {
                        System.out.printf("\t%d) %s\n", i, peticion);
                        i += 1;
                    }
                }

                // Enviar peticion de amistad
                case 9 -> {
                    try {
                        String username = askString("Usuario: ");
                        client.sendFriendRequest(username);
                    } catch (AlreadyExistsException | NotFoundException e) {
                        System.out.println("ERROR: " + e.getMessage());
                    }
                }

                // Aceptar peticion de amistad
                case 10 -> {
                    try {
                        String username = askString("Usuario: ");
                        client.acceptFriendRequest(username);
                    } catch (AlreadyExistsException | NotFoundException e) {
                        System.out.println("ERROR: " + e.getMessage());
                    }
                }

                // Cancelar peticion de amistad
                case 11 -> {
                    try {
                        String username = askString("Usuario: ");
                        client.cancelFriendRequest(username);
                    } catch (NotFoundException e) {
                        System.out.println("ERROR: " + e.getMessage());
                    }
                }

                case 99 -> running = false;
                default -> System.out.println("Numero invalido");
            }
        }

        // Asegurarse de desconectar anes de salir
        client.close();
    }

    private static int menu() {
        while (true) {
            try {
                System.out.println("""
                        ==== PROGRAMA DE PRUEBA DE CLIENTE P2P ====
                          1. Conectar
                          2. Desconectar
                          3. Crear usuario
                          4. Borrar usuario
                          5. Ver amigos online
                          6. Buscar usuarios
                          7. Enviar mensaje
                          8. Ver peticiones de amistad
                          9. Enviar peticion de amistad
                         10. Aceptar peticion de amistad
                         11. Cancelar peticion de amistad
                         99. Salir
                        Seleccione uno:""");
                return Integer.parseInt(System.console().readLine().trim());
            } catch (NumberFormatException _) {
                System.out.println("Numero invalido");
            }
        }
    }

    private static String askString(String prompt) {
        System.out.println(prompt);
        return System.console().readLine().trim();
    }
}

/*
 * TODO: Problemas encontrados:
 * - Conectarse como usuario A, crear usuario B, salir. A nunca se desconecta.
 * - Por algun motivo, el nombre del remitente cuando ClientCallbackImpl recibe un mensaje no es correcto
 *
 *
 */