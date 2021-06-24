package ru.netology.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private List<String> validPaths;        // Пути доступные для обработки сервером
    private int serverPort;                 // Порт сервера
    private int threadsVal;                 // Кол-во потоков
    private String fileNotFound;            // Ошибка 404
    private String badRequest;              // Ошибка 400
    private static Server server;           // Поле сервера

    private Server() {
        getProperties();
    }

    public static Server getInstance() {
        if(server == null) {
            server = new Server();
        }
        return server;
    }
    /**
     * Инициализирует поля класса значениями из файла конфигурации приложения
     */
    private void getProperties() {
        String configFileName = "config.properties";
        try (FileReader reader = new FileReader(configFileName)){
            Properties properties = new Properties();
            properties.load(reader);
            serverPort = Integer.parseInt(properties.getProperty("server.port"));
            threadsVal = Integer.parseInt(properties.getProperty("server.threadsValue"));
            fileNotFound = properties.getProperty("server.response.fileNotFound");
            badRequest = properties.getProperty("server.response.badRequest");
            validPaths = Arrays.asList(properties.getProperty("server.paths").split(","));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    /**
     * Запускает сервер на определённом порту.
     * Создаёт пул потоков фиксированной длины и передаёт в него новые подключения
     */
    public void doWork() {
        try (final var serverSocket = new ServerSocket(serverPort)) {
            serverSocket.setReuseAddress(true);
            ExecutorService threadPool = Executors.newFixedThreadPool(threadsVal);
            while (true) {
                    final var socket = serverSocket.accept();
                        threadPool.execute(getTask(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Описывает работу отдельного потока.
     * @param socket сокет соединения
     * @return Runnable obj
     */
    protected Runnable getTask(Socket socket) {
        return () -> {
            try (var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 var out = new BufferedOutputStream(socket.getOutputStream())) {
                // must be in form GET /path HTTP/1.1
                var requestLine = in.readLine();
                var parts = requestLine.split(" ");
                var path = parts[1];
                if (parts.length != 3) {
                    getErrorResponse(out, badRequest);
                } else {
                    if (!validPaths.contains(path)) {
                        getErrorResponse(out, fileNotFound);
                    } else {
                        sendMsg(path, out);
                    }
                }
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        };
    }
    /**
     * Отправляет путь запроса и отправляет соответствующий response
     * @param path путь запроса
     * @param out выходной поток
     */
    protected void sendMsg(String path, BufferedOutputStream out) throws IOException {
        var filePath = Path.of(".", "public", path);
        var mimeType = Files.probeContentType(filePath);
        // special case for classic
        if (path.equals("/classic.html")) {
            final var content = getDynamicPage(filePath);
            out.write(getTrueResponse(mimeType, content.length).getBytes());
            out.write(content);
        } else {
            final var length = Files.size(filePath);
            out.write(getTrueResponse(mimeType, (int) length).getBytes());
            Files.copy(filePath, out);
        }
        out.flush();
    }
    /**
     * Формирует и отправляет response - error.
     * @param out выходной поток
     * @param error ошибка сервера
     */
    private void getErrorResponse(BufferedOutputStream out, String error) throws IOException {
       String response = "HTTP/1.1 " + error + "\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n" +
                "\r\n";
       out.write(response.getBytes());
       out.flush();
    }
    /**
     * Формирует responseLine и headers положительного сообщения.
     * @param mimeType тип документа
     * @param contentLength длина байт документа
     * @return "Шапка" документа как строка.
     */
    protected String getTrueResponse(String mimeType, int contentLength) {
        return "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + mimeType + "\r\n" +
                "Content-Length: " + contentLength + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";
    }
    /**
     * Выполняет замену метки страницы на строку содержащую время
     * @param filePath путь к шаблону
     * @return html-документ в массиве байт
     */
    private byte[] getDynamicPage(Path filePath) throws IOException {
        var template = Files.readString(filePath);
        return template.replace(
                "{time}",
                LocalDateTime.now().toString()
        ).getBytes();
    }


}
