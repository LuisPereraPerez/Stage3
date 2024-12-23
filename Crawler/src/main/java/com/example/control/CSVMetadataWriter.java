package com.example.control;

import com.example.interfaces.MetadataWriter;

import java.io.*;
import java.util.Map;

public class CSVMetadataWriter implements MetadataWriter {
    private static final String METADATA_CSV_FILE = "datamart/metadata.csv";
    private static final String HEADER = "ID,Title,Author,Release Date,Most Recently Updated,Language";
    private final Object lock = new Object(); // Objeto de bloqueo

    // Método para escapar los valores para el CSV
    private String escapeForCSV(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    @Override
    public void writeMetadata(Map<String, String> metadata) {
        synchronized (lock) {
            File file = new File(METADATA_CSV_FILE);
            File directory = file.getParentFile(); // Obtener el directorio "datamart"

            // Crear la carpeta si no existe
            if (directory != null && !directory.exists()) {
                if (!directory.mkdirs()) {
                    System.out.println("Could not create directory: " + directory.getAbsolutePath());
                    return;
                }
            }

            boolean isNewFile = !file.exists();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                // Si el archivo es nuevo, escribe la cabecera
                if (isNewFile) {
                    writer.write(HEADER);
                    writer.newLine();
                }

                // Extrae el ID del metadato
                String id = metadata.getOrDefault("ID", "");
                if (id.isEmpty()) {
                    System.out.println("ID is missing for the book, skipping.");
                    return; // Si no hay ID, no continuamos
                }

                // Verificamos si el ID ya existe
                if (!isDuplicate(id)) {
                    // Construir la línea CSV
                    String csvLine = String.join(",",
                            id,
                            escapeForCSV(metadata.getOrDefault("Title", "")),
                            escapeForCSV(metadata.getOrDefault("Author", "")),
                            escapeForCSV(metadata.getOrDefault("Release Date", "")),
                            escapeForCSV(metadata.getOrDefault("Most Recently Updated", "")),
                            escapeForCSV(metadata.getOrDefault("Language", "")));

                    // Escribir la nueva línea en el archivo
                    writer.write(csvLine);
                    writer.newLine();
                } else {
                    System.out.println("Duplicate entry found for ID: " + id);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Método para verificar si el ID ya está en el archivo CSV
    private boolean isDuplicate(String id) throws IOException {
        synchronized (lock) {
            try (BufferedReader reader = new BufferedReader(new FileReader(METADATA_CSV_FILE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Si encontramos una línea con el mismo ID, retornamos true (es un duplicado)
                    String[] columns = line.split(",");
                    if (columns.length > 0 && columns[0].equals(id)) {
                        return true; // Duplicado encontrado
                    }
                }
            }
            return false; // No hay duplicado
        }
    }
}
