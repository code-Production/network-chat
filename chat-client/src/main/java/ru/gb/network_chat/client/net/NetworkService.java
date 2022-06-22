package ru.gb.network_chat.client.net;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import ru.gb.network_chat.client.ChatController;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class NetworkService {

    private static final int PORT = 6629;
    private static final String HOST = "127.0.0.1";
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Thread networkServiceThread;
    private final MessageProcessor messageProcessor;
    private final ChatController chatController;

    public NetworkService(MessageProcessor messageProcessor, ChatController chatController) {
        this.messageProcessor = messageProcessor;
        this.chatController = chatController;
        try {
            connect();
        } catch (IOException e) {
//            e.printStackTrace();
            System.out.println("No connection to server.");
        }
    }

    public void connect() throws IOException {
        this.socket = new Socket(HOST, PORT);
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        readMessage();
        System.out.println("Network service started.");
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void shutdown() {
        if (networkServiceThread != null && !networkServiceThread.isInterrupted()) {
            networkServiceThread.interrupt();
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readMessage() {
        networkServiceThread = new Thread(() -> {
            try {
                while(!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                    String income = in.readUTF();
                    messageProcessor.processMessage(income);
                }
            } catch (IOException e) {
//                e.printStackTrace();
                String message = "Server closed the connection.";
                chatController.showError(message);
                chatController.showLoginPanel();
                System.out.println(message);
            } finally {
                shutdown();
                System.out.println("Socket was closed. Network service stopped.");
            }

        });
        networkServiceThread.start();
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
