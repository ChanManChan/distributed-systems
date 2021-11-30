package loadbalancing.haproxy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class WebServer {
    private static final String STATUS_ENDPOINT = "/status";
    private static final String HOME_PAGE_ENDPOINT = "/";
    private static final String HTML_PAGE = "/haproxy/index.html";
    private final int port;
    private final String serverName;

    public WebServer(int port, String serverName) {
        this.port = port;
        this.serverName = serverName;
    }

    public void startServer() {
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        server.createContext(STATUS_ENDPOINT, this::handleStatusCheckRequest);
        server.createContext(HOME_PAGE_ENDPOINT, this::handleHomePageRequest);

        server.setExecutor(Executors.newFixedThreadPool(8));
        System.out.printf("Started server %s on port %d%n", serverName, port);
        server.start();
    }

    private void handleHomePageRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("get")) {
            exchange.close();
            return;
        }

        System.out.printf("%s received a request%n", this.serverName);
        exchange.getResponseHeaders().add("Content-Type", "text/html");
        exchange.getResponseHeaders().add("Cache-Control", "no-cache");
        byte[] response = loadHtml(HTML_PAGE);
        sendResponse(response, exchange);
    }

    private byte[] loadHtml(String htmlFilePath) throws IOException {
        InputStream htmlInputStream = getClass().getResourceAsStream(htmlFilePath);
        if (htmlInputStream == null) {
            return "Index page not found".getBytes();
        }

        Document document = Jsoup.parse(htmlInputStream, "UTF-8", "");

        String modifiedHtml = modifyHtmlDocument(document);
        return modifiedHtml.getBytes();
    }

    private String modifyHtmlDocument(Document document) {
        Element serverNameElement = document.selectFirst("#server_name");
        if (serverNameElement != null) {
            serverNameElement.appendText(serverName);
            return document.toString();
        }
        return "";
    }

    private void handleStatusCheckRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("get")) {
            exchange.close();
            return;
        }

        System.out.println("Received a health check");
        String responseMessage = "Server is alive\n";
        sendResponse(responseMessage.getBytes(), exchange);
    }

    private void sendResponse(byte[] responseBytes, HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(responseBytes);
        outputStream.flush();
        outputStream.close();
    }
}
