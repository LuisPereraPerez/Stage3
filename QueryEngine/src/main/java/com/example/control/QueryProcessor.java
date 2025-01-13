package com.example.control;

import java.util.*;

public class QueryProcessor {
    private final Map<String, Map<String, List<String>>> index;
    private final Map<String, Map<String, String>> metadata;
    private final Lemmatizer lemmatizer;

    public QueryProcessor(Map<String, Map<String, List<String>>> index, Map<String, Map<String, String>> metadata) {
        this.index = index;
        this.metadata = metadata;
        this.lemmatizer = new Lemmatizer(); // Initialize lemmatization
    }

    public Map<String, Object> processQuery(String query) {
        // Sanitize the word
        String sanitizedWord = sanitizeWord(query);

        // Lemmatize the word
        String lemma = lemmatizer.lemmatize(sanitizedWord);
        System.out.println("Lemmatized word: " + lemma);

        Map<String, Object> result = new HashMap<>();
        Map<String, List<String>> wordData = index.get(lemma);

        if (wordData == null) {
            result.put("message", "No results found for the word: " + query + " (Lemma: " + lemma + ")");
            return result;
        }

        List<Map<String, Object>> bookResults = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : wordData.entrySet()) {
            String bookId = entry.getKey();
            List<String> lines = entry.getValue();
            Map<String, Object> bookInfo = new HashMap<>(metadata.getOrDefault(bookId, new HashMap<>()));
            bookInfo.put("Lines", lines);
            bookResults.add(bookInfo);
        }

        result.put("query", query);
        result.put("results", bookResults);
        return result;
    }

    private String sanitizeWord(String word) {
        // Remove quotes at the beginning and end
        return word.replaceAll("^\"|\"$", "").trim();
    }
}
