package org.example;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.example.control.DistributedTSVIndexer;

public class Main {
    public static void main(String[] args) {
        // Create a Hazelcast instance
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();

        // Initialize the DistributedTSVIndexer task
        DistributedTSVIndexer indexerTask = new DistributedTSVIndexer(hazelcastInstance);

        // Execute the task on the Hazelcast cluster
        hazelcastInstance.getExecutorService("default").execute(indexerTask);

        // Optionally shut down Hazelcast after the task is completed
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Hazelcast instance...");
            hazelcastInstance.shutdown();
        }));
    }
}