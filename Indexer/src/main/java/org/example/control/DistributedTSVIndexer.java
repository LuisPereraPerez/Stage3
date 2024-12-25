package org.example.control;

import org.example.interfaces.WordCleaner;
import org.example.interfaces.WordLemmatizer;
import org.example.interfaces.*;

import java.io.IOException;

import java.util.Map;


import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.collection.ISet;
import org.example.interfaces.TSVFileManager;

import java.io.Serializable;


public class DistributedTSVIndexer implements Runnable, Serializable {
    private final HazelcastInstance hazelcastInstance;
    private final IMap<Integer, String> processedBooks;
    private final ISet<Integer> indexedBooks;
    private final IMap<String, String> wordIndex;
    private final TSVFileManager tsvFileManager;
    private final WordCleaner wordCleaner;
    private final WordLemmatizer wordLemmatizer;

    public DistributedTSVIndexer(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
        this.processedBooks = hazelcastInstance.getMap("processedBooks");
        this.indexedBooks = hazelcastInstance.getSet("indexedBooks");
        this.wordIndex = hazelcastInstance.getMap("wordIndex");
        this.tsvFileManager = new TSVFileManagerControl();
        this.wordCleaner = new WordCleanerControl();
        this.wordLemmatizer = new WordLemmatizerControl();
    }

    @Override
    public void run() {
        for (Map.Entry<Integer, String> entry : processedBooks.entrySet()) {
            int bookId = entry.getKey();
            String bookContent = entry.getValue();

            if (!indexedBooks.contains(bookId)) {
                try {
                    indexBook(bookId, bookContent);
                    indexedBooks.add(bookId); // Marcar libro como procesado
                } catch (IOException e) {
                    System.err.println("Error indexing book ID: " + bookId);
                    e.printStackTrace();
                }
            }
        }
    }

    public void indexBook(int bookId, String bookContent) throws IOException {
        String[] lines = bookContent.split("\n");
        Map<String, Map<Integer, Integer>> wordOccurrences = new java.util.HashMap<>();

        for (int lineNumber = 0; lineNumber < lines.length; lineNumber++) {
            String line = lines[lineNumber];
            String[] words = line.split("\\s+");

            for (String word : words) {
                String cleanedWord = wordCleaner.cleanWord(word);
                String lemma = wordLemmatizer.lemmatize(cleanedWord);

                if (!lemma.isEmpty()) {
                    wordOccurrences
                            .computeIfAbsent(lemma, k -> new java.util.HashMap<>())
                            .merge(lineNumber + 1, 1, Integer::sum);
                }
            }
        }

        // Guardar palabras en TSV y en Hazelcast
        for (Map.Entry<String, Map<Integer, Integer>> entry : wordOccurrences.entrySet()) {
            String word = entry.getKey();
            Map<Integer, Integer> occurrences = entry.getValue();

            StringBuilder tsvContent = new StringBuilder();
            for (Map.Entry<Integer, Integer> occurrenceEntry : occurrences.entrySet()) {
                int line = occurrenceEntry.getKey();
                int count = occurrenceEntry.getValue();
                tsvFileManager.saveWordsToFile(word, String.valueOf(bookId), line, count);

                // Crear contenido TSV para subir a Hazelcast
                tsvContent.append(bookId).append("\t").append(line).append("\t").append(count).append("\n");
            }

            // Subir palabra indexada al mapa compartido
            wordIndex.put(word, tsvContent.toString());
        }
    }
}
