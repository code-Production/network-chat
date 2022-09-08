package ru.gb.network_chat.client;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class ChatHistoryManager {

    FileOutputStream fileOutputStream;
    File dir;
    File file;

    public ChatHistoryManager(String folderName, String fileName) {

        dir = new File(folderName);
        file = new File(dir, String.format("[%s]", fileName));

        if (!dir.exists()) {
            if (!dir.mkdir()) {
                System.out.printf("History folder '%s' cannot be created.%n", dir.getPath());
            }
        }
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    System.out.printf("History file '%s' cannot be created.", file.getName());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            fileOutputStream = new FileOutputStream(file, true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void saveChatHistory(String message) {
        byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
        try {
            fileOutputStream.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String loadChatHistory(int lastNLines) {
        StringBuilder sb = new StringBuilder();
        try(BufferedReader reader = new BufferedReader(new FileReader(file));
            BufferedReader counter = new BufferedReader(new FileReader(file))) {
            long maxLines = counter.lines().count();
            int count = 1;
            String line;
            while((line = reader.readLine()) != null && count <= maxLines) {
                if (count++ > maxLines - lastNLines) {
                    sb.append(line).append(System.lineSeparator());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public void shutdown() {
        try {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
