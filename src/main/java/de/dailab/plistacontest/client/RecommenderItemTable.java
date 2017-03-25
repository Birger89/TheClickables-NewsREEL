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


	private Long getNearestNeighbor(Long userID){
		Long neighborID=0L;
		Set<Long> blackListedIDs = new HashSet<Long>();
		blackListedIDs.add(0L);
		Set<Long> userItems = table.getValuesByKey(userID+"", 10,blackListedIDs);
		Set<Long> neighborItems;
		Integer similarityIndex=0;
		if(users.size()>0){
			for (Long user: users) {
				if(user == userID){
					continue;
				}
				neighborItems = table.getValuesByKey(user+"", 10,blackListedIDs);
				neighborItems.retainAll(userItems);
				if(neighborItems.size()>similarityIndex){
					neighborID = user;
					similarityIndex=neighborItems.size();
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
		System.out.println("=======================Handle=========================");
		
		// check the item
		if (_item == null || _item.getItemID() == null || _item.getItemID() == 0L || _item.getDomainID() == null) {
			return false;
		}
		
		// add the item to the table
		// yara
		if(_item.getUserID() == 0){
			System.out.println("Can't track user");
			return true;
		}
		users.add(_item.getUserID());
		System.out.println("Number of users: "+users.size());
		table.addValueByKey(_item.getUserID()+"", _item.getItemID());
		//table.addValueByKey(_item.getDomainID() + "", _item.getItemID());
		return true;
	}



	/**
	 * Return something from the buffer (the most recently inserted items have a higher priority to be chosen).
	 * @param _currentRequest, the itemID, the domainID, an the numberOfRequestedResults are extracted and analyzed  
	 * @return a list of items. The itemID present in the request and itemID=0 will not be contained in the result. 
	 */
	public List<Long> getLastItems(final RecommenderItem _currentRequest) {
		System.out.println("-----GET LAST ITEMS-----");

		Integer numberOfRequestedResults = _currentRequest.getNumberOfRequestedResults();
		System.out.println("Number of Req. Results: "+numberOfRequestedResults);
		Long itemID = _currentRequest.getItemID();
		System.out.println("ITEM ID: "+itemID);
		Long domainID = _currentRequest.getDomainID();
		System.out.println("Number of Req. Results: "+numberOfRequestedResults);

		// yara
		Long userID = _currentRequest.getUserID();
		System.out.println("USER ID: "+userID);
		Long neighborID = 2423831855L;

		
		// handle invalid values
		if (numberOfRequestedResults == null || numberOfRequestedResults.intValue() < 0 || numberOfRequestedResults.intValue() > 10 || domainID == null) {
			return new ArrayList<Long>(0);
		}

		neighborID = getNearestNeighbor(userID);
		System.out.println("NEIGHBOR: "+neighborID);

		Set<Long> bLID = new HashSet<Long>();
		bLID.add(0L);
		Set<Long> userItems = table.getValuesByKey(userID+"", 10, bLID);
		for (Long item: userItems) {
			System.out.println("AN ITEM: "+item);
		}

		// create a set of blacklisted items
		Set<Long> blackListedIDs = new HashSet<Long>();
		blackListedIDs.add(0L);
		blackListedIDs.add(itemID);
		//blackListedIDs.addAll(userItems);
		Set<Long> result = table.getValuesByKey(neighborID+"", numberOfRequestedResults.intValue(), blackListedIDs);
		System.out.println("RESULT SIZE: "+result.size());
		if(result.size()>0) {
			for (Long item : result) {
				System.out.println("User: " + neighborID + " Item: " + item);
			}
		}
		// get the suggestions, considering the domainID and the blacklist
		//Set<Long> result = table.getValuesByKey(userID+"", numberOfRequestedResults.intValue(), blackListedIDs);
		
		// copy the results to a new list and return
		List<Long> returnResult = new ArrayList<Long>();
		returnResult.addAll(result);
		return returnResult;
	}
}
