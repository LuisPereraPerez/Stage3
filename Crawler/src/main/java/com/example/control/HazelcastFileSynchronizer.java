package com.example.control;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.io.*;
import java.nio.file.*;
import java.util.Map;

public class HazelcastFileSynchronizer {

    private final HazelcastInstance hazelcastInstance;
    private static final String PROCESSED_BOOKS_DIR = "datamart/processed_books";
    private static final String METADATA_CSV_FILE = "datamart/metadata.csv";

    public HazelcastFileSynchronizer(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    // Sincroniza los libros procesados entre Hazelcast y el sistema de archivos
    public void synchronizeProcessedBooks() {
        IMap<Integer, String> processedBooksMap = hazelcastInstance.getMap("processedBooks");
        File processedBooksDir = new File(PROCESSED_BOOKS_DIR);

        if (!processedBooksDir.exists()) {
            processedBooksDir.mkdirs();  // Aseguramos que el directorio exista
        }

        // Sincronización: De Hazelcast al sistema de archivos
        for (Map.Entry<Integer, String> entry : processedBooksMap.entrySet()) {
            Integer bookId = entry.getKey();
            String processedContent = entry.getValue();

            File processedFile = new File(PROCESSED_BOOKS_DIR, bookId + "_procesado.txt");
            if (!processedFile.exists()) {
                try {
                    Files.write(processedFile.toPath(), processedContent.getBytes("UTF-8"));
                    System.out.println("Libro " + bookId + " guardado en disco.");
                } catch (IOException e) {
                    System.out.println("Error al guardar el libro " + bookId + " en disco: " + e.getMessage());
                }
            }
        }

        // Sincronización: Del sistema de archivos a Hazelcast
        for (File file : processedBooksDir.listFiles()) {
            if (file.isFile() && file.getName().endsWith("_procesado.txt")) {
                Integer bookId = Integer.parseInt(file.getName().replace("_procesado.txt", ""));
                if (!processedBooksMap.containsKey(bookId)) {
                    try {
                        String content = new String(Files.readAllBytes(file.toPath()), "UTF-8");
                        processedBooksMap.put(bookId, content);
                        System.out.println("Libro " + bookId + " cargado en Hazelcast.");
                    } catch (IOException e) {
                        System.out.println("Error al leer el libro " + bookId + " desde el disco: " + e.getMessage());
                    }
                }
            }
        }
    }

    // Sincroniza los metadatos entre Hazelcast y el archivo CSV
    public void synchronizeMetadata() throws IOException {
        IMap<Integer, Map<String, String>> metadataMap = hazelcastInstance.getMap("metadataMap");
        File metadataFile = new File(METADATA_CSV_FILE);

        // Sincronización: De Hazelcast al archivo CSV
        synchronized (this) {
            for (Map.Entry<Integer, Map<String, String>> entry : metadataMap.entrySet()) {
                Integer bookId = entry.getKey();
                Map<String, String> metadata = entry.getValue();
                String csvLine = createCsvLine(metadata);

                // Si el archivo CSV no contiene la línea del libro, la añadimos
                if (!isDuplicate(metadataFile, csvLine)) {
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(metadataFile, true))) {
                        writer.write(csvLine);
                        writer.newLine();
                        System.out.println("Metadatos para el libro " + bookId + " guardados en CSV.");
                    } catch (IOException e) {
                        System.out.println("Error al guardar los metadatos para el libro " + bookId + " en CSV: " + e.getMessage());
                    }
                }
            }
        }

        // Sincronización: Del archivo CSV a Hazelcast
        if (metadataFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(metadataFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("ID")) continue; // Salta la cabecera

                    String[] fields = line.split(",");
                    Integer bookId = Integer.parseInt(fields[0]);
                    if (!metadataMap.containsKey(bookId)) {
                        Map<String, String> metadata = Map.of(
                                "ID", fields[0],
                                "Title", fields[1],
                                "Author", fields[2],
                                "Release Date", fields[3],
                                "Most Recently Updated", fields[4],
                                "Language", fields[5]
                        );
                        metadataMap.put(bookId, metadata);
                        System.out.println("Metadatos para el libro " + bookId + " cargados en Hazelcast.");
                    }
                }
            } catch (IOException e) {
                System.out.println("Error al leer el archivo de metadatos: " + e.getMessage());
            }
        }
    }

    // Genera una línea CSV desde los metadatos
    private String createCsvLine(Map<String, String> metadata) {
        return String.join(",",
                metadata.getOrDefault("ID", ""),
                escapeForCSV(metadata.getOrDefault("Title", "")),
                escapeForCSV(metadata.getOrDefault("Author", "")),
                escapeForCSV(metadata.getOrDefault("Release Date", "")),
                escapeForCSV(metadata.getOrDefault("Most Recently Updated", "")),
                escapeForCSV(metadata.getOrDefault("Language", ""))
        );
    }

    // Escapa los valores para CSV
    private String escapeForCSV(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    // Verifica si una línea ya está presente en el archivo CSV
    private boolean isDuplicate(File file, String csvLine) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals(csvLine)) {
                    return true;
                }
            }
        }
        return false;
    }
}
