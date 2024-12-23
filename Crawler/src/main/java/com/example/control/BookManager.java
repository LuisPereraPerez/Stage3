package com.example.control;

import com.example.interfaces.*;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class BookManager {
    private final BookDownloader downloader;
    private final MetadataExtractor metadataExtractor;
    private final BookProcessor bookProcessor;
    private final MetadataWriter metadataWriter;
    private final String saveDir;
    private final HazelcastInstance hazelcastInstance;

    public BookManager(BookDownloader downloader, MetadataExtractor metadataExtractor,
                       BookProcessor bookProcessor, MetadataWriter metadataWriter,
                       String saveDir) {
        this.downloader = downloader;
        this.metadataExtractor = metadataExtractor;
        this.bookProcessor = bookProcessor;
        this.metadataWriter = metadataWriter;
        this.saveDir = saveDir;
        this.hazelcastInstance = HazelcastManager.getInstance();
    }

    public boolean handleBook(int bookId) {
        try {
            downloader.downloadBook(bookId);

            Path bookPath = Path.of(saveDir, bookId + ".txt");
            if (!bookPath.toFile().exists()) {
                throw new IOException("File not found after download: " + bookPath);
            }

            Map<String, String> metadata = metadataExtractor.extractMetadata(bookId);
            if (metadata != null) {
                metadataWriter.writeMetadata(metadata);

                // Almacenar metadatos en Hazelcast
                IMap<Integer, Map<String, String>> metadataMap = hazelcastInstance.getMap("metadataMap");
                metadataMap.put(bookId, metadata);
            }

            bookProcessor.processBook(bookId);

            // Almacenar libro procesado en Hazelcast
            Path processedPath = Path.of("datamart/processed_books", bookId + "_procesado.txt");
            String processedContent = Files.readString(processedPath);
            IMap<Integer, String> bookMap = hazelcastInstance.getMap("processedBooks");
            bookMap.put(bookId, processedContent);

            System.out.println("Book processed and saved to Hazelcast: " + bookId);
            return true;

        } catch (IOException e) {
            System.out.println("Error handling the book with ID: " + bookId + ". Error: " + e.getMessage());
            return false;
        }
    }
}
