package org.example.control;

import org.example.interfaces.WordLemmatizer;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.io.Serializable;
import java.util.Properties;

public class WordLemmatizerControl implements WordLemmatizer, Serializable {

    private final StanfordCoreNLP pipeline;

    public WordLemmatizerControl() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        props.setProperty("tokenize.language", "en");
        this.pipeline = new StanfordCoreNLP(props);
    }

    @Override
    public String lemmatize(String word) {
        word = word.toLowerCase();
        CoreDocument document = new CoreDocument(word);
        pipeline.annotate(document);
        for (CoreLabel token : document.tokens()) {
            return token.get(CoreAnnotations.LemmaAnnotation.class);
        }
        return word;
    }
}
