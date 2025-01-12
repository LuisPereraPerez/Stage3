package com.example;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

public class DistributedBookSynchronizer {

    public static void main(String[] args) {
        // Inicia una instancia de Hazelcast
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();

        // Crea un mapa distribuido llamado "books"
        IMap<Integer, String> booksMap = hazelcastInstance.getMap("books");

        // Directorio local donde están los libros descargados
        String datalakeDir = "./datalake";

        // 1. Lee los libros desde el directorio local y los sube al mapa distribuido
        try (Stream<Path> paths = Files.walk(Path.of(datalakeDir))) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            int bookId = Integer.parseInt(path.getFileName().toString().replace(".txt", ""));
                            String content = Files.readString(path);
                            booksMap.put(bookId, content);  // Subir el libro al mapa distribuido
                            System.out.println("Libro sincronizado: ID " + bookId);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 2. Verifica que los libros están en el mapa distribuido
        System.out.println("Libros en el mapa distribuido:");
        booksMap.forEach((id, content) -> {
            // Solo muestra una parte del contenido para evitar volúmenes grandes
            System.out.println("ID: " + id + ", Contenido: " + content.substring(0, Math.min(content.length(), 100)) + "...");
        });

        // 3. Recorre el mapa de libros y asegura que cada libro esté en el datalake local
        booksMap.forEach((bookId, content) -> {
            Path bookPath = Path.of(datalakeDir, bookId + ".txt");

            // Si el libro no existe en el datalake local, lo descargamos
            if (Files.notExists(bookPath)) {
                try {
                    // Guardamos el libro en el datalake local
                    Files.writeString(bookPath, content, StandardOpenOption.CREATE);
                    System.out.println("Libro descargado y guardado en el datalake local: ID " + bookId);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // También subimos el libro al mapa distribuido si no está ya presente
            if (!booksMap.containsKey(bookId)) {
                booksMap.put(bookId, content);
                System.out.println("Libro sincronizado al mapa distribuido: ID " + bookId);
            }
        });

        // 4. Mantiene la aplicación ejecutándose para que el nodo siga activo
        hazelcastInstance.getLifecycleService().addLifecycleListener(event -> System.out.println("Hazelcast event: " + event));
    }
}
