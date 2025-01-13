package com.example.control;

import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.ling.*;
import java.util.*;

public class Lemmatizer {
    private final StanfordCoreNLP pipeline;

    public Lemmatizer() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        this.pipeline = new StanfordCoreNLP(props);
    }

    public String lemmatize(String word) {
        Annotation annotation = new Annotation(word);
        pipeline.annotate(annotation);
        List<CoreLabel> tokens = annotation.get(CoreAnnotations.TokensAnnotation.class);

        // Returns the first lemmatized word (in case of phrases, it only processes the first word)
        return tokens.stream()
                .map(token -> token.get(CoreAnnotations.LemmaAnnotation.class))
                .findFirst()
                .orElse(word); // If no lemmatization is found, it returns the original word.
    }
}
