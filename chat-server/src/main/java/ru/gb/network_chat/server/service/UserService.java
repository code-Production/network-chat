package ru.gb.network_chat.server.service;

import ru.gb.network_chat.server.error.WrongCredentialsException;

public interface UserService {
    void start();
    void stop();
    String authenticate(String login, String password) throws WrongCredentialsException;
    //other user functions
}
