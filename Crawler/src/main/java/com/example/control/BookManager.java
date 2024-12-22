package com.example.control;

import com.example.interfaces.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class BookManager {
    private final BookDownloader downloader;
    private final MetadataExtractor metadataExtractor;
    private final BookProcessor bookProcessor;
    private final MetadataWriter metadataWriter;
    private final String saveDir;

    public BookManager(BookDownloader downloader, MetadataExtractor metadataExtractor, BookProcessor bookProcessor, MetadataWriter metadataWriter, String saveDir) {
        this.downloader = downloader;
        this.metadataExtractor = metadataExtractor;
        this.bookProcessor = bookProcessor;
        this.metadataWriter = metadataWriter;
        this.saveDir = saveDir;
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
            }

            bookProcessor.processBook(bookId);
            System.out.println("Book processed and saved: " + bookId);
            return true;

        } catch (IOException e) {
            System.out.println("Error handling the book with ID: " + bookId + ". Error: " + e.getMessage());
            return false;
        }
    }
}