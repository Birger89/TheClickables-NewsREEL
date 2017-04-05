package de.dailab.plistacontest.client.inventory;

import org.tartarus.snowball.SnowballProgram;

import java.util.*;

/**
 * Created by kenneth on 25.03.17.
 */
public class Item {

    private long id;
    private long timestamp;

    private Set<String> document;

    public static SnowballProgram stemmer = new org.tartarus.snowball.ext.GermanStemmer();
    private static final String[] stopWords = {"aber", "als", "am", "an", "auch", "auf", "aus", "bei", "bin", "bis",
            "bist", "da", "dadurch", "daher", "darum", "das", "daß", "dass", "dein", "deine", "dem", "den", "der",
            "des", "dessen", "deshalb", "die", "dies", "dieser", "dieses", "doch", "dort", "du", "durch", "ein", "eine",
            "einem", "einen", "einer", "eines", "er", "es", "euer", "eure", "für", "hatte", "hatten", "hattest",
            "hattet", "hier", "hinter", "ich", "ihr", "ihre", "im", "in", "ist", "ja", "jede", "jedem", "jeden",
            "jeder", "jedes", "jener", "jenes", "jetzt", "kann", "kannst", "können", "könnt", "machen", "mein", "meine",
            "mit", "muß", "mußt", "musst", "müssen", "müßt", "nach", "nachdem", "nein", "nicht", "nun", "oder", "seid",
            "sein", "seine", "sich", "sie", "sind", "soll", "sollen", "sollst", "sollt", "sonst", "soweit", "sowie",
            "und", "unser", "unsere", "unter", "vom", "von", "vor", "wann", "warum", "was", "weiter", "weitere", "wenn",
            "wer", "werde", "werden", "werdet", "weshalb", "wie", "wieder", "wieso", "wir", "wird", "wirst", "wo",
            "woher", "wohin", "zu", "zum", "zur", "über", "–"};


    public Set<String> getDocument() {
        return document;
    }

    public void setDocument(Set<String> document) {
        this.document = document;
    }

    public long getId() {

        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
    public long getTimestamp() {
        return this.timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public static Set<String> cleanText(String text){
        Set<String> document = new HashSet<>();
        for (String word : text.toLowerCase().split(" ")){
            if (!Arrays.stream(stopWords).anyMatch(word::equals) && 2 < word.length()){
                word = word.replaceAll("[^a-zA-Z]", "");
                stemmer.setCurrent(word);
                stemmer.stem();
                word = stemmer.getCurrent();
                document.add(word);
            }
        }
        return document;
    }

    public Item(long id, String title, String text, long timestamp) {
        this.id = id;
        this.document = cleanText(title + " " + text);
        this.timestamp = timestamp;



    }

    public static void main(String[] args) {
        Item item = new Item(4L,
                "Aladdin - Ein Traum wird wahr",
                "Flieg mit mir um die Welt " +
                "Sie gehört der Prinzessin " +
                "niemals darfst Du's vergessen " +
                "denn im Herzen bist Du frei " +
                "Träume werden nun wahr " +
                "sieh nur hin schon passiert es " +
                "drunter, drüber du fliegst " +
                "als wär es plötzlich Zauberei auffaßt", 0L);
        System.out.println(item.getDocument().toString());
    }
}
