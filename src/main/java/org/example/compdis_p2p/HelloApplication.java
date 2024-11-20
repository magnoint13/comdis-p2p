package org.example.compdis_p2p;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        BaseDatosController baseDatosController = new BaseDatosController();
        baseDatosController.conectarBD();
        baseDatosController.insertarDatos();
        baseDatosController.pruebaConsulta();
      /*  FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("Inicio.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Programa de comunicación P2P!");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
       */
    }

    public static void main(String[] args) {
        launch();
    }
}