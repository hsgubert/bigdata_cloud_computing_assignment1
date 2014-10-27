package tweet_fetcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;
 
public class TwitterHelper {
 
	// Open communication with Tweet API 
	public static Twitter getTwitterClient() throws IOException {
		Properties properties = TwitterHelper.getProperties();
		
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setOAuthConsumerKey(properties.getProperty("OAuthConsumerKey"))
		  .setOAuthConsumerSecret(properties.getProperty("OAuthConsumerSecret"))
		  .setOAuthAccessToken(properties.getProperty("OAuthAccessToken"))
		  .setOAuthAccessTokenSecret(properties.getProperty("OAuthAccessTokenSecret"));
		TwitterFactory tf = new TwitterFactory(cb.build());
		Twitter twitter = tf.getInstance();
		
		return twitter;
	}
	
	private static Properties getProperties() throws IOException { 
		Properties properties = new Properties();
		String propertiesFilename = "twitter.properties";
 
		File file = new File(propertiesFilename);
		InputStream inputStream = new FileInputStream(file);
		properties.load(inputStream);
 
		return properties;
	}
}
