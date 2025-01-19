package com.example.control;

import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;

import com.example.control.*;
import com.example.interfaces.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class DistributedBookDownloader {
    static Integer N = 15;

    public static void downloadBooks(HazelcastInstance hazelcastInstance) throws Exception {
        waitForClusterSize(hazelcastInstance, 3);

        Integer lastID = readLastBookId();

        List<Integer> bookIds = createBookIds(lastID, N);

        saveLastBookId(bookIds);

        List<String> results = downloadBooksDistributed(hazelcastInstance, bookIds);

        // Procesar resultados (opcional)
        for (String result : results) {
            System.out.println(result);
        }
    }

    private static List<Integer> createBookIds(Integer lastID, int size) {
        List<Integer> bookIds = new ArrayList<>();
        for (int i = 1; i <= size; i++) {
            bookIds.add(lastID + i);  // Start from lastID + 1 and continue until reach 'size'
        }
        return bookIds;
    }

    private static void saveLastBookId(List<Integer> idsDeLibros) {
        try {
            Path filePath = Paths.get("recursos_crawler", "last_book_id.txt");
            String last_id = idsDeLibros.get(idsDeLibros.size() - 1).toString();
            Files.write(filePath, last_id.getBytes());
            System.out.println("Last book ID saved to: " + filePath);
        } catch (IOException e) {
            System.err.println("Failed to save last book ID: " + e.getMessage());
        }
    }

    private static Integer readLastBookId() {
        try {
            Path filePath = Paths.get("recursos_crawler", "last_book_id.txt");
            if (Files.exists(filePath)) {
                String lastId = new String(Files.readAllBytes(filePath)).trim();
                if (lastId.isEmpty()) {
                    return 0;
                }
                return Integer.parseInt(lastId);
            } else {
                System.err.println("File not found: " + filePath);
                return 0;
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error reading last book ID: " + e.getMessage());
            return 0;
        }
    }

    private static List<String> downloadBooksDistributed(HazelcastInstance hazelcastInstance, List<Integer> idsDeLibros) throws Exception {
        Set<Member> miembros = hazelcastInstance.getCluster().getMembers();
        List<Member> listaDeMiembros = new ArrayList<>(miembros);

        IExecutorService servicioEjecutor = hazelcastInstance.getExecutorService("ejecutorDescargadorDeLibros");
        List<Future<String>> futuros = new ArrayList<>();

        int totalLibros = idsDeLibros.size();
        int librosPorNodo = totalLibros / listaDeMiembros.size();
        int librosRestantes = totalLibros % listaDeMiembros.size(); // Libros sobrantes a repartir

        int inicioIdx = 0;
        for (int i = 0; i < listaDeMiembros.size(); i++) {
            // Calcular finIdx considerando los libros sobrantes distribuidos
            int librosExtra = (i < librosRestantes) ? 1 : 0;
            int finIdx = inicioIdx + librosPorNodo + librosExtra;

            // Subconjunto de libros para este nodo
            List<Integer> subconjuntoDeLibros = idsDeLibros.subList(inicioIdx, finIdx);
            Member miembroDestino = listaDeMiembros.get(i);

            System.out.printf("Assigning books %d to %d to node: %s%n", inicioIdx, finIdx - 1, miembroDestino);

            // Enviar tarea al nodo correspondiente
            Future<String> futuro = servicioEjecutor.submitToMember(
                    new BookDownloadTask(subconjuntoDeLibros), miembroDestino);
            futuros.add(futuro);

            // Actualizar el índice inicial para el siguiente nodo
            inicioIdx = finIdx;
        }

        // Recoger resultados
        List<String> resultados = new ArrayList<>();
        for (Future<String> futuro : futuros) {
            try {
                resultados.add(futuro.get());
            } catch (Exception e) {
                resultados.add("Error downloading books");
                e.printStackTrace();
            }
        }
        return resultados;
    }

    private static void waitForClusterSize(HazelcastInstance hazelcastInstance, int clusterSize) throws InterruptedException {
        while (hazelcastInstance.getCluster().getMembers().size() < clusterSize) {
            Thread.sleep(1000);
        }
    }

    public static class BookDownloadTask implements Callable<String> {
        private final List<Integer> bookIds;
        private final BookDownloader downloader;
        private final BookProcessor bookProcessor;

        public BookDownloadTask(List<Integer> bookIds) {
            this.bookIds = bookIds;
            this.downloader = new GutenbergBookDownloader("datalake");
            this.bookProcessor = new GutenbergBookProcessor();
        }

        @Override
        public String call() {
            StringBuilder result = new StringBuilder();
            for (Integer bookId : bookIds) {
                try {
                    downloader.downloadBook(bookId);
                    System.out.printf("Book downloaded & saved: ID %d%n", bookId);

                    bookProcessor.processBook(bookId);
                    System.out.printf("Book Processed: ID %d%n", bookId);

                    result.append("Downloaded and processed book with ID: ").append(bookId).append("\n");
                } catch (IOException e) {
                    result.append("Failed to download book with ID: ").append(bookId).append("\n");
                }
            }
            return result.toString();
        }

    }
}
