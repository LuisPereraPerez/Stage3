package com.example.control;

import com.example.interfaces.BookIndexer;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

/**
 * Monitors a directory for new files and processes them using the provided indexer.
 */
public class DataLakeWatcher {
    private final Path directoryPath;
    private final BookIndexer indexer;
    private final BlockingQueue<String> bookQueue = new LinkedBlockingQueue<>();

    public DataLakeWatcher(String directoryPath, BookIndexer indexer) {
        this.directoryPath = Paths.get(directoryPath).toAbsolutePath().normalize();
        this.indexer = indexer;

        if (!Files.exists(this.directoryPath) || !Files.isDirectory(this.directoryPath)) {
            throw new IllegalArgumentException("Invalid directory path: " + this.directoryPath);
        }

        // Load any existing files in the directory into the queue
        initializeQueue();
    }

    public void watch() {
        System.out.println("Listening in directory: " + directoryPath);

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            directoryPath.register(watchService, ENTRY_CREATE);

            // Start a thread to process the queue
            new Thread(this::processQueue).start();

            while (true) {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == ENTRY_CREATE) {
                        Path newFilePath = directoryPath.resolve((Path) event.context());
                        if (isValidNumericFile(newFilePath)) {
                            System.out.println("File added to queue: " + newFilePath);
                            bookQueue.offer(newFilePath.toString());
                        } else {
                            System.out.println("File ignored: " + newFilePath);
                        }
                    }
                }

                if (!key.reset()) {
                    System.err.println("Watcher is no longer valid. Stopping.");
                    break;
                }
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error in watcher: " + e.getMessage());
        }
    }

    private void initializeQueue() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.txt")) {
            for (Path filePath : stream) {
                if (isValidNumericFile(filePath)) {
                    System.out.println("Preloading file into queue: " + filePath);
                    bookQueue.offer(filePath.toString());
                }
            }
        } catch (IOException e) {
            System.err.println("Error initializing queue with existing files: " + e.getMessage());
        }
    }

    private void processQueue() {
        while (true) {
            try {
                // Poll the next file from the queue and process it
                String filePath = bookQueue.take();
                System.out.println("Processing file: " + filePath);
                indexer.indexBook(filePath);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Queue processing interrupted.");
                break;
            } catch (IOException e) {
                System.err.println("Error processing file: " + e.getMessage());
            }
        }
    }

    private boolean isValidNumericFile(Path filePath) {
        String fileName = filePath.getFileName().toString();
        return fileName.matches("\\d+\\.txt");
    }
}
