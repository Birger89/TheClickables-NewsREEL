/*
Copyright (c) 2013, TU Berlin
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

import java.io.IOException;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.dailab.plistacontest.client.inventory.Item;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to handle the communication with the CLEF NewsREEL challenge server Functions:
 * - parsing incoming HTTP-Requests - delegating requests according to their
 * types - responding to the plista challenge server
 * 
 * @author till, andreas, thanh
 * 
 */
public class ContestHandler extends AbstractHandler {

	/**
	 * Define the default logger
	 */
	private final static Logger logger = LoggerFactory.getLogger(ContestHandler.class);

	/**
	 * here we store all relevant data about items.
	 */
	private final RecommenderItemTable recommenderItemTable = new RecommenderItemTable();

    private HashMap<Long, Set<Item>> domains = new HashMap<>();
    private HashMap<Long, Set<Long>> users = new HashMap<>();
    private HashMap<Long, Item> items = new HashMap<>();

	/**
	 * Define the default recommender, currently not used.
	 */
	@SuppressWarnings("unused")
	private Object contestRecommender;

	/**
	 * Constructor, sets some default values.
	 * 
	 * @param _properties
	 * @param _contestRecommender
	 */
	public ContestHandler(final Properties _properties,
			final Object _contestRecommender) {

		this.contestRecommender = _contestRecommender;

	}

	/**
	 * Handle incoming messages. This method is called by plista We check the
	 * message, and extract the relevant parameter values
	 * 
	 * @see org.eclipse.jetty.server.Handler#handle(java.lang.String,
	 *      org.eclipse.jetty.server.Request,
	 *      javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	public void handle(String arg0, Request _breq, HttpServletRequest _request,
			HttpServletResponse _response) throws IOException, ServletException {

		
		// we can only handle POST messages
		if (_breq.getMethod().equals("POST")) {

			if (_breq.getContentLength() < 0) {
				
				// handles first message from the server - returns OK
				logger.info("Initial Message with no content received.");
				response(_response, _breq, null, false);
				
			} else {

				// handle the normal messages - since we do not know the exact format, we try to be flexible
				String typeMessage = _breq.getParameter("type");
				String bodyMessage = _breq.getParameter("body");
				String propertyMessage = _breq.getParameter("properties");
				String entityMessage = _breq.getParameter("entities");

				String responseText = "";
				
				// handle idomaar messages
				if (bodyMessage == null || bodyMessage.length() == 0) {
				
					// we may recode the body message
					if (_breq.getContentType().equals("application/x-www-form-urlencoded; charset=utf-8")) {
						bodyMessage = URLDecoder.decode(bodyMessage,"utf-8");
						propertyMessage = URLDecoder.decode(propertyMessage,"utf-8");
						entityMessage = URLDecoder.decode(entityMessage, "utf-8");
					}
	
					// delegate the request and create a response message
					responseText =  handleIdomaarMessage(typeMessage, propertyMessage, entityMessage);
				} else {
					// handle old data format messages
					// we may recode the body message
					if (_breq.getContentType().equals("application/x-www-form-urlencoded; charset=utf-8")) {
						bodyMessage = URLDecoder.decode(bodyMessage,"utf-8");
					}
					responseText =  handleTraditionalMessage(typeMessage, bodyMessage);
				}
				// send the response message as text
				response(_response, _breq, responseText, true);
			}
		} else {
			// GET requests are answered by a HTML page
			logger.debug("Get request from " + _breq.getRemoteAddr());
			response(
					_response,
					_breq,
					"Visit <h3><a href=\"http://www.clef-newsreel.org/\">CLEF NewsREEL Challenge</a></h3>",
					true);
		}
	}

	/**
	 * Method to handle incoming messages from the server.
	 * 
	 * @param messageType
	 * 					the messageType of the incoming contest server message.
	 * @param properties
	 * 					
	 * @param entities
	 * @return the response to the contest server
	 */
	@SuppressWarnings("unchecked")
	private String handleIdomaarMessage(final String messageType, final String properties, final String entities) {
        System.out.println("Handle idomaar message");
        // write all data from the server to a file
		// logger.info(messageType + "\t" + properties + "\t" + entities);

		// create an jSON object from the String
		final JSONObject jOP = (JSONObject) JSONValue.parse(properties);
		final JSONObject jOE = (JSONObject) JSONValue.parse(entities);
		
		// merge the different jsonObjects and correct missing itemIDs
		jOP.putAll(jOE);
		Object itemID = jOP.get("itemID");
		if (itemID == null) {
			jOP.put("itemID", 0);
		}

		// define a response object
		String response = null;

		if ("impression".equalsIgnoreCase(messageType) || "recommendation".equalsIgnoreCase(messageType)) {

			// parse the type of the event
			final RecommenderItem item = RecommenderItem.parseEventNotification(jOP.toJSONString());
			final String eventNotificationType = messageType; 

			// impression refers to articles read by the user
			if ("impression".equalsIgnoreCase(eventNotificationType) || "recommendation".equalsIgnoreCase(eventNotificationType)) {

				// we mark this information in the article table
				if (item.getItemID() != null) {
					// new items shall be added to the list of items
//					recommenderItemTable.handleItemUpdate(item);
//					item.setNumberOfRequestedResults(6);

                    if (0 < item.getText().length()){
                        updateItem(item);
                    }

					response = "handle impression eventNotification successful";
					
					boolean recommendationExpected = false;
					if (properties.contains("\"event_type\": \"recommendation_request\"")) {
						recommendationExpected = true;
					}
					if (recommendationExpected) {
//						List<Long> suggestedItemIDs = recommenderItemTable.getLastItems(item);
						response = recommendationRequest(item).toString();
					}
					
				}
				// click refers to recommendations clicked by the user
			} else if ("click".equalsIgnoreCase(eventNotificationType)) {

				// we mark this information in the article table
				if (item.getItemID() != null) {
					// new items shall be added to the list of items
					recommenderItemTable.handleItemUpdate(item);

					response = "handle impression eventNotification successful";
				}
				response = "handle click eventNotification successful";

			} else {
				System.out.println("unknown event-type: "
						+ eventNotificationType + " (message ignored)");
			}

		} else if ("error_notification".equalsIgnoreCase(messageType)) {

			System.out.println("error-notification: " + jOP.toString() + jOE.toJSONString());

		} else {
			System.out.println("unknown MessageType: " + messageType);
			// Error handling
			logger.info(jOP.toString() + jOE.toJSONString());
			// this.contestRecommender.error(jObj.toString());
		}
		return response;
	}

