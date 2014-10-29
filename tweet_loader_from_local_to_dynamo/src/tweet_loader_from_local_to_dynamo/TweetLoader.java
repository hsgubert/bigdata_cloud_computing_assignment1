package tweet_loader_from_local_to_dynamo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;

public class TweetLoader {

	private static final String TWEET_TABLE_NAME = "tweets";
	
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
		
		File folder = new File("../tweet_fetcher_to_local/Initial tweets");
		File[] listOfFiles = folder.listFiles();
		Pattern pattern = Pattern.compile("tweets-([^\\.]+)\\.txt");
		
		for (File file : listOfFiles) {
			Matcher matcher = pattern.matcher(file.getName());
			if (matcher.matches()) {
				String keyword = matcher.group(1);
				System.out.println("Uploading " + file.getName() + " (keyword: " + keyword + ")");
				
				// Construct BufferedReader from FileReader
				BufferedReader br = new BufferedReader(new FileReader(file));
				String line = null;
				while ((line = br.readLine()) != null) {
					String[] tokens = line.split(",");
					Map<String, AttributeValue> attributeMap = new HashMap<String,AttributeValue>();
					attributeMap.put("keyword", new AttributeValue().withS(tokens[0]));
					attributeMap.put("id", new AttributeValue().withN(tokens[1]));
					attributeMap.put("created_at", new AttributeValue().withN(tokens[2]));
					attributeMap.put("latitude", new AttributeValue().withN(tokens[3]));
					attributeMap.put("longitude", new AttributeValue().withN(tokens[4]));
					dynamoHelper.putItem(TWEET_TABLE_NAME, attributeMap);
				}
				br.close();
			}
			
		}
		
	}

}
