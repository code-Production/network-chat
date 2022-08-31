package ru.gb.network_chat.server.service;

import ru.gb.network_chat.server.error.UserAlreadyExistsException;
import ru.gb.network_chat.server.error.WrongCredentialsException;

public interface UserService {
    void start();
    void stop();
    String authenticate(String login, String password) throws WrongCredentialsException;
    String register(String nickname, String login, String password) throws UserAlreadyExistsException;
    String changeNickname(String nickname, String login, String password) throws WrongCredentialsException,UserAlreadyExistsException;
    //other user functions
}
