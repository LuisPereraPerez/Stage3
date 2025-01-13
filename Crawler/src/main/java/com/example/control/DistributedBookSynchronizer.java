package com.example.control;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class DistributedBookSynchronizer {

    public static void synchronizeBooks(HazelcastInstance hazelcastInstance) {
        // Creates a distributed map called "books"
        IMap<Integer, String> booksMap = hazelcastInstance.getMap("books");
        IMap<String, Boolean> progressMap = hazelcastInstance.getMap("sync-progress"); // Map for monitoring

        String datalakeDir = "./datalake";
        String nodeName = String.valueOf(hazelcastInstance.getCluster().getLocalMember().getUuid()); // Unique name for each node

        // Step 1: Synchronize books to the distributed map
        new Thread(() -> {
            try (Stream<Path> paths = Files.walk(Path.of(datalakeDir))) {
                paths.filter(Files::isRegularFile)
                        .forEach(path -> {
                            try {
                                int bookId = Integer.parseInt(path.getFileName().toString().replace(".txt", ""));
                                String content = Files.readString(path);
                                System.out.printf("Book read from directory: ID %d%n", bookId);

                                if (!booksMap.containsKey(bookId)) {
                                    booksMap.put(bookId, content);
                                    System.out.printf("Book synchronized to the distributed map: ID %d%n", bookId);
                                } else {
                                    System.out.printf("Book already exists in the distributed map: ID %d%n", bookId);
                                }
                            } catch (IOException e) {
                                System.err.println("Error synchronising book to the distributed map: " + e.getMessage());
                            }
                        });
            } catch (IOException e) {
                System.err.println("Error reading the books directory: " + e.getMessage());
            } finally {
                // Marcar este nodo como terminado en el progreso
                progressMap.put(nodeName, true);
                System.out.println("Node process marked as completed: " + nodeName);
            }
        }).start();

        // Step 2: Wait until all nodes have uploaded their books
        waitForAllNodes(progressMap, hazelcastInstance);

        // Paso 3: Synchronize books from map to locally
        booksMap.forEach((bookId, content) -> {
            Path bookPath = Path.of(datalakeDir, bookId + ".txt");

            if (Files.notExists(bookPath)) {
                try {
                    Files.writeString(bookPath, content, StandardOpenOption.CREATE);
                    System.out.printf("Book obtained from the distributed map and saved locally: ID %d%n", bookId);
                } catch (IOException e) {
                    System.err.printf("Error while saving books: ID %d, Error: %s%n", bookId, e.getMessage());
                }
            } else {
                System.out.printf("Local book already exists, not saving necessary: ID %d%n", bookId);
            }
        });

        System.out.println("Books Synchronized Successfully.");
        hazelcastInstance.getLifecycleService().addLifecycleListener(event -> System.out.println("Hazelcast event: " + event));
    }

    private static void waitForAllNodes(IMap<String, Boolean> progressMap, HazelcastInstance hazelcastInstance) {
        boolean allCompleted = false;
        while (!allCompleted) {
            try {
                // Check if all nodes are marked as completed
                allCompleted = progressMap.values().stream().allMatch(Boolean::booleanValue);
                if (!allCompleted) {
                    System.out.println("Waiting for all the nodes to be synchronized...");
                    TimeUnit.SECONDS.sleep(30); // Wait until check again
                }
            } catch (InterruptedException e) {
                System.err.println("Await interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("All nodes have completed their synchronization with the distributed map.");
    }
}
