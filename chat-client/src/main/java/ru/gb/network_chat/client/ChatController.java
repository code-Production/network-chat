package ru.gb.network_chat.client;

import javafx.application.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import javafx.scene.*;
import ru.gb.network_chat.client.net.MessageProcessor;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import ru.gb.network_chat.client.net.NetworkService;
import ru.gb.network_chat.enums.Command;

import static ru.gb.network_chat.constants.MessageConstants.REGEX;
import static ru.gb.network_chat.enums.Command.*;

public class ChatController implements Initializable, MessageProcessor {

    @FXML
    public VBox mainPanel;
    @FXML
    public TextField loginField;
    @FXML
    public TextField passwordField;
    @FXML
    public Button btnSignIn;
    @FXML
    public VBox loginPanel;
    @FXML
    private TextArea chatArea;
    @FXML
    private ListView<String> contactList;
    @FXML
    private TextField inputField;
    @FXML
    private Button btnSend;

    private MultipleSelectionModel<String> selectionModel;
    private NetworkService networkService;
    private String nickname;

    private void parseMessage(String message) {
        String[] split = message.split(REGEX);
        Command command = Command.getByCode(split[0]);
        switch(command) {
            case AUTH_OK -> auth_ok(split);
            case LIST_USERS -> parseUsers(split);
            case ERROR_MESSAGE -> showError(split[1]);
            default -> chatArea.appendText(split[1] + System.lineSeparator());
        }
    }

    private void parseUsers(String[] split) {
        List<String> users_online = new ArrayList<>(List.of(split));
        users_online.remove(0);
        contactList.setItems(FXCollections.observableList(users_online));
    }

    public void showError(String message) {
        Platform.runLater(() -> {
            new Alert(
                    Alert.AlertType.ERROR,
                    message,
                    ButtonType.CLOSE)
                    .showAndWait();
        });
    }
    
    public void showLoginPanel() {
        loginPanel.setVisible(true);
        mainPanel.setVisible(false);
    }

    private void auth_ok(String[] split) {
        this.nickname = split[1];
        System.out.println("Successfull authorization for user: " + split[1]);
        loginPanel.setVisible(false);
        mainPanel.setVisible(true);
    }

    public void closeApplication(ActionEvent actionEvent) {
        Platform.runLater(() -> networkService.shutdown());
        Platform.exit();
        System.out.println("Application closed.");
    }

    public void sendAuth(ActionEvent actionEvent) {
        String login = loginField.getText();
        String password = passwordField.getText();
        if (login.isEmpty() || password.isEmpty()) return;
        String message = AUTH_MESSAGE.getCode() + REGEX + login + REGEX + password;

        try {
            if (!networkService.isConnected()) {
                networkService.connect();
            }
            networkService.sendMessage(message);
            loginField.clear();
            passwordField.clear();
        } catch (IOException e) {
//                e.printStackTrace();
            showError("Connection problem.");
        }


    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        networkService = new NetworkService(this, this);
        selectionModel = contactList.getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.MULTIPLE);

    }

    public void sendMessage(ActionEvent actionEvent) {
        String text = inputField.getText();
        if (text.isEmpty()) {return;}

        ObservableList<String> selectedContacts = selectionModel.getSelectedItems();

        if (selectedContacts.size() == 0 ||
                selectedContacts.size() == contactList.getItems().size()) {
            networkService.sendMessage(BROADCAST_MESSAGE.getCode() + REGEX + text);
        } else {
            chatArea.appendText(String.format("[%s]: %s", this.nickname, text) + System.lineSeparator());
            selectedContacts.forEach((s) -> networkService.sendMessage(PRIVATE_MESSAGE.getCode() + REGEX + s + REGEX + text));
        }

        inputField.clear();
    }

    public void helpPage(ActionEvent actionEvent) {
//        new App.getHostServices().showDocument("http://github.com/code-Production/github/blob/feature/git.md");
    }

    public void aboutPage(ActionEvent actionEvent) {
        Stage helpStage = new Stage();
        helpStage.setTitle("About...");
        GridPane gridPane = new GridPane();
        Scene helpScene = new Scene(gridPane, 600, 400);
        Label helpLabel = new Label("...Example JavaFX application about...");
        helpStage.setScene(helpScene);
        gridPane.setAlignment(Pos.CENTER);
        gridPane.add(helpLabel,1,1);
        helpStage.show();
    }

    @Override
    public void processMessage(String message) {
        Platform.runLater(() -> parseMessage(message));
    }

    public void mockAction(ActionEvent actionEvent) {
        System.out.println("mock");
    }


}



