package com.example.control;

import com.example.interfaces.BookProcessor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GutenbergBookProcessor implements BookProcessor {
    private static final String PROCESSED_DIR = "datamart/processed_books"; // Ruta para los libros procesados

    @Override
    public void processBook(int bookId) {
        try {
            Path bookPath = Paths.get("datalake", bookId + ".txt"); // Ubicación del libro crudo
            String text = new String(Files.readAllBytes(bookPath), "UTF-8");

            Pattern startPattern = Pattern.compile("\\*\\*\\* START OF THE PROJECT GUTENBERG EBOOK .+? \\*\\*\\*");
            Pattern endPattern = Pattern.compile("\\*\\*\\* END OF THE PROJECT GUTENBERG EBOOK .+? \\*\\*\\*");

            Matcher startMatcher = startPattern.matcher(text);
            Matcher endMatcher = endPattern.matcher(text);

            if (startMatcher.find() && endMatcher.find()) {
                String rawContent = text.substring(startMatcher.end(), endMatcher.start()).trim();
                String[] lines = rawContent.split("\n");
                List<String> paragraphs = new ArrayList<>();
                StringBuilder currentParagraph = new StringBuilder();

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

                Path processedBookPath = Paths.get(PROCESSED_DIR, bookId + "_procesado.txt");
                Files.write(processedBookPath, finalContent.getBytes("UTF-8"));

                System.out.println("El libro procesado con ID " + bookId + " ha sido guardado en: " + processedBookPath);
            } else {
                System.out.println("No se encontraron las marcas de inicio o fin para el libro con ID " + bookId + ". El procesamiento se detiene para este libro.");
            }
        } catch (IOException e) {
            System.out.println("Error al procesar el libro con ID " + bookId + ": " + e.getMessage());
        }
    }
}
