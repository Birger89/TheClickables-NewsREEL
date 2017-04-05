package de.dailab.plistacontest.client;

import java.util.List;

/**
 * Created by kenneth on 25.03.17.
 * Stolen from https://gist.github.com/guenodz/d5add59b31114a3a3c66
 */
public class TfIdfCalculator {

    public TfIdfCalculator(){}

    public double tf(List<String> document, String term){
        double n = 0;
        for (String word : document) {
            n += term.equalsIgnoreCase(word) ? 1:0;
        }
        return n / document.size();
    }

    public double idf(List<List<String>> documents, String term) {
        double n = 0;
        for (List<String> document : documents) {
            for (String word : document) {
                if (term.equalsIgnoreCase(word)) {
                    n++;
                    break;
                }
            }
        }
        return Math.log(documents.size() / n);
    }

    public double tfidf(List<String> document,List<List<String>> documents, String term) {
        return tf(document, term) / idf(documents, term);
    }
}
