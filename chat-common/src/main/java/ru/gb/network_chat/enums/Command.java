package ru.gb.network_chat.enums;

import java.util.Objects;

public enum Command {
    AUTH_MESSAGE("/auth"),
    AUTH_OK("/auth_ok"),
    ADD_USER_MESSAGE("/add_user"),
    ADD_USER_OK("/add_user_ok"),
    CHANGE_NICK_MESSAGE("/change_nick"),
    CHANGE_NICK_OK("/change_nick_ok"),
    BROADCAST_MESSAGE("/broadcast"),
    PRIVATE_MESSAGE("/private"),
    ERROR_MESSAGE("/error"),
    LIST_USERS("/list");

    private final String code;

    Command(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static Command getByCode(String code) {
        for (Command c: values()) {
            if (Objects.equals(c.code, code)) return c;
        }
        throw new IllegalArgumentException("Unknown code for enum Command: " + code);
    }
}
