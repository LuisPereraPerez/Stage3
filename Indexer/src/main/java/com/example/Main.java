package com.example;

import com.example.control.DataLakeWatcher;
import com.example.control.ProcessedBooksManager;
import com.example.control.TSVIndexer;
import com.example.interfaces.BookIndexer;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        // Configurable paths
        String datalakeDirectoryPath = "./datalake"; // Directory where books will be saved

        try {
            // Initialize the manager for processed books
            System.out.println("Initializing ProcessedBooksManager...");
            ProcessedBooksManager processedBooksManager = new ProcessedBooksManager();

            // Initialize the indexer
            System.out.println("Initializing TSVIndexer...");
            BookIndexer indexer = new TSVIndexer(processedBooksManager);

            // Initialize and start the DataLakeWatcher
            System.out.println("Starting DataLakeWatcher...");
            DataLakeWatcher watcher = new DataLakeWatcher(datalakeDirectoryPath, indexer);

            System.out.println("System initialized successfully! Watching for new files...");
            watcher.watch();

        } catch (IOException e) {
            System.err.println("Error initializing the system: " + e.getMessage());
        }
    }
}
