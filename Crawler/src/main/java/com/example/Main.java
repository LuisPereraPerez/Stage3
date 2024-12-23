package com.example;

import com.example.control.*;
import com.example.interfaces.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final int NUM_BOOKS = 5;
    private static final String SAVE_DIR = "datalake";
    private static final int MAX_DOWNLOAD_ATTEMPTS = 3;

    public static void main(String[] args) {
        // Crear instancias de las interfaces necesarias
        BookDownloader downloader = new GutenbergBookDownloader(SAVE_DIR);
        MetadataExtractor metadataExtractor = new GutenbergMetadataExtractor();
        BookProcessor bookProcessor = new GutenbergBookProcessor();
        MetadataWriter metadataWriter = new CSVMetadataWriter();
        LastIdManager lastIdManager = new FileLastIdManager();

        // Crear el objeto BookManager que gestiona el proceso completo
        BookManager bookManager = new BookManager(downloader, metadataExtractor, bookProcessor, metadataWriter, SAVE_DIR);

        // Definir el tamaño del pool de hilos para el crawling paralelo
        int threadPoolSize = 4;
        GutenbergCrawler crawler = new GutenbergCrawlerParallel(bookManager, lastIdManager, MAX_DOWNLOAD_ATTEMPTS, threadPoolSize);

        // Crear una instancia de Hazelcast
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();

        // Crear el sincronizador de Hazelcast
        HazelcastFileSynchronizer synchronizer = new HazelcastFileSynchronizer(hazelcastInstance);

        // Crear el programador para ejecutar el crawling periódicamente
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Tarea programada para el crawling
        Runnable crawlTask = () -> {
            System.out.println("Ejecutando el proceso de crawling...");
            crawler.startCrawling(NUM_BOOKS);

            // Sincronizar después de completar el proceso de crawling
            synchronizer.synchronizeProcessedBooks();  // Sincroniza los libros procesados
            try {
                synchronizer.synchronizeMetadata();        // Sincroniza los metadatos
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        // Programar la tarea cada minuto
        scheduler.scheduleAtFixedRate(crawlTask, 0, 1, TimeUnit.MINUTES);
    }
}
