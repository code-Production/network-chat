package ru.gb.network_chat.server;

import ru.gb.network_chat.enums.Command;
import ru.gb.network_chat.server.error.UserAlreadyExistsException;
import ru.gb.network_chat.server.error.WrongCredentialsException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

import static ru.gb.network_chat.constants.MessageConstants.REGEX;
import static ru.gb.network_chat.enums.Command.*;

public class Handler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Thread handlerThread;
    private String nickname;
    private String login;
    private boolean isAuthorized = false;

    public Handler(Socket socket, Server server) {
        try {
            this.socket = socket;
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.out.println("Connection problem with user " + nickname);
        }
    }

    private void timeoutShutdown() {
        Thread timeoutThread = new Thread(() -> {
            try {
                Thread.sleep(30000);
                if (!isAuthorized) {
                    shutdown();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        timeoutThread.setDaemon(true);
        timeoutThread.start();
    }

    public void start() {
        handlerThread = new Thread (() -> {
            authorize();
            while (!handlerThread.isInterrupted() && !socket.isClosed()) {
                try {
                    String income = in.readUTF();
                    parseMessage(income);
                } catch (IOException e) {
                    System.out.printf("Connection with %s was terminated.\n", nickname);
                    server.removeClientHandler(this);
                    break;
                }
            }
        });
        handlerThread.start();
    }

    private void parseMessage(String message) {
        String[] split = message.split(REGEX);
        Command command = Command.getByCode(split[0]);

        switch(command) {
            case BROADCAST_MESSAGE -> server.broadcast(this.nickname, split[1]);
            case PRIVATE_MESSAGE -> server.privateMessage(split[1], this.nickname, split[2]);
            case CHANGE_NICK_MESSAGE -> changeNickname(split);
            default -> System.out.println("Unknown command code - " + split[0]);
        }
    }

    private void changeNickname(String[] split) {
        if(!socket.isClosed()){
            String nick = null;
            String response = "";
            try {
                nick = server.getUserService().changeNickname(split[1], split[2], split[3]);
            } catch (WrongCredentialsException e) {
                response = ERROR_MESSAGE.getCode() + REGEX + e.getMessage();
                System.out.println("Wrong credentials for login - " + split[2] + ", with password - " + split[3] + ".");
            } catch (UserAlreadyExistsException e) {
                response = ERROR_MESSAGE.getCode() + REGEX + e.getMessage();
                System.out.println("User with nickname '" + split[1] + "' already exists.");
            }
            if (!Objects.equals(response, "")) {
                sendMessage(response);
            } else if (nick != null) {
                String message = "Changing nickname from " + nickname + " to " + nick + " is success.";
                nickname = nick;
                sendMessage(CHANGE_NICK_OK.getCode() + REGEX + nickname + REGEX + message);
                System.out.println(message);
                server.sendContacts();
            }
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void authorize() {
        timeoutShutdown();
        try {
            while (!socket.isClosed()) {
                String income = in.readUTF();
                String[] split = income.split(REGEX);
                if (Objects.equals(AUTH_MESSAGE.getCode(), split[0])) {
                    String nick = null;
                    String response = "";
                    try {
                        nick = server.getUserService().authenticate(split[1], split[2]);
                    } catch (WrongCredentialsException e) {
                        response = ERROR_MESSAGE.getCode() + REGEX + e.getMessage();
                        System.out.println("Wrong credentials for user - " + split[1] + ", with password - " + split[2] + ".");
                    }
                    if (nick != null && server.isUserAlreadyOnline(nick)) {
                        response = ERROR_MESSAGE.getCode() + REGEX + "User with this nick is already online";
                    }
                    if (!Objects.equals(response, "")) {
                        sendMessage(response);
                    } else {
                        this.nickname = nick;
                        this.login = split[1];
                        isAuthorized = true;
                        sendMessage(AUTH_OK.getCode() + REGEX + nickname + REGEX + login);
                        server.addClientHandler(this);
                        server.removeUnAuthHandler(this);
                        System.out.printf("Authorization with %s complete.\n", nickname);
                        break;
                    }
                } else if (Objects.equals(ADD_USER_MESSAGE.getCode(), split[0])) {
                    String nick = null;
                    String response = "";
                    try {
                        nick = server.getUserService().register(split[1], split[2], split[3]);
                    } catch (UserAlreadyExistsException e) {
                        response = ERROR_MESSAGE.getCode() + REGEX + e.getMessage();
                        System.out.println("User with login: " + split[2] + " or nickname: " + split[1] + "already exists.");
                    }
                    if (!Objects.equals(response, "")) {
                        sendMessage(response);
                    } else if (nick != null){
                        String message = "Registration for " + nick + " is complete.";
                        sendMessage(ADD_USER_OK.getCode() + REGEX + message);
                        System.out.println(message);
                    }
                }
            }
        } catch (IOException e) {
//            e.printStackTrace();
            if (socket != null && !socket.isClosed()){
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            System.out.println("Connection with new user was terminated.");
        }
    }

    public void shutdown() {
        if (handlerThread != null && !handlerThread.isInterrupted()) {
            handlerThread.interrupt();
        }
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getNickname() {
        return nickname;
    }
}
