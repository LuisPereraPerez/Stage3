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
    private static final String PROCESSED_DIR = "datalake"; // Path for processed books
    private static final String CSV_FILE_PATH = "datamart/book_metadata.csv"; // Path for the CSV file
    private static final String HEADER = "ID,Title,Author,Language";

    // Patterns to extract metadata (adjust them according to the book format)
    private static final Pattern titlePattern = Pattern.compile("Title: (.+)");
    private static final Pattern authorPattern = Pattern.compile("Author: (.+)");
    private static final Pattern languagePattern = Pattern.compile("Language: (.+)");

    @Override
    public void processBook(int bookId) {
        try {
            Path bookPath = Paths.get("datalake", bookId + ".txt"); // Location of the raw book
            String text = new String(Files.readAllBytes(bookPath), "UTF-8");

            // Extract metadata
            Map<String, String> metadata = extractMetadata(bookId);

            // If metadata is successfully extracted, print and save it in the CSV
            if (metadata != null) {
                // Save metadata in the CSV file
                writeMetadata(metadata);

                // Clean and sort the metadata file after saving
                cleanAndSortMetadata();
            }

            // Search for Gutenberg delimiters to extract only the book content
            Pattern startPattern = Pattern.compile("\\*\\*\\* START OF THE PROJECT GUTENBERG EBOOK .+? \\*\\*\\*");
            Pattern endPattern = Pattern.compile("\\*\\*\\* END OF THE PROJECT GUTENBERG EBOOK .+? \\*\\*\\*");

            Matcher startMatcher = startPattern.matcher(text);
            Matcher endMatcher = endPattern.matcher(text);

            if (startMatcher.find() && endMatcher.find()) {
                String rawContent = text.substring(startMatcher.end(), endMatcher.start()).trim();
                String[] lines = rawContent.split("\n");
                List<String> paragraphs = new ArrayList<>();
                StringBuilder currentParagraph = new StringBuilder();

                // Process the book content
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

                // Save the processed book in the folder 'datamart/processed_books'
                File processedDir = new File(PROCESSED_DIR);
                if (!processedDir.exists()) {
                    processedDir.mkdirs(); // Create the directory if it does not exist
                }

                Path processedBookPath = Paths.get(PROCESSED_DIR, bookId + ".txt");
                Files.write(processedBookPath, finalContent.getBytes("UTF-8"));

                System.out.println("The processed book with ID " + bookId + " has been saved at: " + processedBookPath);
            }

        } catch (IOException e) {
            System.out.println("Error processing the book with ID " + bookId + ": " + e.getMessage());
        }
    }

    // Method to extract the book's metadata
    public Map<String, String> extractMetadata(int bookId) {
        Map<String, String> metadata = new HashMap<>();
        File bookFile = new File("datalake" + "/" + bookId + ".txt");

        try {
            String text = new String(Files.readAllBytes(Paths.get(bookFile.toURI())), "UTF-8");

            // Extract metadata using regular expressions
            metadata.put("ID", String.valueOf(bookId));
            metadata.put("Title", extract(titlePattern, text));
            metadata.put("Author", extract(authorPattern, text));
            metadata.put("Language", extract(languagePattern, text));

            return metadata;
        } catch (IOException e) {
            System.out.println("Error reading the book with ID " + bookId + ": " + e.getMessage());
            return null;
        }
    }

    // Private method to extract a text field using a regular pattern
    private String extract(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : "Unknown";
    }

    // Method to escape values for CSV format
    private String escapeForCSV(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    // Method to write metadata into a CSV file
    public void writeMetadata(Map<String, String> metadata) {
        File file = new File(CSV_FILE_PATH);
        File directory = file.getParentFile();

        if (directory != null && !directory.exists()) {
            if (!directory.mkdirs()) {
                System.out.println("Failed to create the directory: " + directory.getAbsolutePath());
                return;
            }
        }

        boolean isNewFile = !file.exists();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            // Add the header only if the file is new
            if (isNewFile) {
                writer.write(HEADER);
                writer.newLine();
            }

            // Check if the ID is already in the file before writing
            String id = metadata.getOrDefault("ID", "");
            if (id.isEmpty()) {
                System.out.println("The book ID is missing, skipping.");
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
            } else {
                System.out.println("Duplicate entry found for ID: " + id);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to check if an ID is already present in the CSV file
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

    // Method to clean and sort the metadata file
    public void cleanAndSortMetadata() {
        try {
            Path filePath = Paths.get(CSV_FILE_PATH);

            // Read all records from the CSV file
            List<String> lines = Files.readAllLines(filePath);

            // Separate the header and the data
            String header = HEADER;
            List<String> records = new ArrayList<>();

            for (String line : lines) {
                if (!line.equals(header)) { // Skip duplicate headers
                    records.add(line);
                }
            }

            // Use a TreeMap to sort by ID and remove duplicates
            Map<Integer, String> sortedRecords = new TreeMap<>();

            for (String record : records) {
                String[] columns = record.split(",", -1);
                if (columns.length > 0) {
                    try {
                        int id = Integer.parseInt(columns[0].trim()); // Parse the ID
                        sortedRecords.put(id, record); // TreeMap handles duplicates automatically
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid record, cannot parse ID: " + record);
                    }
                }
            }

            // Rewrite the CSV file with the sorted records
            try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                writer.write(header);
                writer.newLine();
                for (String record : sortedRecords.values()) {
                    writer.write(record);
                    writer.newLine();
                }
            }

            System.out.println("Metadata cleaned and sorted successfully.");

        } catch (IOException e) {
            System.err.println("Error cleaning and sorting the metadata file: " + e.getMessage());
        }
    }

}
