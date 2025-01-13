package com.example.control;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import com.example.interfaces.WordLemmatizer;

import java.io.Serializable;
import java.util.Properties;

public class WordLemmatizerControl implements WordLemmatizer, Serializable {

    private transient StanfordCoreNLP pipeline; // Transient para evitar serialización

    public WordLemmatizerControl() {
        initializePipeline(); // Inicializar el pipeline al crear la instancia
    }

    private void initializePipeline() {
        if (pipeline == null) {
            Properties props = new Properties();
            props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
            props.setProperty("tokenize.language", "en");
            pipeline = new StanfordCoreNLP(props);
        }
    }

    @Override
    public String lemmatize(String word) {
        initializePipeline(); // Asegurar que el pipeline esté inicializado
        word = word.toLowerCase();
        CoreDocument document = new CoreDocument(word);
        pipeline.annotate(document);
        for (CoreLabel token : document.tokens()) {
            return token.get(CoreAnnotations.LemmaAnnotation.class);
        }
        return word;
    }
}
