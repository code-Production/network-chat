package ru.gb.network_chat.server;

import ru.gb.network_chat.server.service.impl.DatabaseUserServiceImpl;
import ru.gb.network_chat.server.service.impl.InMemoryUserServiceImpl;

public class App {
    public static void main(String[] args) {
//        new Server(new InMemoryUserServiceImpl()).start();
        new Server(new DatabaseUserServiceImpl()).start();
    }
}
