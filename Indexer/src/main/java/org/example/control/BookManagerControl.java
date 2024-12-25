package org.example.control;

import org.example.interfaces.LastBookManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class BookManagerControl implements LastBookManager, Serializable {

    public BookManagerControl() {
    }

    @Override
    public int readLastProcessedBookId(String filePath) throws IOException {
        File file = new File(filePath);

        // Check if the parent directory exists, and create it if it does not
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // If the file does not exist or is empty, initialize it with 0
        if (!file.exists() || file.length() == 0) {
            updateLastProcessedBookId(filePath, 0);
            return 0;
        }

        // Read the first line from the file to get the last processed book ID
        List<String> lines = readLines(filePath);
        try {
            return Integer.parseInt(lines.get(0).trim());
        } catch (NumberFormatException e) {
            System.out.println("The file content is not a valid integer.");
            return 0; // Default value in case of a format error
        }
    }

    @Override
    public void updateLastProcessedBookId(String filePath, int lastBookId) throws IOException {
        // Write the new last processed book ID to the file
        writeLines(filePath, List.of(Integer.toString(lastBookId)));
    }

    // Helper method to read all lines from a file
    private List<String> readLines(String filePath) throws IOException {
        try {
            return Files.readAllLines(Paths.get(filePath));
        } catch (IOException e) {
            System.out.println("The file '" + filePath + "' does not exist.");
            throw e;
        }
    }

    // Helper method to write lines to a file
    private void writeLines(String filePath, List<String> lines) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            for (String line : lines) {
                writer.write(line + System.lineSeparator());
            }
        }
    }
}
