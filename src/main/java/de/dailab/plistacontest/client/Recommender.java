package de.dailab.plistacontest.client;

public class Recommender {

    private static RecommenderItemTable table;

    /**
	 * Constructor
     */
    public Recommender() {
        this.table = new RecommenderItemTable();
    }
    
    public void handleItemUpdate(RecommenderItem item) {
        this.table.handleItemUpdate(item);
    }
}
