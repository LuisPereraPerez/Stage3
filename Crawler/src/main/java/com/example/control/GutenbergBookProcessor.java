package com.example.control;

import com.example.interfaces.BookProcessor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GutenbergBookProcessor implements BookProcessor {
    private static final String PROCESSED_DIR = "datalake"; // Ruta para los libros procesados
    private static final String CSV_FILE_PATH = "datamart/metadatos_libros.csv"; // Ruta para el archivo CSV
    private static final String HEADER = "ID,Title,Author,Language";

    // Patrones para extraer los metadatos (ajustarlos según el formato de los libros)
    private static final Pattern titlePattern = Pattern.compile("Title: (.+)");
    private static final Pattern authorPattern = Pattern.compile("Author: (.+)");
    private static final Pattern languagePattern = Pattern.compile("Language: (.+)");

    @Override
    public void processBook(int bookId) {
        try {
            Path bookPath = Paths.get("datalake", bookId + ".txt"); // Ubicación del libro crudo
            String text = new String(Files.readAllBytes(bookPath), "UTF-8");

            // Extraer metadatos
            Map<String, String> metadata = extractMetadata(bookId);

            // Si los metadatos fueron extraídos con éxito, imprimirlos y guardarlos en CSV
            if (metadata != null) {
                // Imprimir metadatos
                //System.out.println("Metadatos para el libro con ID " + bookId + ":");
                //for (Map.Entry<String, String> entry : metadata.entrySet()) {
                //    System.out.println(entry.getKey() + ": " + entry.getValue());
                //}

                // Guardar metadatos en el archivo CSV
                writeMetadata(metadata);
            }

            // Buscar los delimitadores de Gutenberg para extraer solo el contenido del libro
            Pattern startPattern = Pattern.compile("\\*\\*\\* START OF THE PROJECT GUTENBERG EBOOK .+? \\*\\*\\*");
            Pattern endPattern = Pattern.compile("\\*\\*\\* END OF THE PROJECT GUTENBERG EBOOK .+? \\*\\*\\*");

            Matcher startMatcher = startPattern.matcher(text);
            Matcher endMatcher = endPattern.matcher(text);

            if (startMatcher.find() && endMatcher.find()) {
                String rawContent = text.substring(startMatcher.end(), endMatcher.start()).trim();
                String[] lines = rawContent.split("\n");
                List<String> paragraphs = new ArrayList<>();
                StringBuilder currentParagraph = new StringBuilder();

                // Procesar el contenido del libro
                for (String line : lines) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        currentParagraph.append(line).append(" ");
                    } else if (currentParagraph.length() > 0) {
                        paragraphs.add(currentParagraph.toString().trim());
                        currentParagraph.setLength(0);
                    }
                }

                if (currentParagraph.length() > 0) {
                    paragraphs.add(currentParagraph.toString().trim());
                }

                String finalContent = String.join("\n\n", paragraphs);

                // Guardar el libro procesado en la carpeta 'datamart/libros_procesados'
                File processedDir = new File(PROCESSED_DIR);
                if (!processedDir.exists()) {
                    processedDir.mkdirs(); // Crear el directorio si no existe
                }

                Path processedBookPath = Paths.get(PROCESSED_DIR, bookId + ".txt");
                Files.write(processedBookPath, finalContent.getBytes("UTF-8"));

                System.out.println("El libro procesado con ID " + bookId + " ha sido guardado en: " + processedBookPath);
            }

        } catch (IOException e) {
            System.out.println("Error al procesar el libro con ID " + bookId + ": " + e.getMessage());
        }
    }

    // Método para extraer los metadatos del libro
    public Map<String, String> extractMetadata(int bookId) {
        Map<String, String> metadata = new HashMap<>();
        File bookFile = new File("datalake" + "/" + bookId + ".txt");

        try {
            String text = new String(Files.readAllBytes(Paths.get(bookFile.toURI())), "UTF-8");

            // Extraer los metadatos usando las expresiones regulares
            metadata.put("ID", String.valueOf(bookId));
            metadata.put("Title", extract(titlePattern, text));
            metadata.put("Author", extract(authorPattern, text));
            metadata.put("Language", extract(languagePattern, text));

            return metadata;
        } catch (IOException e) {
            System.out.println("Error al leer el libro con ID " + bookId + ": " + e.getMessage());
            return null;
        }
    }

    // Método privado para extraer un campo de texto usando un patrón regular
    private String extract(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : "Unknown";
    }

    // Método para escapar valores para el formato CSV
    private String escapeForCSV(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    // Método para escribir metadatos en un archivo CSV
    public void writeMetadata(Map<String, String> metadata) {
            File file = new File(CSV_FILE_PATH);
            File directory = file.getParentFile();

            if (directory != null && !directory.exists()) {
                if (!directory.mkdirs()) {
                    System.out.println("No se pudo crear el directorio: " + directory.getAbsolutePath());
                    return;
                }
            }

            boolean isNewFile = !file.exists();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                // Agregar la cabecera solo si el archivo es nuevo
                if (isNewFile) {
                    writer.write(HEADER);
                    writer.newLine();
                }

                // Verificar si el ID ya está en el archivo antes de escribir
                String id = metadata.getOrDefault("ID", "");
                if (id.isEmpty()) {
                    System.out.println("El ID del libro está ausente, se omite.");
                    return;
                }

                if (!isDuplicate(id)) {
                    String csvLine = String.join(",",
                            id,
                            escapeForCSV(metadata.getOrDefault("Title", "")),
                            escapeForCSV(metadata.getOrDefault("Author", "")),
                            escapeForCSV(metadata.getOrDefault("Language", "")));
                    writer.write(csvLine);
                    writer.newLine();
                    System.out.println("Metadatos guardados: " + csvLine);
                } else {
                    System.out.println("Entrada duplicada encontrada para el ID: " + id);
                }
            } catch (IOException e) {
                e.printStackTrace();
        }
    }

    // Método para verificar si un ID ya está presente en el archivo CSV
    private boolean isDuplicate(String id) throws IOException {
            try (BufferedReader reader = new BufferedReader(new FileReader(CSV_FILE_PATH))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] columns = line.split(",", -1);
                    if (columns.length > 0 && columns[0].trim().equals(id.trim())) {
                        return true;
                    }
                }
            }
            return false;
    }
}
