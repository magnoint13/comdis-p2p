package org.comdis.p2p.client;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

public class ChatView extends ScrollPane {
    private final static int MAX_MESSAGES = 100;

    private final String username;
    private final ObservableList<Node> msgNodes;

    public ChatView(String username) {
        this.username = username;

        // Ocupar el espacio disponible
        this.setFitToHeight(true);
        this.setFitToWidth(true);

        VBox content = new VBox();
        content.setSpacing(10);
        content.setAlignment(Pos.BOTTOM_CENTER);
        content.setStyle("-fx-background-color:white;");
        content.setPadding(new Insets(20, 10, 20, 10));
        msgNodes = content.getChildren();
        this.setContent(content);
    }

    private VBox createText(String text, boolean right) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMinWidth(200);
        label.setPrefWidth(300);
        label.setMaxWidth(300);
        label.setPadding(new Insets(5));
        label.setStyle("-fx-background-radius: 10px;-fx-background-color: " + (right ? "turquoise;" : "peru;"));

        VBox pane = new VBox(label);
        pane.setAlignment(right ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        return pane;
    }

    public String getUsername() {
        return username;
    }

    public boolean fromUser(String username) {
        return this.username.equals(username);
    }

    public void addReceivedMsg(String msg) {
        addMsg(msg, false);
    }

    public void addSentMsg(String msg) {
        addMsg(msg, true);
    }

    private void addMsg(String msg, boolean sent) {
        Platform.runLater(() -> {
            VBox msgBox = createText(msg, sent);
            msgNodes.add(msgBox);

            if (msgNodes.size() >= MAX_MESSAGES) {
                msgNodes.removeFirst();
            }

            // Mantener los ultimos mensajes visibles
            // Es decir, scroll al fondo
            setVvalue(1.2);
        });
    }
}
