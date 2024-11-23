package comdis.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import comdis.AlreadyExistsException;
import comdis.AuthException;
import comdis.server.MainServer;
import comdis.server.ServerInterface;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class ClientApp extends Application {
    public static void main(String[] args) throws MalformedURLException, NotBoundException, RemoteException {
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
        }

        System.out.print("Mensaje a: ");
        String friend = System.console().readLine().trim();
        System.out.print("Mensaje: ");
        String msg = System.console().readLine().trim();

        ClientInterface other = server.getClient(friend);
        if (other != null) {
            client.message(other, msg);
            System.out.println("enviado");
        } else {
            System.out.println("ese usuario no esta online");
        }

        server.disconnect(client);
        System.out.println("desconectado");

        //launch();
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(ClientApp.class.getResource("Inicio.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Programa de comunicación P2P!");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }
}