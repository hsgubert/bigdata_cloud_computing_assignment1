package models;

import helpers.DynamoHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;

public class Tweets {

	private static final String TWEET_TABLE_NAME = "tweets";
	public static final String[] KEYWORDS = {"columbia university", "stanford", "harvard", 
		"yale", "upenn", "carnegie mellon", "cornell", "@mit", "ucberkeley", "princeton",
		"caltech", "duke", "vanderbilt", "dartmouth", "georgia tech", 
		"purdue", "georgetown", "brown university", "rice university", "darden", "wharton", "kellogg", 
		"booth", "sloan", "haas", "tuck", "stern", "community college"};
	
	@SuppressWarnings("unchecked")
	public static JSONObject retrieveTweetsForKeyword(String keyword) throws IOException {
		DynamoHelper dynamoHelper = DynamoHelper.getInstance();
		List<Map<String, AttributeValue>> tweets = dynamoHelper.queryByPrimaryKey(TWEET_TABLE_NAME, "keyword", keyword);
		
		JSONArray latitudes = new JSONArray();
		JSONArray longitudes = new JSONArray();
		for (Map<String, AttributeValue> tweet : tweets) {
			latitudes.add(Double.valueOf(tweet.get("latitude").getN()));
			longitudes.add(Double.valueOf(tweet.get("longitude").getN()));
		}
		
		JSONObject points = new JSONObject();
		points.put("latitudes", latitudes);
		points.put("longitudes", longitudes);
		
		return points;
	}
	
	public static void ensureTableExists() throws IOException {
		DynamoHelper dynamoHelper = DynamoHelper.getInstance();
		
		if (!dynamoHelper.checkIfTableExists(TWEET_TABLE_NAME)) {
			// create a hash key for the email (string)
			ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
			attributeDefinitions.add(new AttributeDefinition().withAttributeName("keyword").withAttributeType("S"));
			attributeDefinitions.add(new AttributeDefinition().withAttributeName("id").withAttributeType("N"));
			
			// specify that the key is of type hash and range
			ArrayList<KeySchemaElement> keySchemaElements = new ArrayList<KeySchemaElement>();
			keySchemaElements.add(new KeySchemaElement().withAttributeName("keyword").withKeyType(KeyType.HASH));
			keySchemaElements.add(new KeySchemaElement().withAttributeName("id").withKeyType(KeyType.RANGE));
			
			// create table
			dynamoHelper.createTable(attributeDefinitions, keySchemaElements, 20L, 20L, TWEET_TABLE_NAME);
		}
	}
}
