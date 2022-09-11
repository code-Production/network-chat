package ru.gb.network_chat.client;

import javafx.application.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import javafx.scene.*;
import ru.gb.network_chat.client.net.MessageProcessor;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
    public TextField loginField_log;
    @FXML
    public TextField passwordField_log;
    @FXML
    public Button btnSignIn;
    @FXML
    public VBox loginPanel;
    @FXML
    public VBox changeNickPanel;
    @FXML
    public TextField nicknameField_chg;
    @FXML
    public TextField passwordField_chg;
    @FXML
    public Button btnChangeNick;
    @FXML
    public Button btnMainPanel;
    @FXML
    public Button btnSignUp;
    @FXML
    public Button btnSend;
    @FXML
    private TextArea chatArea;
    @FXML
    private ListView<String> contactList;
    @FXML
    private TextField inputField;
    @FXML
    public VBox regPanel;
    @FXML
    public TextField loginField_reg;
    @FXML
    public TextField passwordField_reg;
    @FXML
    public TextField nicknameField_reg;
    @FXML
    public Button btnShowRegPanel;
    @FXML
    public Button btnShowLoginPanel;


    private MultipleSelectionModel<String> selectionModel;
    private NetworkService networkService;
    private String nickname;
    private String login;
    private Stage stage;
    private ChatHistoryManager historyManager;


    private void parseMessage(String input) {
        String[] split = input.split(REGEX);
        Command command = Command.getByCode(split[0]);
        switch(command) {
            case AUTH_OK -> {
                auth_ok(split);
                updateStageTitle();
                chatArea.appendText(historyManager.loadChatHistory(1000));
            }
            case LIST_USERS -> parseUsers(split);
            case ERROR_MESSAGE -> showError(split[1]);
            case ADD_USER_OK -> reg_ok(split[1]);
            case CHANGE_NICK_OK -> {
                change_nick_ok(split);
                updateStageTitle();
            }
            default -> {
                String message = split[1] + System.lineSeparator();
                chatArea.appendText(message);
                historyManager.saveChatHistory(message);
            }
        }
    }

    private void updateStageTitle() {
        stage.setTitle(String.format("Network chat client [%s]", nickname));
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void change_nick_ok(String[] split) {
        changeNickPanel.setVisible(false);
        mainPanel.setVisible(true);
        nickname = split[1];
        Platform.runLater(() -> {
            new Alert(
                    Alert.AlertType.CONFIRMATION,
                    split[2],
                    ButtonType.CLOSE)
                    .showAndWait();
        });
    }

    private void reg_ok(String message) {
        showLoginPanel();
        Platform.runLater(() -> {
            new Alert(
                    Alert.AlertType.CONFIRMATION,
                    message,
                    ButtonType.CLOSE)
                    .showAndWait();
        });
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
        regPanel.setVisible(false);
        mainPanel.setVisible(false);
    }

    private void auth_ok(String[] split) {
        this.nickname = split[1];
        this.login = split[2];
        System.out.println("Successful authorization for user: " + split[1]);
        loginPanel.setVisible(false);
        mainPanel.setVisible(true);
        historyManager = new ChatHistoryManager("History", login);
    }

    public void closeApplication(ActionEvent actionEvent) {
        Platform.runLater(() -> historyManager.shutdown());
        Platform.runLater(() -> networkService.shutdown());
        Platform.exit();
        System.out.println("Application closed.");
    }

    public void sendAuth(ActionEvent actionEvent) {
        String login = loginField_log.getText();
        String password = passwordField_log.getText();
        if (login.isEmpty() || password.isEmpty()) return;
        String message = AUTH_MESSAGE.getCode() + REGEX + login + REGEX + password;

        try {
            if (!networkService.isConnected()) {
                networkService.connect();
            }
            networkService.sendMessage(message);
            loginField_log.clear();
            passwordField_log.clear();
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
        chatArea.setWrapText(true);
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


    public void sendReg(ActionEvent actionEvent) {
        String nickname = nicknameField_reg.getText();
        String login = loginField_reg.getText();
        String password = passwordField_reg.getText();
        if (login.isEmpty() || password.isEmpty() || nickname.isEmpty()) return;
        String message = ADD_USER_MESSAGE.getCode() + REGEX + nickname + REGEX + login + REGEX + password;
        try {
            if (!networkService.isConnected()) {
                networkService.connect();
            }
            networkService.sendMessage(message);
            loginField_reg.clear();
            passwordField_reg.clear();
            nicknameField_reg.clear();
        } catch (IOException e) {
//            e.printStackTrace();
            showError("Connection problem.");
        }
    }

    public void showRegPanel(ActionEvent actionEvent) {
        loginPanel.setVisible(false);
        regPanel.setVisible(true);
    }


    public void sendChangeNick(ActionEvent actionEvent) {
        String newNickname = nicknameField_chg.getText();
        String password = passwordField_chg.getText();
        if (login.isEmpty() || password.isEmpty() || newNickname.isEmpty()) return;
        String message = CHANGE_NICK_MESSAGE.getCode() + REGEX + newNickname + REGEX + login + REGEX + password;
        try {
            if (!networkService.isConnected()) {
                networkService.connect();
            }
            networkService.sendMessage(message);
            passwordField_chg.clear();
            nicknameField_chg.clear();
        } catch (IOException e) {
//            e.printStackTrace();
            showError("Connection problem.");
        }
    }

    public void showMainPanel(ActionEvent actionEvent) {
        changeNickPanel.setVisible(false);
        mainPanel.setVisible(true);
    }

    public void showChangeNickPanel(ActionEvent actionEvent) {
        changeNickPanel.setVisible(true);
        mainPanel.setVisible(false);
        regPanel.setVisible(false);
        loginPanel.setVisible(false);
    }
}



