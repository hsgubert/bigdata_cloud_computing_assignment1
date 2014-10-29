package tweet_fetcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;

public class TweetFetcher {

	private static final String TWEET_TABLE_NAME = "tweets";
	private static final String[] KEYWORDS = {"columbia university", "stanford", "harvard", 
		"yale", "upenn", "carnegie mellon", "cornell", "@mit", "ucberkeley", "princeton",
		"caltech", "duke", "vanderbilt", "dartmouth", "georgia tech", 
		"purdue", "georgetown", "brown university", "rice university", "darden", "wharton", "kellogg", 
		"booth", "sloan", "haas", "tuck", "stern", "community college"};
	
	public static void main(String[] args) throws IOException {
		DynamoHelper dynamoHelper = DynamoHelper.getInstance();
		
		// Checks if the DynamoDb table does not exist, and creates it
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
		
		Twitter twitter = TwitterHelper.getTwitterClient();
	    
		String language = "en";
		Integer batchSize = 100;
		Query query = null;
		QueryResult result = null;
		
		int tweetFetchSuccessCounter = 0;
		int tweetFetchErrorCounter = 0;
		int dynamoPutSuccessCounter = 0;
		int dynamoPutErrorCounter = 0;
		int tweetCount = 0;
		int geoTweetCount = 0;
		
		// Fetch new tweets
		for (String queryString : KEYWORDS) {

			// Start by discovering the last tweet fetched for this keyword
			Long highestIdInDynamo = null;
			Map<String, AttributeValue> lastTweetAttributes = dynamoHelper.getLastItemInRangeByHashKey(TWEET_TABLE_NAME, "keyword", queryString);
			if (lastTweetAttributes != null) {
				highestIdInDynamo = Long.valueOf(lastTweetAttributes.get("id").getN());
			}
			else {
				highestIdInDynamo = 521861657798631424L;
			}
			
			// Now start fetching new tweets
			Long lowestTweetIdFromLastPage = null;
			while(true) {
				query = new Query(queryString).
					lang(language).
					count(batchSize).
					sinceId(highestIdInDynamo);
				
				if (lowestTweetIdFromLastPage != null) {
					query.maxId(lowestTweetIdFromLastPage-1);
				}
				
				try {
					result = twitter.search(query);
					tweetFetchSuccessCounter += 1;
					
					// save tweets
					for (Status tweet : result.getTweets()) {
						if (tweet.getGeoLocation() == null)
							continue;
						
						geoTweetCount += 1;
						Map<String, AttributeValue> attrMap = new HashMap<String, AttributeValue>();
						attrMap.put("keyword", new AttributeValue().withS(queryString));
						attrMap.put("id", new AttributeValue().withN(Long.toString(tweet.getId())));
						attrMap.put("created_at", new AttributeValue().withN(Long.toString(tweet.getCreatedAt().getTime())));
						attrMap.put("latitude", new AttributeValue().withN(Double.toString(tweet.getGeoLocation().getLatitude())));
						attrMap.put("longitude", new AttributeValue().withN(Double.toString(tweet.getGeoLocation().getLongitude())));
						
						if (dynamoHelper.putItem(TWEET_TABLE_NAME, attrMap)) {
							dynamoPutSuccessCounter += 1;
						} else {
							System.out.println("Could not save tweet...");
							dynamoPutErrorCounter += 1;
						}
					}

					// aggregates statistic
					int tweetsFetchedCount = result.getTweets().size(); 
					tweetCount += tweetsFetchedCount;
					
					// if the batch was not full means the tweets are over
					if (tweetsFetchedCount < batchSize) {
						break;
					}
					
					// saves the lowest tweet id return so it can ask for the next page
					lowestTweetIdFromLastPage = result.getTweets().get(tweetsFetchedCount-1).getId();
					
					// stops the script if we have seen too much errors
					if (tweetFetchSuccessCounter < tweetFetchErrorCounter || dynamoPutSuccessCounter < dynamoPutErrorCounter) {
						System.out.println("Too many errors!");
						System.out.println("tweetFetchSuccessCounter: " + tweetFetchSuccessCounter);
						System.out.println("tweetFetchErrorCounter: " + tweetFetchErrorCounter);
						System.out.println("dynamoPutSuccessCounter:" + dynamoPutSuccessCounter);
						System.out.println("dynamoPutError: " + dynamoPutErrorCounter);
						System.exit(1);
					}
				} catch (TwitterException e) {
					System.out.println("Could not fetch all tweets...");
					if (e.exceededRateLimitation()) {
						System.out.println("Already used all 180 requests... Shutting down");
						System.out.println(geoTweetCount + " geo tweets fetched");
						System.out.println(tweetCount + " tweets fetched");
						System.exit(0); 
					}
					tweetFetchErrorCounter += 1;
					e.printStackTrace();
					break;
				}
			}
		}
		
		System.out.println(geoTweetCount + " geo tweets fetched");
		System.out.println(tweetCount + " tweets fetched");
	}

}
