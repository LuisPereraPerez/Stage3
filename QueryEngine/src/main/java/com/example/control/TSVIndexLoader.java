package com.example.control;

import java.io.*;
import java.util.*;

public class TSVIndexLoader {
    public Map<String, Map<String, List<String>>> loadIndex(String baseDir) throws IOException {
        Map<String, Map<String, List<String>>> index = new HashMap<>();
        File baseFolder = new File(baseDir);

        if (!baseFolder.exists() || !baseFolder.isDirectory()) {
            throw new IOException("The base directory does not exist or is invalid: " + baseDir);
        }

        for (File firstLetter : Optional.ofNullable(baseFolder.listFiles(File::isDirectory)).orElse(new File[0])) {
            for (File secondLetter : Optional.ofNullable(firstLetter.listFiles(File::isDirectory)).orElse(new File[0])) {
                for (File file : Optional.ofNullable(secondLetter.listFiles((dir, name) -> name.endsWith(".tsv"))).orElse(new File[0])) {
                    String word = file.getName().replace(".tsv", "");
                    Map<String, List<String>> wordData = new HashMap<>();
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        reader.readLine(); // Skip header
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] parts = line.split("\t");
                            if (parts.length >= 3) {
                                String bookId = parts[0];
                                String lineNumber = parts[1];
                                String occurrences = parts[2];
                                wordData.computeIfAbsent(bookId, k -> new ArrayList<>())
                                        .add("Line: " + lineNumber + ", Occurrences: " + occurrences);
                            }
                        }
                    }
                    index.put(word, wordData);
                }
            }
        }
        return index;
    }
}
