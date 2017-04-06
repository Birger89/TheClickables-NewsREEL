/*
Copyright (c) 2014, TU Berlin
Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
DEALINGS IN THE SOFTWARE.
*/

package de.dailab.plistacontest.client;



import java.io.*;
import java.util.*;

/**
* This class stores a list of items that can be recommended.
 */
public class RecommenderItemTable {

	/**
	 * We create a data structure providing a fixed size array/ring buffer for all relevant domains (news portals)
	 * first parameter (string) defines the domainID; the second parameter the type of the stored newsID
	 */


	private DirtyRingBuffer<String, Long> table = new DirtyRingBuffer<String, Long>(100);

	private Set<Long> users = new HashSet<Long>();

	private static Map<Long, Set<Long>> userBasedTable = new HashMap<Long, Set<Long>>();

	private static final String FILEPATH = "output.dat";

	private static BufferedWriter bufferedWriter;

	public RecommenderItemTable() {
		try{
		bufferedWriter = new BufferedWriter(new FileWriter(new File(FILEPATH)));}
		catch(IOException exceptiom){
			exceptiom.printStackTrace();
		}
	}

	/**
	 * Adds the userID and itemID to the userBasedTable Map
	 * @param userID
	 * @param itemID
	 */
	private void addItem(Long userID, Long itemID){
		users.add(userID);
		Set<Long> itemList = userBasedTable.get(userID);
		if(itemList==null){
			itemList = new HashSet<Long>();
		}
		itemList.add(itemID);
		userBasedTable.put(userID,itemList);
	}

	/**
	 * Calculate similarityIndex between two users
	 * @param user
	 * @param targetUser
	 * @return similarityIntdex
	 */
	private Integer getSimilarityIndex(Long user, Long targetUser){
		Set<Long> neighborItems = userBasedTable.get(user);
		Set<Long> userItems = userBasedTable.get(targetUser);
		neighborItems.retainAll(userItems);
		return neighborItems.size();
	}

	/**
	 * Get the user that is most similar to the target user
	 * @param _item
	 * @return nearest neighbor
	 */
	private Long getNearestNeighbor(final RecommenderItem _item){
		Long userID = _item.getUserID();
		Long neighborID=0L;
		Set<Long> blackListedIDs = new HashSet<Long>();
		blackListedIDs.add(0L);
		Integer similarityIndex=0;
		Integer similarityTemp;
		if(users.size()>0){
			for (Long user: users) {
				if(user == userID){
					continue;
				}
				similarityTemp = getSimilarityIndex(user,userID);
				if(similarityTemp>similarityIndex){
					neighborID = user;
					similarityIndex=similarityTemp;
				}
			}
		}
		return neighborID;
	}

	/**
	 * Handle the item update; put the itemID in the buffer for the correct domain.
	 * @param _item
	 * @return
	 */
	public boolean handleItemUpdate(final RecommenderItem _item) {
		
		// check the item
		if (_item == null || _item.getItemID() == null || _item.getItemID() == 0L || _item.getDomainID() == null) {
			return false;
		}
		
		// add the item to the table
		if(_item.getUserID() == 0){
			System.out.println("Can't track user");
			table.addValueByKey(_item.getDomainID()+"",_item.getItemID());
			return false;
		}

		addItem(_item.getUserID(), _item.getItemID());
		table.addValueByKey(_item.getDomainID()+"",_item.getItemID());
		return true;
	}


	public static void writeTransactions() {
		String transactionItems = "";
		for(Map.Entry<Long, Set<Long>> entry : userBasedTable.entrySet()){
			if(entry.getValue().size()<=0){
				continue;
			}
			for (Long item : entry.getValue()) {
				System.out.print(item);
				transactionItems+=(item+" ");
			}
			transactionItems+=System.getProperty("line.separator");
			try {
				bufferedWriter.write(transactionItems);
			}catch (IOException exc){
				exc.printStackTrace();
			}
		}
	}



	/**
	 * Return something from the buffer (the most recently inserted items have a higher priority to be chosen).
	 * @param _currentRequest, the itemID, the domainID, an the numberOfRequestedResults are extracted and analyzed  
	 * @return a list of items. The itemID present in the request and itemID=0 will not be contained in the result. 
	 */
	public List<Long> getLastItems(final RecommenderItem _currentRequest) {
		Integer numberOfRequestedResults = _currentRequest.getNumberOfRequestedResults();
		Long itemID = _currentRequest.getItemID();
		Long domainID = _currentRequest.getDomainID();
		Long userID = _currentRequest.getUserID();
		//Add User and Item to the Map @userBasedTable
		addItem(userID,itemID);

		// handle invalid values
		if (numberOfRequestedResults == null || numberOfRequestedResults.intValue() < 0 || numberOfRequestedResults.intValue() > 10 || domainID == null) {
			return new ArrayList<Long>(0);
		}

		Long neighborID = 0L;
		neighborID = getNearestNeighbor(_currentRequest);
		Set<Long> result = userBasedTable.get(neighborID);

		// create a set of blacklisted items
		Set<Long> blackListedIDs = new HashSet<Long>();
		blackListedIDs.add(0L);
		blackListedIDs.add(itemID);

		System.out.println("RESULT SIZE: "+result.size());
		// if user-based algorithm couldn't recommend, get the suggestions, considering the domainID and the blacklist
		if(result.size()<numberOfRequestedResults.intValue()){
			result.addAll(table.getValuesByKey(domainID+"",numberOfRequestedResults.intValue() - result.size(), blackListedIDs));
		}
		System.out.println("RESULT SIZE: "+result.size());
		if(result.size()>0) {
			for (Long item : result) {
				System.out.println("User: " + neighborID + " Item: " + item);
			}
		}
		writeTransactions();

		List<Long> returnResult = new ArrayList<Long>();
		returnResult.addAll(result);
		return returnResult;
	}
}
