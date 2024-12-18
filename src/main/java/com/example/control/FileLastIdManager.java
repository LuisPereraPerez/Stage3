package com.example.control;

import com.example.interfaces.LastIdManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileLastIdManager implements LastIdManager {
    private static final String LAST_ID_FILE = "resources/last_id_crawler.txt";

    @Override
    public int getLastDownloadedId() {
        try {
            Path filePath = Paths.get(LAST_ID_FILE);
            File file = filePath.toFile();
            File directory = file.getParentFile(); // Obtener el directorio "resources"

            if (directory != null && !directory.exists()) {
                // Crear la carpeta si no existe
                if (!directory.mkdirs()) {
                    System.out.println("Could not create directory: " + directory.getAbsolutePath());
                    return 0;
                }
            }

            if (!file.exists()) {
                Files.createFile(filePath); // Crear el archivo si no existe
                System.out.println("ID file not found, created a new one in Crawler module. Starting from 0.");
                return 0;
            }

            String lastId = new String(Files.readAllBytes(filePath));
            return Integer.parseInt(lastId.trim());

        } catch (IOException e) {
            System.out.println("The ID file could not be read, starting from 0.");
        } catch (NumberFormatException e) {
            System.out.println("The ID file is empty or incorrectly formatted. Starting from 0.");
        }
        return 0;
    }

    @Override
    public void updateLastDownloadedId(int lastId) {
        try {
            Path filePath = Paths.get(LAST_ID_FILE);
            File file = filePath.toFile();
            File directory = file.getParentFile(); // Obtener el directorio "resources"

            if (directory != null && !directory.exists()) {
                // Crear la carpeta si no existe
                if (!directory.mkdirs()) {
                    System.out.println("Could not create directory: " + directory.getAbsolutePath());
                    return;
                }
            }

            Files.write(filePath, String.valueOf(lastId).getBytes());
        } catch (IOException e) {
            System.out.println("The ID file could not be updated.");
        }
    }
}
