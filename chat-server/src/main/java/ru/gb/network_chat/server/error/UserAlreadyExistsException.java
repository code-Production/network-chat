package ru.gb.network_chat.server.error;

public class UserAlreadyExistsException extends IllegalArgumentException {

    public UserAlreadyExistsException() {
    }

    public UserAlreadyExistsException(String s) {
        super(s);
    }
}
