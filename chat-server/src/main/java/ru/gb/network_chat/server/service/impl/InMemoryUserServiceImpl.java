package ru.gb.network_chat.server.service.impl;

import ru.gb.network_chat.server.error.UserAlreadyExistsException;
import ru.gb.network_chat.server.error.WrongCredentialsException;
import ru.gb.network_chat.server.model.User;
import ru.gb.network_chat.server.service.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InMemoryUserServiceImpl implements UserService {
    private final List<User> users;

    public InMemoryUserServiceImpl() {
        this.users = new ArrayList<>();
    }

    @Override
    public void start() {
        System.out.println("User service started.");
        users.addAll(List.of(
                new User("log1", "pass1", "nick1"),
                new User("log2", "pass2", "nick2"),
                new User("log3", "pass3", "nick3"),
                new User("log4", "pass4", "nick4"),
                new User("log5", "pass5", "nick5")));
    }

    @Override
    public void stop() {
        System.out.println("User service stopped.");
    }

    @Override
    public String authenticate(String login, String password) throws WrongCredentialsException {
        for (User user : users) {
            if (Objects.equals(user.getLogin(), login) && Objects.equals(user.getPassword(), password)) {
                return user.getNickname();
            }
        }
        throw new WrongCredentialsException("Wrong credentials for user: " + login + ", with password: " + password + ".");
    }

    @Override
    public String register(String nickname, String login, String password) throws UserAlreadyExistsException {
        return null;
    }

    @Override
    public String changeNickname(String old_nickname, String new_nickname, String login, String password) throws WrongCredentialsException, UserAlreadyExistsException {
        return null;
    }
}
