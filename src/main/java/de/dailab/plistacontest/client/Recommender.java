package de.dailab.plistacontest.client;

import java.util.List;


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
    
    public List<Long> getRecommendations(RecommenderItem item) {
        List<Long> recs = this.table.getLastItems(item);
        
        return recs;
    }
}
