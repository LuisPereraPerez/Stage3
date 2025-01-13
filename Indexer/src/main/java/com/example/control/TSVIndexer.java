package com.example.control;

import com.example.interfaces.BookIndexer;
import com.example.interfaces.TSVFileManager;
import com.example.interfaces.WordCleaner;
import com.example.interfaces.WordLemmatizer;

import java.io.IOException;


import java.nio.file.*;

public class TSVIndexer implements BookIndexer {

    private final ProcessedBooksManager processedBooksManager;
    private final TSVFileManager tsvFileManager;
    private final WordCleaner wordCleaner;
    private final WordLemmatizer wordLemmatizer;

    public TSVIndexer(ProcessedBooksManager processedBooksManager) {
        this.processedBooksManager = processedBooksManager;
        this.tsvFileManager = new TSVFileManagerControl();
        this.wordCleaner = new WordCleanerControl();
        this.wordLemmatizer = new WordLemmatizerControl();
    }

    public void indexBook(String filePath) {
        String bookId = extractBookId(filePath); // Extrae el ID del archivo
        if (processedBooksManager.isProcessed(bookId)) {
            System.out.println("Book already processed: " + bookId);
            return;
        }

        try {
            String bookContent = new String(Files.readAllBytes(Paths.get(filePath))); // Lee el contenido
            processContent(bookId, bookContent); // Procesa el contenido
            processedBooksManager.markAsProcessed(bookId); // Marca como procesado
            System.out.println("Book indexed successfully: " + bookId);
        } catch (IOException e) {
            System.err.println("Error processing book: " + bookId + ". " + e.getMessage());
        }
    }

    private void processContent(String bookId, String bookContent) throws IOException {
        String[] lines = bookContent.split("\n");
        for (int lineNumber = 0; lineNumber < lines.length; lineNumber++) {
            String[] words = lines[lineNumber].split("\\s+");
            for (String word : words) {
                String cleanedWord = wordCleaner.cleanWord(word);
                String lemma = wordLemmatizer.lemmatize(cleanedWord);

                if (!lemma.isEmpty()) {
                    tsvFileManager.saveWordsToFile(lemma, bookId, lineNumber + 1, 1);
                }
            }
        }
    }

    private String extractBookId(String filePath) {
        return Paths.get(filePath).getFileName().toString().replace(".txt", "");
    }
}