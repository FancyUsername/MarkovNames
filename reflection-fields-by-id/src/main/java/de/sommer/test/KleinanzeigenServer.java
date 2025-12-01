package de.sommer.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class KleinanzeigenServer {
        private static final Gson GSON = new Gson();

        public static void main(String[] args) throws IOException {
                int port = Integer.parseInt(Optional.ofNullable(System.getenv("PORT")).orElse("8080"));
                KleinanzeigenClient client = new KleinanzeigenClient();

                HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
                server.createContext("/", exchange -> serveIndex(exchange));
                server.createContext("/static/", KleinanzeigenServer::serveStatic);
                server.createContext("/api/search", exchange -> handleSearch(exchange, client));

                server.start();
                System.out.printf("Kleinanzeigen visualizer listening on http://localhost:%d%n", port);
        }

        private static void handleSearch(HttpExchange exchange, KleinanzeigenClient client) throws IOException {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(405, -1);
                        return;
                }

                Map<String, String> params = queryToMap(exchange.getRequestURI().getRawQuery());
                String query = params.getOrDefault("q", "");
                int page = safeParseInt(params.get("page"), 1);

                try {
                        List<SearchResult> results = client.search(query, page);
                        byte[] body = GSON.toJson(results).getBytes(StandardCharsets.UTF_8);
                        Headers headers = exchange.getResponseHeaders();
                        headers.add("Content-Type", "application/json; charset=utf-8");
                        exchange.sendResponseHeaders(200, body.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                                os.write(body);
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                        byte[] body = ("{\"error\":\"" + e.getMessage() + "\"}").getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                        exchange.sendResponseHeaders(500, body.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                                os.write(body);
                        }
                }
        }

        private static void serveIndex(HttpExchange exchange) throws IOException {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(405, -1);
                        return;
                }
                serveResource(exchange, "static/index.html", "text/html; charset=utf-8");
        }

        private static void serveStatic(HttpExchange exchange) throws IOException {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(405, -1);
                        return;
                }
                String path = exchange.getRequestURI().getPath().replaceFirst("/static/?", "static/");
                String contentType = path.endsWith(".js") ? "application/javascript; charset=utf-8"
                                : path.endsWith(".css") ? "text/css; charset=utf-8"
                                                : "application/octet-stream";
                serveResource(exchange, path, contentType);
        }

        private static void serveResource(HttpExchange exchange, String resourcePath, String contentType) throws IOException {
                byte[] body = readResource(resourcePath);
                if (body == null) {
                        exchange.sendResponseHeaders(404, -1);
                        return;
                }
                exchange.getResponseHeaders().add("Content-Type", contentType);
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                        os.write(body);
                }
        }

        private static byte[] readResource(String path) throws IOException {
                try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
                        if (stream == null) {
                                return null;
                        }
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        byte[] data = new byte[4096];
                        int nRead;
                        while ((nRead = stream.read(data, 0, data.length)) != -1) {
                                buffer.write(data, 0, nRead);
                        }
                        return buffer.toByteArray();
                }
        }

        private static Map<String, String> queryToMap(String query) {
                Map<String, String> map = new HashMap<>();
                if (query == null || query.isEmpty()) {
                        return map;
                }
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                        String[] kv = pair.split("=", 2);
                        String key = decode(kv[0]);
                        String value = kv.length > 1 ? decode(kv[1]) : "";
                        map.put(key, value);
                }
                return map;
        }

        private static int safeParseInt(String value, int defaultValue) {
                try {
                        return Integer.parseInt(value);
                } catch (Exception e) {
                        return defaultValue;
                }
        }

        private static String decode(String value) {
                return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }
}
