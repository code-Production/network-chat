package ru.gb.network_chat.server;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.gb.network_chat.enums.Command;
import ru.gb.network_chat.server.service.UserService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static ru.gb.network_chat.constants.MessageConstants.REGEX;
import static ru.gb.network_chat.enums.Command.*;


public class Server {

    private static final int PORT = 6629;
    private static final int MAX_THREADS = 10;
    private static final Logger log = LogManager.getLogger(Server.class.getName());

    private final List<Handler> clientHandlers;
    private final List<Handler> unAuthHandlers;
    private final UserService userService;

    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private ExecutorService daemonExecutorService;
    private boolean shutdown = false;


    public Server(UserService userService) {
        this.userService = userService;
        clientHandlers = new ArrayList<>();
        unAuthHandlers = new ArrayList<>();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            executorService = Executors.newFixedThreadPool(MAX_THREADS);
            daemonExecutorService = Executors.newCachedThreadPool();// =< MAX_THREADS
            userService.start();
            log.info("Server socket started bound to port {}.", PORT);
            log.info("Fixed thread pool with {} threads started.", MAX_THREADS);
            log.info("Daemon cached thread pool started.");
            log.info("User service started.");
            consoleInput();
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                log.info("Connection with new client established.");
                Handler handler = new Handler(socket, this, executorService, daemonExecutorService);
                handler.start();
                addUnAuthHandler(handler);
            }
        } catch (IOException e) {
            if (shutdown) {
                log.warn("Server socket was shutdown manually, error={} - IOException.", e.getMessage());
            } else {
                log.error("Server socket error={} - IOException.", e.getMessage());
            }
        } finally {
            userService.stop();
            for (Handler handler : clientHandlers) {
                handler.shutdown();
            }
            for (Handler handler : unAuthHandlers){
                handler.shutdown();
            }
            if (executorService != null && !executorService.isTerminated()) {
                executorService.shutdownNow();
            }
            if (daemonExecutorService != null && !daemonExecutorService.isTerminated()) {
                daemonExecutorService.shutdownNow();
            }
            log.info("Server stopped.");
        }

    }

    public void shutdown() {
        shutdown = true;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log.error("Server socket closure error={} - IOException", e.getMessage());
            }
        }
    }

    private void consoleInput() {
        executorService.execute(() -> {
            System.out.println("Enter /end to shutdown");
            while (true) {
                try (Scanner sc = new Scanner(System.in)) {//
                    String input = sc.nextLine();
                    if (input.equalsIgnoreCase("/end")) {
                        shutdown();
                        break;
                    }
                }
            }
        });
    }

    public UserService getUserService() {
        return userService;
    }

    private synchronized void addUnAuthHandler(Handler handler) {
        unAuthHandlers.add(handler);
    }

    public synchronized void removeUnAuthHandler(Handler handler) {
        unAuthHandlers.remove(handler);
    }

    public synchronized void addClientHandler(Handler handler) {
        clientHandlers.add(handler);
        sendContacts();
    }

    public synchronized void removeClientHandler(Handler handler) {
        clientHandlers.remove(handler);
        sendContacts();
    }

    public void broadcast(String from, String message) {
        log.info("Server got broadcast message from nickname={}", from);
        String outcome = BROADCAST_MESSAGE.getCode() + REGEX + String.format("[%s]: %s", from, message);
        for (Handler handler : clientHandlers) {
            handler.sendMessage(outcome);
        }
    }

    public void privateMessage(String to, String from, String message) {
        log.info("Server got private message from nickname={}, to nickname={}", from, to);
        String outcome = PRIVATE_MESSAGE.getCode() + REGEX + String.format("[%s]: %s", from, message);
        for (Handler handler : clientHandlers) {
            if (handler.getNickname().equals(to)) {
                handler.sendMessage(outcome);
                break;
            }
        }
    }

    public boolean isUserAlreadyOnline(String nickname) {
        for (Handler handler : clientHandlers) {
            if (Objects.equals(handler.getNickname(), nickname)) return true;
        }
        return false;
    }

    public void sendContacts() {
        log.info("Server updated contacts on clients.");
        String message = clientHandlers.stream()
                .map(Handler::getNickname)
                .collect(Collectors.joining(REGEX));
        for (Handler handler : clientHandlers) {
            handler.sendMessage(Command.LIST_USERS.getCode() + REGEX + message);
        }
    }
}
