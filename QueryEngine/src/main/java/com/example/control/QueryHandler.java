package com.example.control;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;

public class QueryHandler implements HttpHandler {
    private final QueryProcessor queryProcessor;

    public QueryHandler(QueryProcessor queryProcessor) {
        this.queryProcessor = queryProcessor;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, -1);
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        if (query == null || !query.startsWith("word=")) {
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, -1);
            return;
        }

        // Extract the word from the query string
        String word = query.split("=", 2)[1].replace("%20", " ");

        // Process the query
        var results = queryProcessor.processQuery(word);

        // Convert the results to a String
        String stringResponse = results.toString();

        // Send response as plain text
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, stringResponse.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(stringResponse.getBytes());
        }
    }
}
