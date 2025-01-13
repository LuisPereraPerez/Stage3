package com.example.control;

import com.example.interfaces.WordCleaner;

import java.io.Serializable;
import java.text.Normalizer;

public class WordCleanerControl implements WordCleaner, Serializable {

    public WordCleanerControl() {
    }

    @Override
    public String cleanWord(String word) {
        // Eliminar caracteres especiales y quedarnos con la parte antes de ellos
        word = word.replaceAll("['’].*$", ""); // Esto elimina cualquier cosa después de ' o ’
        word = word.replaceAll("[^\\p{L}]", ""); // Mantener solo letras

        // Normalizar y eliminar caracteres no ASCII
        word = Normalizer.normalize(word, Normalizer.Form.NFD);
        word = word.replaceAll("[^\\p{ASCII}]", "");

        return word.trim();
    }
}

