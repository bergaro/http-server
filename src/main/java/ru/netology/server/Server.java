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
    private final Map<String, Map<String, MyHandler>> handlers = new HashMap<>(); // Хранит обработчики запросов

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
     * Добавляет обработчик по методу запроса.
     * Если при добавлении обработчика необходимого метода запроса не сущестует, то создаёт его.
     * @param typeRequest метод запрос POST, GET
     * @param path путь запроса по RequestLine
     * @param handler обработчик запроса
     */
    public void addHandler(String typeRequest, String path, MyHandler handler) {
        if (!handlers.containsKey(typeRequest)) {
            handlers.put(typeRequest, new HashMap<>());
        }
        handlers.get(typeRequest).put(path, handler);
    }
    /**
     * Если сервер не содержит ни отдного обработчика, то он не запустится.
     */
    public void serverListenPort() {
        if(!handlers.isEmpty()) {
            doWork();
        } else {
            System.out.println("Не установлено ни одного обработчика");
        }
    }
    /**
     * Запускает сервер на определённом порту.
     * Создаёт пул потоков фиксированной длины и передаёт в него новые подключения
     */
    protected void doWork() {
        try (final var serverSocket = new ServerSocket(serverPort)) {
            serverSocket.setReuseAddress(true);
            ExecutorService threadPool = Executors.newFixedThreadPool(threadsVal);
            while (true) {
                final var socket = serverSocket.accept();
                if(handlers.size() > 0) threadPool.execute(getTask(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Описывает алгоритм работы отдельного потока.
     * @param socket сокет соединения
     * @return Runnable obj
     */
    protected Runnable getTask(Socket socket) {
        return () -> {
            try (var in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                var request = new Request();
                // must be in form GET /path HTTP/1.1
                var requestLine = in.readLine();
                var parts = requestLine.split(" ");
                var path = parts[1];
                if (parts.length != 3) {
                    getErrorResponse(socket, badRequest);
                } else {
                    if (!validPaths.contains(path)) {
                        getErrorResponse(socket, fileNotFound);
                    } else {
                        request.setRequestPath(path);
                        request.setMsgType(parts[0]);
                        sendMsg(request, socket);
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        };
    }
    /**
     * Новый метод отпарвки ответов сервера.
     * Теперь всю логику формирования и отправки ответа
     * должен содержать обработчик.
     * @param request запрос
     * @param socket сокет подключения
     */
    protected void sendMsg(Request request, Socket socket) {
        var typeMsg = request.getMsgType();
        var pathMsg = request.getRequestPath();
        if (handlers.containsKey(typeMsg)) {
            var handlerMap = handlers.get(typeMsg);
            if(handlerMap.containsKey(pathMsg)) {
                var requestHandler = handlerMap.get(pathMsg);
                try (BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())){
                    requestHandler.handle(request, out);
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        } else {
            System.out.println("Обработчика для такого типа запроса не существует.");
        }
    }
    /**
     * (НЕ ИСПОЛЬЗУЕТСЯ, устарел)
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
     * @param socket выходной поток
     * @param error ошибка сервера
     */
    private void getErrorResponse(Socket socket, String error) {
        try (BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())){
            String response = "HTTP/1.1 " + error + "\r\n" +
                    "Content-Length: 0\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";
            out.write(response.getBytes());
            out.flush();
            socket.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
    /**
     * Формирует responseLine и headers положительного сообщения.
     * @param mimeType тип документа
     * @param contentLength длина байт документа
     * @return "Шапка" документа как строка.
     */
    public String getTrueResponse(String mimeType, int contentLength) {
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
    public byte[] getDynamicPage(Path filePath) throws IOException {
        var template = Files.readString(filePath);
        return template.replace(
                "{time}",
                LocalDateTime.now().toString()
        ).getBytes();
    }


}
