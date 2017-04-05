package de.dailab.plistacontest.client.inventory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by kenneth on 01.04.17.
 */
public class Test {

    public static void main(String[] args) {
        Set<Item> domainItems = new HashSet<>();
        domainItems.add(new Item(1L, "Kek", "Kuk", 10L));
        domainItems.add(new Item(2L, "Kek", "Kuk", 2L));
        domainItems.add(new Item(3L, "Kek", "Kuk", 99999L));
        domainItems.add(new Item(4L, "Kek", "Kuk", 4L));
        domainItems.add(new Item(5L, "Kek", "Kuk", 4L));
        domainItems.add(new Item(6L, "Kek", "Kuk", 80L));


        List<Long> result = domainItems.stream()
                .sorted((e1, e2) -> Long.compare(e2.getTimestamp(), e1.getTimestamp()))
                .map(Item::getId)
                .limit(3)
                .collect(Collectors.toList());
        System.out.println(result.toString());
        int n = 0;
        String term = "kek";
        String word = "kek";
        n += term.equalsIgnoreCase(word) ? 1:0;
        System.out.println(n);
    }
}
