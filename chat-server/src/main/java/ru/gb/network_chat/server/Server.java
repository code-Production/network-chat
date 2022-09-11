package ru.gb.network_chat.server;

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
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

import static ru.gb.network_chat.constants.MessageConstants.REGEX;
import static ru.gb.network_chat.enums.Command.*;

public class Server {

    private static final int PORT = 6629;
    private static final int MAX_THREADS = 10;

    private ExecutorService executorService;
    private ExecutorService daemonExecutorService;

    private final List<Handler> clientHandlers;
    private final List<Handler> unAuthHandlers;
    private final UserService userService;

    private ServerSocket serverSocket;


    public Server(UserService userService) {
        this.userService = userService;
        clientHandlers = new ArrayList<>();
        unAuthHandlers = new ArrayList<>();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started.");
            executorService = Executors.newFixedThreadPool(MAX_THREADS);
            daemonExecutorService = Executors.newCachedThreadPool();// =< MAX_THREADS
            System.out.println("Threads pool started");
            userService.start();
            consoleInput();
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                System.out.println("Connection with new client established.");
                Handler handler = new Handler(socket, this, executorService, daemonExecutorService);
                handler.start();
                addUnAuthHandler(handler);
            }
        } catch (IOException e) {
//            e.printStackTrace();
            System.out.println("Server socket was closed.");
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
            System.out.println("Server stopped.");
        }

    }

    public void shutdown() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
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
        String outcome = BROADCAST_MESSAGE.getCode() + REGEX + String.format("[%s]: %s", from, message);
        for (Handler handler : clientHandlers) {
            handler.sendMessage(outcome);
        }
    }

    public void privateMessage(String to, String from, String message) {
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
        String message = clientHandlers.stream()
                .map(Handler::getNickname)
                .collect(Collectors.joining(REGEX));
        for (Handler handler : clientHandlers) {
            handler.sendMessage(Command.LIST_USERS.getCode() + REGEX + message);
        }
    }
}
