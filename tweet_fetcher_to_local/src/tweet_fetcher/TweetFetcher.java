package tweet_fetcher;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class TweetFetcher {

	//private static final String TWEET_TABLE_NAME = "tweets";
	private static final String[] KEYWORDS = {"columbia university", "stanford", "harvard", 
		"yale", "upenn", "carnegie mellon", "cornell", "@mit", "ucberkeley", "princeton",
		"caltech", "duke", "vanderbilt", "dartmouth", "georgia tech", "harveymudd", 
		"purdue", "georgetown", "brown university", "rice university", "darden", "wharton", "kellogg", 
		"booth", "sloan", "haas", "tuck", "stern", "community college"};
	
	public static void main(String[] args) throws IOException, JSONException {

		File file = new File("loading_status.txt");
		if (!file.exists()) {
			JSONArray jsonArray = new JSONArray();
			for (String keyword : KEYWORDS) {
				KeywordLoader loader = new KeywordLoader(keyword);
				jsonArray.put((JSONObject) loader.toJson());
			}
			jsonArray.getJSONObject(0);
			PrintWriter writer = new PrintWriter(file);
			writer.print(jsonArray.toString(2));
			writer.close();
		}
		
		FileReader reader = new FileReader(file);
		char[] buffer = new char[100];
		StringBuilder str = new StringBuilder();
		while (reader.read(buffer, 0, 100) > 0) {
			str.append(buffer);
		}
		reader.close();
		
		// load configs
		JSONArray jsonArray = new JSONArray(str.toString());
		ArrayList<KeywordLoader> loaders = new ArrayList<KeywordLoader>();
		for (int i=0; i<jsonArray.length(); i++) {
			loaders.add(new KeywordLoader(jsonArray.getJSONObject(i)));
		}
		
		// find loading that did not finish
		boolean stop = false;
		for (KeywordLoader loader : loaders) {
			if (loader.inTheMiddleOfABlock()) {
				boolean goOn = loader.loadTweets();
				System.out.println("Keyword: " + loader.getKeyword() + "  Tweets: " + loader.getTweetsFetched() + "  Geotweets: " + loader.getGeoTweetsFetched());
				if (!goOn) {
					stop = true;
					break;
				}
			}
		}
		
		if (!stop) {
			for (KeywordLoader loader : loaders) {
				boolean goOn = loader.loadTweets();
				System.out.println("Keyword: " + loader.getKeyword() + "  Tweets: " + loader.getTweetsFetched() + "  Geotweets: " + loader.getGeoTweetsFetched());
				if (!goOn) {
					stop = true;
					break;
				}
			}
		}
		
		JSONArray newJsonArray = new JSONArray();
		for (KeywordLoader loader : loaders) {
			newJsonArray.put(loader.toJson());
		}
		PrintWriter writer = new PrintWriter(file);
		writer.print(newJsonArray.toString(2));
		writer.close();
		
		System.out.println("THE END");
		System.out.println();
	}
	
}