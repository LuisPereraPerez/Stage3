package com.example.control;

import com.example.interfaces.TSVFileManager;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TSVFileManagerControl implements TSVFileManager {

    @Override
    public List<String> readLines(String bookFilePath) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(bookFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + bookFilePath + ". " + e.getMessage());
        }
        return lines;
    }

    @Override
    public void saveWordsToFile(String word, String bookId, int paragraphIndex, int count) {
        // Extraer solo el ID del libro desde bookId
        String[] pathParts = bookId.split("[/\\\\]"); // Divide por / o \ según el sistema operativo
        String bookIdOnly = pathParts[pathParts.length - 1].replace(".txt", ""); // Obtiene el nombre sin extensión

        // Crear la ruta de directorio usando las primeras dos letras de la palabra
        String subfolder = word.length() > 1 ? word.substring(0, 2).toLowerCase() : word.substring(0, 1).toLowerCase();
        String directoryPath = "datamart/reverse_indexes/" + subfolder.charAt(0) + "/" + subfolder;

        File dir = new File(directoryPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String filePath = directoryPath + "/" + word.toLowerCase() + ".tsv";
        File file = new File(filePath);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            // Si el archivo no existe, escribir la cabecera antes de cualquier dato
            if (file.length() == 0) {
                writer.write("Book_ID\tLine\tOccurrences");
                writer.newLine();
            }

            // Crear la línea a añadir con el formato requerido
            String lineToAdd = bookIdOnly + "\t" + paragraphIndex + "\t" + count;

            // Escribir la línea de datos
            writer.write(lineToAdd);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error writing to TSV file: " + filePath + ". " + e.getMessage());
        }
    }
}
