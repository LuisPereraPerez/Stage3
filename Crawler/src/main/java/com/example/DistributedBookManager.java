package com.example;

import com.example.control.DistributedBookDownloader;
import com.example.control.DistributedBookSynchronizer;
import com.example.control.DistributedMetadataSynchronizer;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class DistributedBookManager {

    public static void main(String[] args) {
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();

        // Execute tasks initially
        executeTasks(hazelcastInstance);

        // Schedule a task to run every 3 minutes
        Timer timer = new Timer(true); // "true" to make it a daemon thread
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                executeTasks(hazelcastInstance);
            }
        }, 180000, 180000); // Initial delay and period in milliseconds (3 minutes)

        // Keep the application running
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Hazelcast...");
            hazelcastInstance.shutdown();
        }));

        System.out.println("The system is running. Press Ctrl+C to stop.");
        while (true) {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private static void executeTasks(HazelcastInstance hazelcastInstance) {
        System.out.println("Starting tasks...");

        // Distributed map to track progress
        IMap<String, Boolean> taskCompletionMap = hazelcastInstance.getMap("task-completion");

        // Step 1: Download books across nodes
        System.out.println("Downloading books...");
        try {
            DistributedBookDownloader.downloadBooks(hazelcastInstance);
            markTaskAsComplete(taskCompletionMap, "download-books");
        } catch (Exception e) {
            System.err.println("Error downloading books: " + e.getMessage());
        }

        // Step 2: Wait for all nodes to finish downloading books
        waitForTaskCompletion(taskCompletionMap, "download-books", hazelcastInstance);

        // Step 3: Synchronize books across nodes
        System.out.println("Synchronizing books...");
        try {
            DistributedBookSynchronizer.synchronizeBooks(hazelcastInstance);
            markTaskAsComplete(taskCompletionMap, "synchronize-books");
        } catch (Exception e) {
            System.err.println("Error synchronizing books: " + e.getMessage());
        }

        // Step 4: Wait for all nodes to finish synchronizing books
        waitForTaskCompletion(taskCompletionMap, "synchronize-books", hazelcastInstance);

        // Step 5: Synchronize book metadata
        System.out.println("Synchronizing metadata...");
        try {
            DistributedMetadataSynchronizer.synchronizeMetadata(hazelcastInstance);
            markTaskAsComplete(taskCompletionMap, "synchronize-metadata");
        } catch (Exception e) {
            System.err.println("Error synchronizing metadata: " + e.getMessage());
        }

        // Step 6: Wait for all nodes to finish synchronizing metadata
        waitForTaskCompletion(taskCompletionMap, "synchronize-metadata", hazelcastInstance);

        System.out.println("Tasks completed.");
    }

    private static void markTaskAsComplete(IMap<String, Boolean> taskCompletionMap, String taskName) {
        taskCompletionMap.put(taskName, true);
        System.out.printf("Task '%s' marked as completed.%n", taskName);
    }

    private static void waitForTaskCompletion(IMap<String, Boolean> taskCompletionMap, String taskName, HazelcastInstance hazelcastInstance) {
        boolean allCompleted = false;
        while (!allCompleted) {
            try {
                // Ensure all nodes have marked the task as completed
                allCompleted = taskCompletionMap.values().stream().allMatch(Boolean::booleanValue);
                if (!allCompleted) {
                    System.out.printf("Waiting for all nodes to complete the task '%s'...%n", taskName);
                    TimeUnit.SECONDS.sleep(30); // Wait 30 seconds before checking again
                }
            } catch (InterruptedException e) {
                System.err.println("Wait interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        System.out.printf("All nodes have completed the task '%s'.%n", taskName);
    }
}
