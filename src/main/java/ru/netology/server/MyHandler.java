package ru.netology.server;

import java.io.BufferedOutputStream;

@FunctionalInterface
public interface MyHandler {
    void handle(Request request, BufferedOutputStream outputStream);
}