	/**
	 * Method to handle incoming messages from the server.
	 * 
	 * @param messageType
	 *            the messageType of the incoming contest server message
	 * @param _jsonString
	 *            the incoming contest server message
	 * @return the response to the contest server
	 */
	@SuppressWarnings({"unused", "Duplicates"})
	private String handleTraditionalMessage(final String messageType,
			final String _jsonMessageBody) {
		System.out.println("Traditional message");

		// write all data from the server to a file
		logger.info(messageType + "\t" + _jsonMessageBody);

		// create an jSON object from the String
		final JSONObject jObj = (JSONObject) JSONValue.parse(_jsonMessageBody);

		// define a response object
		String response = null;

		// TODO handle "item_create"

		// in a complex if/switch statement we handle the differentTypes of
		// messages
		if ("item_update".equalsIgnoreCase(messageType)) {

			// we extract itemID, domainID, text and the timeTime, create/update
			final RecommenderItem recommenderItem = RecommenderItem
					.parseItemUpdate(_jsonMessageBody);

			// we mark this information in the article table
			if (recommenderItem.getItemID() != null) {
				recommenderItemTable.handleItemUpdate(recommenderItem);
			}
			updateItem(recommenderItem);
			response = ";item_update successfull";
		}

		else if ("recommendation_request".equalsIgnoreCase(messageType)) {

			// we handle a recommendation request
			try {
				// parse the new recommender request
				RecommenderItem currentRequest = RecommenderItem.parseRecommendationRequest(_jsonMessageBody);
				response = getRecommendationResultJSON(recommendationRequest(currentRequest).toString());

				// TODO? might handle the the request as impressions
			} catch (Throwable t) {
				t.printStackTrace();
			}
		} else if ("event_notification".equalsIgnoreCase(messageType)) {

			// parse the type of the event
			final RecommenderItem item = RecommenderItem.parseEventNotification(_jsonMessageBody);
			final String eventNotificationType = item.getNotificationType();

			// impression refers to articles read by the user
			if ("impression".equalsIgnoreCase(eventNotificationType)
					|| "impression_empty".equalsIgnoreCase(eventNotificationType)) {

				// we mark this information in the article table
				if (item.getItemID() != null) {
					// new items shall be added to the list of items
					recommenderItemTable.handleItemUpdate(item);

					response = "handle impression eventNotification successful";
				}
				// click refers to recommendations clicked by the user
			} else if ("click".equalsIgnoreCase(eventNotificationType)) {

				response = "handle click eventNotification successful";

			} else {
				System.out.println("unknown event-type: " + eventNotificationType + " (message ignored)");
			}

		} else if ("error_notification".equalsIgnoreCase(messageType)) {

			System.out.println("error-notification: " + _jsonMessageBody);

		} else {
			System.out.println("unknown MessageType: " + messageType);
			// Error handling
			logger.info(jObj.toString());
			// this.contestRecommender.error(jObj.toString());
		}
		return response;
	}

