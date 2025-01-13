package com.example.control;

import java.io.*;
import java.nio.file.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 * Manages a set of processed books, ensuring no duplicates are indexed.
 */
public class ProcessedBooksManager {
    private final String indexerResourcesPath = "./indexer_resources/processed_books.txt";
    private final Set<String> processedBooks = Collections.synchronizedSet(new HashSet<>());

    public ProcessedBooksManager() throws IOException {
        initializeIndexerResources();
        loadProcessedBooks();
    }

    /**
     * Ensures the indexer_resources directory and file exist.
     */
    private void initializeIndexerResources() throws IOException {
        Path indexerDir = Path.of(indexerResourcesPath).getParent();
        if (indexerDir != null && !Files.exists(indexerDir)) {
            Files.createDirectories(indexerDir);
            System.out.println("Created indexer resources directory: " + indexerDir);
        }

        File indexerFile = new File(indexerResourcesPath);
        if (!indexerFile.exists()) {
            if (indexerFile.createNewFile()) {
                System.out.println("Created indexer resources file: " + indexerResourcesPath);
            }
        }
    }

    /**
     * Loads processed books from the indexer resources file.
     */
    private void loadProcessedBooks() throws IOException {
        synchronized (processedBooks) {
            File indexerFile = new File(indexerResourcesPath);
            if (indexerFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(indexerFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        processedBooks.add(line.trim());
                    }
                }
            }
        }
    }

    /**
     * Checks if a book ID has already been processed.
     *
     * @param bookId the ID of the book to check
     * @return true if the book has been processed, false otherwise
     */
    public synchronized boolean isProcessed(String bookId) {
        return processedBooks.contains(bookId);
    }

    /**
     * Marks a book as processed and updates the indexer resources file.
     *
     * @param bookId the ID of the book to mark as processed
     * @throws IOException if an error occurs while writing to the file
     */
    public synchronized void markAsProcessed(String bookId) throws IOException {
        if (processedBooks.add(bookId)) {
            // Save to indexer_resources
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(indexerResourcesPath, true))) {
                writer.write(bookId);
                writer.newLine();
            }
        }
    }
}
