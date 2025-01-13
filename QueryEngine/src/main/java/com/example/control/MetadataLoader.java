package com.example.control;

import java.io.*;
import java.util.*;

public class MetadataLoader {
    public Map<String, Map<String, String>> loadMetadata(String filePath) throws IOException {
        Map<String, Map<String, String>> metadata = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (parts.length >= 4) {
                    String id = parts[0].trim();
                    String title = parts[1].replace("\"", "").trim();
                    String author = parts[2].replace("\"", "").trim();
                    String language = parts[3].replace("\"", "").trim();

                    Map<String, String> bookMetadata = new HashMap<>();
                    bookMetadata.put("Title", title);
                    bookMetadata.put("Author", author);
                    bookMetadata.put("Language", language);

                    metadata.put(id, bookMetadata);
                }
            }
        }

        return metadata;
    }

    private String[] parseCSVLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char ch : line.toCharArray()) {
            if (ch == '"') {
                inQuotes = !inQuotes; // Toggle quotes state
            } else if (ch == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current.setLength(0); // Reset the StringBuilder
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim()); // Add the last value
        return values.toArray(new String[0]);
    }
}
