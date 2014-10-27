Name: Henrique Spyra Gubert
UNI: hs2807

#############################
REPOSITORY CONTENTS
#############################

The complete assignment is saved in this repository. The top level of the repository contains:
	- assignment1: java web application to show the tweets on the map
	- tweet_fetcher: java application to fetch tweets from the Twitter API and to store tweets with geolocation in DynamoDB
	- deployer: java application that deploys the web application programmatically to ElasticBeanstalk
	- ...

#############################
RUNNING THE APPLICATIONS (LOCALLY)
#############################

WEB APPLICATION (assignment1)
This is a Eclipse Java project. To run the project just make sure you have all external dependencies and import the project in Eclipse. The dependencies are: Tomcat 7, JDK 1.7 and AWS SDK for Java.

TWEET_FETCHER
This is a Eclipse Java project for a regular java standalone application. You need to do is import the project in eclipse and run. Additionally you have to copy the "twitter.properties.default" file to a "twitter.properties" file and fill in your credentials so the application can access the Twitter API. The dependencies are: JDK 1.7 and AWS SDK for Java.
To run this application using CRON every 15 minutes just issue the command "crontab -e" to edit the crontab and add the line "*/15 * * * * /usr/bin/java -jar PATH_TO_JAR". The "twitter.properties" file should be in the same folder as the JAR.

DEPLOYER
This is a Eclipse Java project for a regular java standalone application. All you need to do is import the project in eclipse and run. The dependencies are: JDK 1.7 and AWS SDK for Java.

#############################
SYSTEM ARCHITECTURE
#############################

The system is designed in the following way:
	
	- TWEET_FETCHER runs in a EC2 instance periodically, configured with CRON, and downloads all tweets it can related to the target fixed keywords. Every tweet with geo location is stored in DynamoDB, together with its coordinates and indexed by keyword. After 180 requests the Twitter API will refuse to respond and then TWEET_FETCHER finishes. After 15 minutes the Twitter API is accepting requests again and so TWEET_FETCHER is run by CRON every 15 minutes.

	- WEB APPLICATION (backend) runs in ElasticBeanstalk within a Autoscale group. This means the application can be running in multiple EC2 instances and have its requests forwarded by a node balancer. The WEB APPLICATION connects to DynamoDB to load data (like users, and tweets) and responds to HTTP requests of a client browser.

	- WEB APPLICATION (frontend) is written in HTML and Javascript. When the WEB APPLICATION shows the map (from Google Maps API) to the user, the map is initially blank, but through assynchronous AJAX calls the javascript populates the map with the tweets. Tha javascript on the page makes requests to the server, that in turn loads the data from DynamoDB and returns the coordinates of the tweets in JSON format. The javascript takes care of requesting the tweets related to the selected keyword, or simply to request all tweets and show them in the map as a heatmap. The javascript also take care of doing multiple requests to load data, so the JSON responses within a acceptable size.

	- DEPLOYER is a Java application designed to be ran locally to deploy the WEB APPLICATION to ElasticBeanstalk. The DEPLOYER creates the ElasticBeanstalk application if it does not exist, uploads the WAR file of the WEB APPLICATION to a S3 bucket, and then accesses the ElasticBeanstalk API to configure and create the environment and launch the application. If the application is already running in ElasticBeanstalk, DEPLOYER detects that and instead of deploying a new application only updates the current application.

#############################
ASSIGNMENT THEME
#############################

Since we had to choose a some keywords to fetch the related tweets, instead of just picking random words I decided to do a themed application. In my application all keywords are US universities names or acronyms. I have picked the possibly 30 more famous universities in the US, plus the string "community college", and used that as keywords to classify and segment the tweets. Because of that we can see on the map which universities are mentioned more often in each region of the USA and we can also see the total distribution of people mentioning any of those universities or tweets related to "community college". 

This theme changes nothing in the implementation of the assignment, but does make the resulting map a little more interesting to analyze.

#############################
EXTRA FEATURES
#############################

	- USER / LOGIN SYSTEM: As the system is deployed in the public network I didn't want random people (or even crawlers) to consume my already limited DynamoDB throughput (Free Tier) or to generate me extra charges, so I created a simple authentication system (I also wanted to create the authentication system just as a learning exercise). I made a simple login page that also allows users with "@columbia.edu" email to sign up to create their users. Users are stored in a table in DynamoDB that holds the user name, email and encrypted password. When the user logs in a session key is generated and stored in the user cookies, as well as in DynamoDB. After that this key is used to authenticate every request.

	- EMAIL ADDRESS CONFIRMATION: So that the authentication system would work, it was also necessary to authenticate the email address the user put to sign up, otherwise anyone could just come up with a columbia email and login to my system. To protect the system I implemented an email confirmation mechanism using Amazon SES (Simple Email Service). Basically the system sends an email with an activation link to the user, and the user must click the link to activate its account. Before activation the user is not allowed to login.

	- KEYWORD FILTERING: I undestood this was optional, but I implemented it anyway. However, if you want to see all the data all you have to do is select "All data" in the keyword select box.

	- ASYNCHRONOUS TWEET LOADING: In order to keep the user experience better, the web application first renders the page containing an empty map, and then the javascript does AJAX calls to populate the heatmap, according to the keywords selected. The javascript also caches tweet data so it does not have to do multiple requests to load the same data.

	- PROGRAMMATIC DEPLOY: As explained earlier, the DEPLOYER application take care of everything related to deploy and re-deploys.

#############################
URL
#############################

http://hs2807-assignment1.elasticbeanstalk.com/

