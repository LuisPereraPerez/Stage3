package com.example;

import com.example.control.HazelcastManager;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HazelcastVerifier {
    public static void main(String[] args) {
        HazelcastInstance instance = HazelcastManager.getInstance();

        // Crear el programador para ejecutar cada minuto
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Tarea programada
        Runnable verificationTask = () -> {
            IMap<Integer, String> processedBooks = instance.getMap("processedBooks");
            IMap<Integer, Map<String, String>> metadataMap = instance.getMap("metadataMap");

            System.out.println("Processed Books:");
            processedBooks.forEach((id, content) -> System.out.println("ID: " + id + "\nContent: " + content));

            System.out.println("Metadata:");
            metadataMap.forEach((id, metadata) -> System.out.println("ID: " + id + "\nMetadata: " + metadata));
        };

        // Programar la tarea cada minuto
        scheduler.scheduleAtFixedRate(verificationTask, 0, 1, TimeUnit.MINUTES);

        // Nota: El programa continuará ejecutándose hasta que se detenga manualmente.
    }
}
