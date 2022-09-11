package ru.gb.network_chat.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.gb.network_chat.enums.Command;
import ru.gb.network_chat.server.error.UserAlreadyExistsException;
import ru.gb.network_chat.server.error.WrongCredentialsException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import static ru.gb.network_chat.constants.MessageConstants.REGEX;
import static ru.gb.network_chat.enums.Command.*;

public class Handler {

    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nickname;
    private String login;
    private boolean isAuthorized = false;
    private ExecutorService executorService;
    private ExecutorService daemonExecutorService;
    private static final Logger log = LogManager.getLogger(Handler.class.getName());


    public Handler(Socket socket,
                   Server server,
                   ExecutorService executorService,
                   ExecutorService daemonExecutorService) {
        try {
            this.socket = socket;
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.executorService = executorService;
            this.daemonExecutorService = daemonExecutorService;
        } catch (IOException e) {
            log.error("Connection problem with nickname={}, error={} - IOException", nickname, e.getMessage());
        }
    }

    private void timeoutShutdown() {
        daemonExecutorService.execute(() -> {
            try {
                Thread.sleep(30000);
                if (!isAuthorized) {
                    shutdown();
                }
            } catch (InterruptedException e) {
                log.warn("Daemon thread was interrupted, threadName={}, error={} - InterruptedException",
                        Thread.currentThread().getName(), e.getMessage());
            }
        });
    }

    public void start() {
        executorService.execute(() -> {
            authorize();
            while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                try {
                    String income = in.readUTF();
                    parseMessage(income);
                } catch (IOException e) {
                    log.error("Connection with nickname={} was terminated, error={} - IOException", nickname, e.getMessage());
                    server.removeClientHandler(this);
                    break;
                }
            }
        });
    }

    private void parseMessage(String message) {
        String[] split = message.split(REGEX);
        Command command = Command.getByCode(split[0]);
        switch(command) {
            case BROADCAST_MESSAGE -> server.broadcast(this.nickname, split[1]);
            case PRIVATE_MESSAGE -> server.privateMessage(split[1], this.nickname, split[2]);
            case CHANGE_NICK_MESSAGE -> changeNickname(split);
            default -> log.error("Message contains unknown Command.code={}.", split[0]);
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
                log.warn("Change nickname with wrong credentials, nickname={}, login={}, password{}.", nickname, split[2], split[3]);
            } catch (UserAlreadyExistsException e) {
                response = ERROR_MESSAGE.getCode() + REGEX + e.getMessage();
                log.warn("Change nickname to already existed, oldNickname={}, newNickname={}.", nickname, split[1]);
            }
            if (!Objects.equals(response, "")) {
                sendMessage(response);
            } else if (nick != null) {
                String message = "Changing nickname from " + nickname + " to " + nick + " is completed.";
                log.info("Change nickname completed, oldNickname={} to newNickname={}.", nickname, nick);
                nickname = nick;
                sendMessage(CHANGE_NICK_OK.getCode() + REGEX + nickname + REGEX + message);
                server.sendContacts();
            }
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            log.error("DataOutputStream sending message exception, error={} - IOException", e.getMessage());
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
                        log.warn("Authorize with wrong credentials, login={}, password={}.", split[1], split[2]);
                    }
                    if (nick != null && server.isUserAlreadyOnline(nick)) {
                        response = ERROR_MESSAGE.getCode() + REGEX + "User with this nickname is already online";
                        log.warn("Authorize under user already online, nickname={}.", nickname);
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
                        log.info("Authorize completed, nickname={}.", nickname);
                        break;
                    }
                } else if (Objects.equals(ADD_USER_MESSAGE.getCode(), split[0])) {
                    String nick = null;
                    String response = "";
                    try {
                        nick = server.getUserService().register(split[1], split[2], split[3]);
                    } catch (UserAlreadyExistsException e) {
                        response = ERROR_MESSAGE.getCode() + REGEX + e.getMessage();
                        log.warn("Register with login/nickname already exist, login={}, nickname={}.", split[2], split[1]);
                    }
                    if (!Objects.equals(response, "")) {
                        sendMessage(response);
                    } else if (nick != null){
                        String message = "Registration for " + nick + " is completed.";
                        sendMessage(ADD_USER_OK.getCode() + REGEX + message);
                        log.info("Register completed, nickname={}.", nickname);
                    }
                }
            }
        } catch (IOException e) {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    log.error("Socket closure problem, error={} - IOException", ex.getMessage());
                }
            }
            log.error("Connection with unauthorized user was terminated, error={} - IOException", e.getMessage());
        }
    }

    public void shutdown() {
        if (!Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
        }
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                log.error("Socket closure problem, error={} - IOException", e.getMessage());
            }
        }
    }

    public String getNickname() {
        return nickname;
    }
}
