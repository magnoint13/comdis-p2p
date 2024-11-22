module org.example.compdis_p2p {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.rmi;
    requires com.fasterxml.jackson.databind;
    requires org.xerial.sqlitejdbc;

    opens org.example.compdis_p2p to javafx.fxml;
    exports org.example.compdis_p2p;
    exports org.example.compdis_p2p.server;
    opens org.example.compdis_p2p.server to javafx.fxml;
    exports org.example.compdis_p2p.client;
    opens org.example.compdis_p2p.client to javafx.fxml;
}