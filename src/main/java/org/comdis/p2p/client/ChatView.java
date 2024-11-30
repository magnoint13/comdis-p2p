package org.comdis.p2p.client;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class ChatView extends ScrollPane {
    private final static int MAX_MESSAGES = 100;

    private final String username;
    private final ObservableList<Node> msgNodes;

    private final VBox content;

    public ChatView(String username, StackPane chatPane) {
        this.username = username;

        content = new VBox();
        content.setPrefWidth(chatPane.getWidth());
        chatPane.widthProperty().addListener((observable, oldValue, newValue) -> {
            content.setPrefWidth(newValue.doubleValue());  // Ajusta el ancho de content cuando cambie el ancho de chatPane
        });
        content.setSpacing(10);
        content.setAlignment(Pos.BOTTOM_CENTER);
        content.setStyle("-fx-background-color: white;");
        content.setPadding(new Insets(20, 10, 20, 10));
        msgNodes = content.getChildren();

        setContent(content);
    }

    public String getUsername() {
        return username;
    }


    public VBox getContentObject() {
        return content;
    }

    public void addReceivedMsg(String msg) {
        addMsg(msg, false);
    }

    public void addSentMsg(String msg) {
        addMsg(msg, true);
    }

    private void addMsg(String msg, boolean sent) {
        HBox msgBox = createText(msg, sent);
        Platform.runLater(() -> {
            msgNodes.add(msgBox);
            if (msgNodes.size() >= MAX_MESSAGES) {
                msgNodes.removeFirst();
            };
        });
    }

    private HBox createText(String text, boolean right) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMinWidth(200);
        label.setPrefWidth(300);
        label.setMaxWidth(300);
        label.setPadding(new Insets(5));
        label.setStyle("-fx-background-radius: 10px;-fx-background-color: " + (right ? "turquoise;" : "peru;"));

        HBox pane = new HBox(label);
        pane.setAlignment(right ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        return pane;
    }

    public boolean fromUser(String username) {
        return this.username.equals(username);
    }
}
