package com.example;

import com.sun.net.httpserver.HttpServer;
import com.example.control.*;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Wait for files to be available
        waitForFiles("datamart/reverse_indexes");
        waitForFiles("datamart/book_metadata.csv");

        // Load indexes and metadata
        TSVIndexLoader indexLoader = new TSVIndexLoader();
        MetadataLoader metadataLoader = new MetadataLoader();

        var index = indexLoader.loadIndex("datamart/reverse_indexes");
        var metadata = metadataLoader.loadMetadata("datamart/book_metadata.csv");

        // Create the QueryProcessor
        QueryProcessor queryProcessor = new QueryProcessor(index, metadata);

        // Register routes
        server.createContext("/api/query", new QueryHandler(queryProcessor));

        // Start server
        server.setExecutor(null); // Use the default executor
        System.out.println("Server started at http://localhost:" + port);
        server.start();
    }

    private static void waitForFiles(String path) {
        File fileOrDir = new File(path);
        int retries = 30; // Maximum number of attempts
        int waitTime = 5; // Wait time between attempts (in seconds)

        System.out.println("Waiting for files to be available at: " + path);

        while (retries > 0) {
            if (fileOrDir.exists() && (fileOrDir.isFile() || fileOrDir.isDirectory())) {
                System.out.println("Files found at: " + path);
                return;
            }

            System.out.println("Files not found at: " + path + ". Retrying in " + waitTime + " seconds...");
            try {
                Thread.sleep(waitTime * 1000);
            } catch (InterruptedException e) {
                System.err.println("Error during wait: " + e.getMessage());
                Thread.currentThread().interrupt();
                throw new RuntimeException("Unexpected interruption while waiting for files.");
            }
            retries--;
        }

        throw new RuntimeException("Files not found at " + path + " after multiple attempts.");
    }
}
