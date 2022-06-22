package ru.gb.network_chat.server.error;

public class WrongCredentialsException extends IllegalArgumentException{
    public WrongCredentialsException() {
    }

    public WrongCredentialsException(String s) {
        super(s);
    }
}
