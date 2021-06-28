package ru.netology;

import ru.netology.server.MyHandler;
import ru.netology.server.Request;
import ru.netology.server.Server;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

  public static void main(String[] args) {
    Server server = Server.getInstance();
    // Обработчик для запросов index.html
    server.addHandler("GET", "/index.html", new MyHandler() {
      @Override
      public void handle(Request request, BufferedOutputStream out) {
        getHandlerBody(server, request, out);
      }
    });
    // Обработчик для запросов classic.html
    server.addHandler("GET", "/classic.html", new MyHandler() {
      @Override
      public void handle(Request request, BufferedOutputStream out) {
        try {
          var filePath = Path.of(".", "public", request.getRequestPath());
          var mimeType = Files.probeContentType(filePath);
          var content = server.getDynamicPage(filePath);
          out.write(server.getTrueResponse(mimeType, content.length).getBytes());
          out.write(content);
          out.flush();
        } catch (IOException ex) {
          System.out.println(ex.getMessage());
        }
      }
    });
    // Обработчик для запросов forms.html (GET with params)
    server.addHandler("GET", "/forms.html", new MyHandler() {
      @Override
      public void handle(Request request, BufferedOutputStream out) {
        getHandlerBody(server, request, out);
      }
    });
    server.serverListenPort();
  }
  /**
   * Большинство обработчиков формируют статическую страницу,
   * ввиду чего был выделен код, который может дублироваться
   * @param server объект класса Server
   * @param request объект класса Request
   * @param out потока вывода BufferedOutputStream
   */
  private static void getHandlerBody(Server server, Request request, BufferedOutputStream out) {
    try {
      var filePath = Path.of(".", "public", request.getRequestPath());
      var mimeType = Files.probeContentType(filePath);
      var length = Files.size(filePath);
      out.write(server.getTrueResponse(mimeType, (int) length).getBytes());
      Files.copy(filePath, out);
      out.flush();
    } catch (IOException ex) {
      System.out.println(ex.getMessage());
    }
  }
}