	private void addItemIdUserSet(long userId, long itemId){
        if (users.get(userId) != null) {
            users.get(userId).add(itemId);
        } else {
            Set<Long> l = new HashSet<>();
            l.add(itemId);
            users.put(userId, l);
        }

    }

	private List<Long> recommendationRequest(RecommenderItem currentRequest) {
        addItemIdUserSet(currentRequest.getUserID(), currentRequest.getItemID());

        List<Long> result = new ArrayList<>();
        Map<Long, Integer> itemScore = new HashMap<>();
        Set<Item> domainItems = domains.get(currentRequest.getDomainID());
        Item item = items.get(currentRequest.getItemID());
        if (item != null) {
            for (Item it: items.values()) {
                itemScore.put(it.getId(), compareKeywords(item, it));
            }
            result = itemScore.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .map(Map.Entry::getKey)
                    .limit(currentRequest.getNumberOfRequestedResults())
                    .collect(Collectors.toList());
        } else if (domainItems != null && 0 < domainItems.size()) {
            result = domainItems.stream()
                    .sorted((e1, e2) -> Long.compare(e2.getTimestamp(), e1.getTimestamp()))
                    .map(Item::getId)
                    .limit(currentRequest.getNumberOfRequestedResults())
                    .collect(Collectors.toList());
        }
        if (result.size() < currentRequest.getNumberOfRequestedResults()) {
            result.addAll(items.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e2.getValue().getTimestamp(), e1.getValue().getTimestamp()))
                    .map(d -> d.getValue().getId())
                    .limit(currentRequest.getNumberOfRequestedResults()-result.size())
                    .collect(Collectors.toList())
            );
        }

        return result;
    }

	private void updateItem(RecommenderItem recommenderItem) {
        Item item = new Item(recommenderItem.getItemID(), "", recommenderItem.getText(), System.currentTimeMillis());
        Item existingItem = items.get(item.getId());

        if (existingItem == null){
            items.put(recommenderItem.getItemID(), item);
        } else if (existingItem.getDocument().size() < item.getDocument().size()){
            items.put(item.getId(), item);
        } else {
            existingItem.setTimestamp(System.currentTimeMillis());
        }

        Set<Item> domainItemList = domains.get(recommenderItem.getDomainID());
        if (domainItemList != null) {
            domainItemList.add(item);
        } else {
            domainItemList = new HashSet<>();
            domainItemList.add(item);
            domains.put(recommenderItem.getDomainID(), domainItemList);
        }
    }

	private int compareKeywords(Item firstItem, Item secondItem) {
		int match = 0;
		if(firstItem == null) {
			System.out.println("first item null");
			return 0;
		}
		if(secondItem == null) {
			System.out.println("second item null");
			return 0;
		}
		List<String> firstItemKeywords = new ArrayList<>(firstItem.getDocument());
		List<String> secondItemKeywords = new ArrayList<>(secondItem.getDocument());
		if(firstItemKeywords == null) {
			System.out.println("first keywords null");
			return 0;
		}
		if(secondItemKeywords == null) {
			System.out.println("second keywords null");
			return 0;
		}

		for (int i = 0; i < firstItemKeywords.size(); i++) {
			for (int j = 0; j < secondItemKeywords.size(); j++) {
				if(firstItemKeywords.get(i).equals(secondItemKeywords.get(j))) {
					match++;
					break;
				}
			}
		}
		return match;
	}



	/**
	 * Response handler.
	 * 
	 * @param _response
	 *            {@link HttpServletResponse} object
	 * @param _breq
	 *            the initial request
	 * @param _text
	 *            response text
	 * @param _b
	 *            boolean to set whether the response text should be sent
	 * @throws IOException
	 */
	private void response(HttpServletResponse _response, Request _breq,	String _text, boolean _b) throws IOException {
		
		// configure the esponse parameters
		_response.setContentType("text/html;charset=utf-8");
		_response.setStatus(HttpServletResponse.SC_OK);
		_breq.setHandled(true);

		if (_text != null && _b) {
			_response.getWriter().println(_text);
			if (_text != null && !_text.startsWith("handle")) {
				logger.debug("send response: " + _text);
			}
		}
	}

	/**
	 * Create a json response object for recommendation requests.
	 * 
	 * @param _itemsIDs
	 *     the recommendation result
	 * @return 
	 * 		the recommendation result as json
	 */
	public static final String getRecommendationResultJSON(String _itemsIDs) {

		// invalid recommendations result in empty result sets
		if (_itemsIDs == null || _itemsIDs.length() == 0) {
			_itemsIDs = "[]";
		} 
		// add brackets if needed
		else if (!_itemsIDs.trim().startsWith("[")) {
			_itemsIDs = "[" + _itemsIDs + "]";
		}
		// build result as JSON according to formal requirements
		String result = "{" + "\"recs\": {" + "\"ints\": {" + "\"3\": "
				+ _itemsIDs + "}" + "}}";

		return result;
	}

}
