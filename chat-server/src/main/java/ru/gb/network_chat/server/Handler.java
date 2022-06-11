package ru.gb.network_chat.server;

import ru.gb.network_chat.enums.Command;
import ru.gb.network_chat.server.error.WrongCredentialsException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.spec.RSAOtherPrimeInfo;
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

    public void start() {
        handlerThread = new Thread (() -> {
            authorize();
            while (!handlerThread.isInterrupted() && !socket.isClosed()) {
                try {
                    String income = in.readUTF();
                    parseMessage(income);
                } catch (IOException e) {
                    System.out.printf("Connection with %s was terminated.\n", nickname);
                    server.removeHandler(this);
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
            default -> System.out.println("Unknown command code - " + split[0]);
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
                        sendMessage(AUTH_OK.getCode() + REGEX + nickname);
                        server.addHandler(this);
                        System.out.printf("Authorization with %s complete.\n", nickname);
                        break;
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
