package ru.gb.network_chat.client;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private ChatController chatController;

    public static void run(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Network chat client");
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(this.getClass().getResource("/ChatWindow.fxml"));
        Parent parent = loader.load();
        chatController = loader.getController();
        chatController.setStage(primaryStage);
        Scene primaryScene = new Scene(parent);
        primaryStage.setScene(primaryScene);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        chatController.closeApplication(new ActionEvent());
    }

}
