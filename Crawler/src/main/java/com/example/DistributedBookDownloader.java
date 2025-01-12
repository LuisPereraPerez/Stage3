package com.example;

import com.hazelcast.cluster.Member;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;

import com.example.control.*;
import com.example.interfaces.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class DistributedBookDownloader {
    static Integer N = 15;

    public static void main(String[] args) throws Exception {
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();

        waitForClusterSize(hazelcastInstance, 3);

        Integer lastID = readLastBookId();

        List<Integer> bookIds = createBookIds(lastID, N);

        saveLastBookId(bookIds);

        List<String> results = downloadBooksDistributed(hazelcastInstance, bookIds);

        hazelcastInstance.shutdown();
    }

    private static List<Integer> createBookIds(Integer lastID, int size) {
        List<Integer> bookIds = new ArrayList<>();
        for (int i = 1; i <= size; i++) {
            bookIds.add(lastID + i);  // Empezar desde lastID + 1 y continuar hasta obtener 'size' números
        }
        return bookIds;
    }

    // Guardar los IDs de libros descargados en un archivo local (en cada nodo)
    private static void saveLastBookId(List<Integer> idsDeLibros) {
        try {
            // Cada nodo guarda su archivo de estado de forma independiente en su datalake
            Path filePath = Paths.get("recursos_crawler", "last_book_id.txt");

            // Obtener el último ID de libro descargado (último de la lista de libros descargados)
            String last_id = idsDeLibros.get(idsDeLibros.size() - 1).toString();

            // Escribir el último ID descargado en el archivo
            Files.write(filePath, last_id.getBytes());

            System.out.println("Last book ID saved to: " + filePath);
        } catch (IOException e) {
            System.err.println("Failed to save last book ID: " + e.getMessage());
        }
    }

    // Leer el último ID de libro almacenado en el archivo local
    private static Integer readLastBookId() {
        try {
            // Ruta del archivo donde se guarda el último ID de libro
            Path filePath = Paths.get("recursos_crawler", "last_book_id.txt");

            // Leer todo el contenido del archivo (asumiendo que solo hay un ID en el archivo)
            if (Files.exists(filePath)) {
                // Leer el contenido del archivo como un String
                String lastId = new String(Files.readAllBytes(filePath)).trim();

                // Verificar si el archivo está vacío
                if (lastId.isEmpty()) {
                    return 0;  // Si está vacío, devolver 0
                }

                // Convertir el último ID a Integer
                return Integer.parseInt(lastId);
            } else {
                System.err.println("File not found: " + filePath);
                return 0;  // Si el archivo no existe, devolver 0
            }
        } catch (IOException e) {
            System.err.println("Failed to read last book ID: " + e.getMessage());
            return 0;  // En caso de error, devolver 0
        } catch (NumberFormatException e) {
            System.err.println("Invalid number format in the file: " + e.getMessage());
            return 0;  // Si el formato es incorrecto, devolver 0
        }
    }

    private static List<String> downloadBooksDistributed(HazelcastInstance hazelcastInstance, List<Integer> idsDeLibros) throws Exception {
        Set<Member> miembros = hazelcastInstance.getCluster().getMembers();
        List<Member> listaDeMiembros = new ArrayList<>(miembros);

        IExecutorService servicioEjecutor = hazelcastInstance.getExecutorService("ejecutorDescargadorDeLibros");

        List<Future<String>> futuros = new ArrayList<>();

        int totalLibros = idsDeLibros.size();
        int librosPorNodo = totalLibros / listaDeMiembros.size(); // Se divide el total entre todos los nodos

        // Dividir el trabajo entre todos los nodos (local y remotos)
        for (int i = 0; i < listaDeMiembros.size(); i++) {
            int inicioIdx = i * librosPorNodo;
            int finIdx = (i == listaDeMiembros.size() - 1) ? totalLibros : (i + 1) * librosPorNodo;

            List<Integer> subconjuntoDeLibros = idsDeLibros.subList(inicioIdx, finIdx);
            Member miembroDestino = listaDeMiembros.get(i);
            System.out.printf("Asignando libros %d a %d al nodo: %s%n", inicioIdx, finIdx - 1, miembroDestino);

            // Enviar la tarea de descarga a cada nodo (local o remoto)
            Future<String> futuro = servicioEjecutor.submitToMember(
                    new BookDownloadTask(subconjuntoDeLibros), miembroDestino);
            futuros.add(futuro);
        }

        List<String> resultados = new ArrayList<>();
        // Obtener los resultados de cada tarea
        for (Future<String> futuro : futuros) {
            try {
                resultados.add(futuro.get());  // Obtener y agregar los resultados de cada tarea
            } catch (Exception e) {
                resultados.add("Ocurrió un error durante la descarga del libro.");
                e.printStackTrace();  // Imprimir detalles del error
            }
        }

        // Asegurarse de apagar el servicio de Hazelcast después de obtener los resultados
        hazelcastInstance.shutdown();
        return resultados;
    }

    // Método para esperar hasta que el clúster tenga un tamaño específico
    private static void waitForClusterSize(HazelcastInstance hazelcastInstance, int clusterSize) throws InterruptedException {
        while (hazelcastInstance.getCluster().getMembers().size() < clusterSize) {
            Thread.sleep(1000);
        }
    }

    public static class BookDownloadTask implements Callable<String> {
        private final List<Integer> bookIds;
        private final BookDownloader downloader;
        private final BookProcessor bookProcessor;
        //private final MetadataManager metadataManager;

        public BookDownloadTask(List<Integer> bookIds) {
            this.bookIds = bookIds;
            this.downloader = new GutenbergBookDownloader("datalake");
            this.bookProcessor = new GutenbergBookProcessor();
            //this.metadataManager = new MetadataManager();
        }

        @Override
        public String call() {
            StringBuilder result = new StringBuilder();

            // Descargar y procesar cada libro
            for (Integer bookId : bookIds) {
                try {
                    downloader.downloadBook(bookId);
                    //metadataManager.processBookMetada(bookId);
                    bookProcessor.processBook(bookId);
                    result.append("Downloaded and processed book with ID: ").append(bookId).append("\n");
                } catch (IOException e) {
                    result.append("Failed to download book with ID: ").append(bookId).append("\n");
                }
            }
            return result.toString();
        }
    }
}
