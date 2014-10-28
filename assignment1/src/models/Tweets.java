package models;

import helpers.DynamoHelper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

public class Tweets {

	private static final String TWEET_TABLE_NAME = "tweets";
	public static final String[] VALID_KEYWORDS = {"columbia", "stanford", "harvard", 
		"yale", "upenn", "carnegie", "cornell", "@mit", "ucberkeley", "princeton",
		"caltech", "duke", "vanderbilt", "dartmouth", "georgia tech", "harveymudd", 
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
}
