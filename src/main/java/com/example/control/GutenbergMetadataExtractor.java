package com.example.control;

import com.example.interfaces.MetadataExtractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GutenbergMetadataExtractor implements MetadataExtractor {
    private static final String SAVE_DIR = "datalake";

    @Override
    public Map<String, String> extractMetadata(int bookId) {
        Map<String, String> metadata = new HashMap<>();
        File bookFile = new File(SAVE_DIR + "/" + bookId + ".txt");

        try {
            String text = new String(Files.readAllBytes(Paths.get(bookFile.toURI())), "UTF-8");
            metadata.put("ID", String.valueOf(bookId));
            metadata.put("Title", extract("Title: (.+)", text));
            metadata.put("Author", extract("Author: (.+)", text));
            metadata.put("Release Date", extract("Release Date: (.+)", text));
            metadata.put("Most Recently Updated", extract("Most recently updated: (.+)", text));
            metadata.put("Language", extract("Language: (.+)", text));
            return metadata;
        } catch (IOException e) {
            System.out.println("Error al leer el libro con ID " + bookId + ": " + e.getMessage());
            return null;
        }
    }

    private String extract(String regex, String text) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : "Unknown";
    }
}