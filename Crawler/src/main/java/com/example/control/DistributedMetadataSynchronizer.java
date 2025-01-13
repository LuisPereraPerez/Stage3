package com.example.control;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DistributedMetadataSynchronizer {

    public static void synchronizeMetadata(HazelcastInstance hazelcastInstance) {
        // Creates a distributed map called “metadata"
        System.out.println("WEBO");
        IMap<Integer, Map<String, String>> metadataMap = hazelcastInstance.getMap("metadata");

        String datamartDir = "./datamart/book_metadata.csv";

        try {
            // 1. Read metadata from the CSV file and synchronize to the distributed map.
            int metadataRead = readMetadataFromCSV(metadataMap, datamartDir);
            System.out.printf("[Step 1] Metadata read from the CSV and synchronized to the distributed map: %d records%n", metadataRead);

            // 2. Ensure that the distributed map metadata is in the local file.
            int metadataWritten = syncMetadataToLocal(metadataMap, datamartDir);
            System.out.printf("[Step 2] Metadata retrieved from the distributed map and stored locally: %d records%n", metadataWritten);

            // Keeps the application running so that the node remains active.
            hazelcastInstance.getLifecycleService().addLifecycleListener(event -> System.out.println("Hazelcast event: " + event));

        } catch (IOException e) {
            System.err.println("Error while synchronizing metadata: " + e.getMessage());
        }
    }

    // Method to read the metadata from the CSV file and store it in the distributed map
    private static int readMetadataFromCSV(IMap<Integer, Map<String, String>> metadataMap, String datamartDir) throws IOException {
        int count = 0;
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(datamartDir))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip the header
                if (line.startsWith("ID")) {
                    continue;
                }

                String[] columns = line.split(",", -1);
                int bookId = Integer.parseInt(columns[0]);  // ID
                Map<String, String> metadata = new HashMap<>();
                metadata.put("ID", columns[0]);
                metadata.put("Title", columns[1]);
                metadata.put("Author", columns[2]);
                metadata.put("Language", columns[3]);

                metadataMap.put(bookId, metadata);  // Upload metadata to the distributed map
                count++;
            }
        }
        return count; // Returns the number of records processed
    }

    // Method to ensure that the metadata in the distributed map is in the local file
    private static int syncMetadataToLocal(IMap<Integer, Map<String, String>> metadataMap, String datamartDir) {
        Set<Integer> existingIds = getExistingMetadataIds(datamartDir);
        int count = 0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(datamartDir, true))) {
            // Traverses the distributed map and writes the metadata that is not in the local file
            for (Map.Entry<Integer, Map<String, String>> entry : metadataMap.entrySet()) {
                int id = entry.getKey();
                Map<String, String> metadata = entry.getValue();

                if (!existingIds.contains(id)) {
                    try {
                        String csvLine = id + "," +
                                escapeForCSV(metadata.get("Title")) + "," +
                                escapeForCSV(metadata.get("Author")) + "," +
                                escapeForCSV(metadata.get("Language"));
                        writer.write(csvLine);
                        writer.newLine();
                        count++; // Increment the counter only for new records
                    } catch (IOException e) {
                        System.err.printf("Error saving metadata locally: ID %d, Error: %s%n", id, e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing metadata to CSV file: " + e.getMessage());
        }
        return count; // Returns the number of locally written records
    }

    // Method to obtain the IDs already in the CSV file
    private static Set<Integer> getExistingMetadataIds(String datamartDir) {
        Set<Integer> existingIds = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(datamartDir))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip the header
                if (line.startsWith("ID")) continue;

                String[] columns = line.split(",", -1);
                existingIds.add(Integer.parseInt(columns[0].trim()));
            }
        } catch (IOException e) {
            System.err.println("Error reading existing IDs in CSV file: " + e.getMessage());
        }
        return existingIds;
    }

    // Method to escape values for the CSV format
    private static String escapeForCSV(String value) {
        if (value == null) {
            return "";
        }
        // Remove double quotation marks (avoid escape)
        return value.replace("\"", "");
    }
}
