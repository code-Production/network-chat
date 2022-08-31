package ru.gb.network_chat.server.service.impl;

import ru.gb.network_chat.server.error.UserAlreadyExistsException;
import ru.gb.network_chat.server.error.WrongCredentialsException;
import ru.gb.network_chat.server.service.UserService;

import java.sql.*;

public class DatabaseUserServiceImpl implements UserService {

    private Connection connection;
    private Statement statement;

    @Override
    public void start() {
        try {
            connect();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            disconnect();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String authenticate(String login, String password) throws WrongCredentialsException {
        try {
            PreparedStatement prepStat = connection.prepareStatement("SELECT nickname FROM Clients WHERE login = ? and password = ?");
            prepStat.setString(1, login);
            prepStat.setString(2, password);
            prepStat.addBatch();
            ResultSet rs = prepStat.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new WrongCredentialsException("Wrong credentials for user: " + login + ", with password: " + password + ".");

    }

    @Override
    public String register(String nickname, String login, String password) throws UserAlreadyExistsException {
        try {
            PreparedStatement prepStat = connection.prepareStatement("SELECT login FROM Clients WHERE login = ? or nickname = ?");
            prepStat.setString(1, login);
            prepStat.setString(2, nickname);
            prepStat.addBatch();
            ResultSet rs = prepStat.executeQuery();
            if (!rs.next()) {
                prepStat = connection.prepareStatement("INSERT INTO Clients (login, password, nickname) VALUES (?, ?, ?)");
                prepStat.setString(1, login);
                prepStat.setString(2, password);
                prepStat.setString(3, nickname);
                prepStat.addBatch();
                int result = prepStat.executeUpdate();
                if (result != 0) {
                    return nickname;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new UserAlreadyExistsException("User with login '" + login + "' or nickname '" + nickname + "' already exists.");
    }

    @Override
    public String changeNickname(String nickname, String login, String password) throws WrongCredentialsException, UserAlreadyExistsException {
        try {
            PreparedStatement prepStat = connection.prepareStatement("SELECT nickname FROM Clients WHERE login = ? and password = ?");
            prepStat.setString(1, login);
            prepStat.setString(2, password);
            prepStat.addBatch();
            ResultSet rs = prepStat.executeQuery();
            if (rs.next()) {
                prepStat = connection.prepareStatement("SELECT nickname FROM Clients WHERE nickname = ?"); //to avoid exception in UPDATE UNIQUE
                prepStat.setString(1, nickname);
                rs = prepStat.executeQuery();
                if (!rs.next()) {
                    prepStat = connection.prepareStatement("UPDATE Clients SET nickname = ? WHERE login = ? and password = ?");
                    prepStat.setString(1, nickname);
                    prepStat.setString(2, login);
                    prepStat.setString(3, password);
                    prepStat.addBatch();
                    int result = prepStat.executeUpdate();
                    if (result != 0) {
                        return nickname;
                    }
                }
            } else {
                throw new WrongCredentialsException("Wrong credentials for user: " + login + ", with password: " + password + ".");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new UserAlreadyExistsException("User with such nickname '" + nickname + "' already exists.");
    }

    private void connect() throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:chat-server/server.db");
        statement = connection.createStatement();
        statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS clients (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                login TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                nickname TEXT NOT NULL UNIQUE
                );""");
    }

    private void disconnect() throws SQLException {
        if (connection != null) {
            connection.close();
        }
        if (statement != null) {
            statement.close();
        }
    }

}
