package com.example;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class DistributedMetadataSynchronizer {

    public static void main(String[] args) throws IOException {
        // Inicia una instancia de Hazelcast
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();

        // Crea un mapa distribuido llamado "metadata"
        IMap<Integer, Map<String, String>> metadataMap = hazelcastInstance.getMap("metadata");

        String datamartDir = "./datamart/metadatos_libros.csv";

        // 1. Lee los metadatos desde el archivo CSV local y los sube al mapa distribuido
        readMetadataFromCSV(metadataMap, datamartDir);

        // 2. Verifica que los metadatos están en el mapa distribuido
        System.out.println("Metadatos en el mapa distribuido:");
        metadataMap.forEach((id, metadata) -> {
            System.out.println("ID: " + id + ", Metadatos: " + metadata);
        });

        // 3. Recorre el mapa de metadatos y asegura que cada metadato esté en el archivo local
        syncMetadataToLocal(metadataMap, datamartDir);

        // 4. Mantiene la aplicación ejecutándose para que el nodo siga activo
        hazelcastInstance.getLifecycleService().addLifecycleListener(event -> System.out.println("Hazelcast event: " + event));
    }

    // Método para leer los metadatos del archivo CSV y almacenarlos en el mapa distribuido
    private static void readMetadataFromCSV(IMap<Integer, Map<String, String>> metadataMap, String datamartDir) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(datamartDir))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Salta la cabecera
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

                metadataMap.put(bookId, metadata);  // Subir los metadatos al mapa distribuido
            }
        }
    }

    // Método para asegurar que los metadatos en el mapa distribuido estén en el archivo local
    private static void syncMetadataToLocal(IMap<Integer, Map<String, String>> metadataMap, String datamartDir) {
        Set<Integer> existingIds = getExistingMetadataIds(datamartDir);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(datamartDir, true))) {
            // Recorre el mapa distribuido y escribe los metadatos que no estén en el archivo local
            metadataMap.forEach((id, metadata) -> {
                if (!existingIds.contains(id)) {
                    try {
                        String csvLine = id + "," +
                                escapeForCSV(metadata.get("Title")) + "," +
                                escapeForCSV(metadata.get("Author")) + "," +
                                escapeForCSV(metadata.get("Language"));
                        writer.write(csvLine);
                        writer.newLine();
                        System.out.println("Metadatos guardados localmente: ID " + id);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para obtener los IDs ya presentes en el archivo CSV
    private static Set<Integer> getExistingMetadataIds(String datamartDir) {
        Set<Integer> existingIds = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(datamartDir))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Salta la cabecera
                if (line.startsWith("ID")) continue;

                String[] columns = line.split(",", -1);
                existingIds.add(Integer.parseInt(columns[0].trim()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return existingIds;
    }

    // Método para escapar valores para el formato CSV
    private static String escapeForCSV(String value) {
        if (value == null) {
            return "";
        }
        // Eliminar las comillas dobles (evitar escape)
        return value.replace("\"", "");
    }
}
