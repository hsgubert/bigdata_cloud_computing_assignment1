package tweet_fetcher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class KeywordLoader {

	private final Long INITIAL_TWEET_ID = 512861657798631424L;
	private final Integer BATCH_SIZE = 100;
	
	private String mKeyword;
	private Long mHighestTweetIdLoaded;
	private Long mLastTweetIdLoaded;
	private Long mLowestTweetIdToLoad;
	private PrintWriter mWriter;
	private boolean mFirstLine = false;
	
	private Long mGeoTweetsFetchedNow = 0L;
	private Long mTweetsFetchedNow = 0L;
	
	public KeywordLoader(String keyword) {
		mKeyword = keyword;
		mHighestTweetIdLoaded = 0L;
		mLastTweetIdLoaded = 0L;
		mLowestTweetIdToLoad = INITIAL_TWEET_ID;
	}
	
	public KeywordLoader(JSONObject json) throws JSONException {
		mKeyword = json.getString("keyword");
		mHighestTweetIdLoaded = json.getLong("highestTweetIdLoaded"); 
		mLastTweetIdLoaded = json.getLong("lastTweetIdLoaded");
		mLowestTweetIdToLoad = json.getLong("lowestTweetIdToLoad");
	}
	
	public boolean inTheMiddleOfABlock() {
		return mLastTweetIdLoaded != 0L;
	}
	
	public Long getGeoTweetsFetched() {
		return mGeoTweetsFetchedNow;
	}
	
	public Long getTweetsFetched() {
		return mTweetsFetchedNow;
	}
	
	public String getKeyword() {
		return mKeyword;
	}
	
	public JSONObject toJson() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("keyword", mKeyword);
		json.put("highestTweetIdLoaded", mHighestTweetIdLoaded);
		json.put("lastTweetIdLoaded", mLastTweetIdLoaded);
		json.put("lowestTweetIdToLoad", mLowestTweetIdToLoad);
		return json;
	}
	
	public boolean loadTweets() throws IOException {
		openWriter();
		Twitter twitter = TwitterHelper.getTwitterClient();
		boolean success = false;
		
		while(true) {
			Query query = new Query(mKeyword).
				lang("en").
				count(BATCH_SIZE).
				sinceId(mLowestTweetIdToLoad);
			
			if (mLastTweetIdLoaded != null) {
				query.maxId(mLastTweetIdLoaded-1);
			}
			
			try {
				QueryResult result = twitter.search(query);
				List<Status> tweets = result.getTweets();
				
				// save tweets
				for (Status tweet : tweets) {
					if (tweet.getGeoLocation() == null)
						continue;

					writeTweet(tweet);
					mGeoTweetsFetchedNow += 1;
				}
				mTweetsFetchedNow += tweets.size();

				if (tweets.size() > 1) {
					if (mLastTweetIdLoaded == 0L) {
						mHighestTweetIdLoaded = tweets.get(0).getId();
					}
				
					mLastTweetIdLoaded = tweets.get(tweets.size()-1).getId();
				}
				
				if (tweets.size() < BATCH_SIZE) {
					mLowestTweetIdToLoad = mHighestTweetIdLoaded;
					mLastTweetIdLoaded = 0L;
					System.out.println("Fetched a whole tweet block for keyword: " + mKeyword);
					success = true;
					break;
				}
			}
			catch (TwitterException e) {
				System.out.println("Could not fetch whole tweet block for keyword: " + mKeyword);
				if (e.exceededRateLimitation()) {
					System.out.println("Already used all 180 requests...");
				}
				break;
			}
		}
		closeWriter();
		return success;
	}
	
	private void openWriter() throws IOException {
		File file = new File(getStorageFilename());
		mFirstLine = !file.exists();
		mWriter = createAppendWritter(file);
	}
		
	private void writeTweet(Status tweet) {
		write(
			tweet.getId(), 
			tweet.getCreatedAt().getTime(), 
			tweet.getGeoLocation().getLatitude(), 
			tweet.getGeoLocation().getLongitude()
		);
	}
		
	private void write(Long id, Long createdAt, Double latitude, Double longitude) {
		if (mFirstLine) {
			mFirstLine = false;
		} else {
			mWriter.println();
		}
		
		mWriter.print(
			mKeyword + "," +
			String.valueOf(id) + "," +
			String.valueOf(createdAt) + "," +
			String.valueOf(latitude) + "," +
			String.valueOf(longitude)
		);
	}
	
	private void closeWriter() {
		mWriter.close();
	}
	
	private String getStorageFilename() {
		return "tweets-" + mKeyword.replaceAll(" ", "_") + ".txt";
	}
	
//	private String lastLine(File file) throws IOException {
//		String lastLine = null;
//		ReversedLinesFileReader reverseReader = null;
//		try {
//			reverseReader = new ReversedLinesFileReader(file);
//			lastLine = reverseReader.readLine();
//			lastLine = reverseReader.readLine();
//			reverseReader.close();
//		} catch (IOException e) {
//			if (reverseReader != null) {
//				reverseReader.close();
//			}
//		}
//		return lastLine;
//	}
	
	private PrintWriter createAppendWritter(File file) throws IOException {
		return (new PrintWriter(new BufferedWriter(new FileWriter(file, true))));
	}
}
