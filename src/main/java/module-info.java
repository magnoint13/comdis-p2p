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
    requires java.desktop;

    opens org.comdis.p2p to javafx.fxml;
    exports org.comdis.p2p;
    exports org.comdis.p2p.server;
    opens org.comdis.p2p.server to javafx.fxml;
    exports org.comdis.p2p.client;
    opens org.comdis.p2p.client to javafx.fxml;
    exports org.comdis.p2p.exceptions;
    opens org.comdis.p2p.exceptions to javafx.fxml;
}