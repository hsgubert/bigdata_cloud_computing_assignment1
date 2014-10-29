package deployer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationVersionDescription;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentStatus;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentTier;
import com.amazonaws.services.elasticbeanstalk.model.S3Location;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

public class Deployer {

	private static final String APPLICATION_NAME = "TweetMap";
	private static final String ENVIRONMENT_NAME = "Staging";
	private static final String DEPLOYMENTS_BUCKET_NAME = "elasticbeanstalk-us-east-1-9898989898";
	private static final String WAR_FILE_PATH = "../assignment1/output/assignment1.war";
	
	public static void main(String[] args) throws InterruptedException {
		// Create client
		System.out.println("Connecting to Amazon...");
		AWSCredentials credentials = new DefaultAWSCredentialsProviderChain().getCredentials();
		AWSElasticBeanstalkClient ebClient = new AWSElasticBeanstalkClient(credentials);
		ebClient.setRegion(Region.getRegion(Regions.US_EAST_1));
		
		// Check if the application already exists
		System.out.println("Checking is application already exists.");
		boolean applicationAlreadyExists = false;
		int triesLeft = 10;
		while (!applicationAlreadyExists) {
			DescribeApplicationsRequest daRequest = new DescribeApplicationsRequest();
			daRequest.withApplicationNames(APPLICATION_NAME);
			DescribeApplicationsResult daResult = ebClient.describeApplications(daRequest);
			applicationAlreadyExists = (daResult.getApplications().size() == 1);
				
			// Create application if it does not exist
			if (applicationAlreadyExists) {
				System.out.println("Application exists!");
				System.out.println();
				break;
			}
			else {
				if (triesLeft == 10) {
					System.out.println("Requesting application creation");
					CreateApplicationRequest caRequest = new CreateApplicationRequest(APPLICATION_NAME);
					ebClient.createApplication(caRequest);
				}
				if (triesLeft == 0) {
					System.out.println("Could not create application.");
					System.exit(1);
				}
				triesLeft -= 1;
				System.out.println("Waiting for application to be created");
				Thread.sleep(10000);
			}
		}
		
		// Retrieve the last application version if it exists
		int maxVersion = 0;
		System.out.println("Looking for the last version deployed");
		DescribeApplicationVersionsRequest davRequest = new DescribeApplicationVersionsRequest()
			.withApplicationName(APPLICATION_NAME);
		List<ApplicationVersionDescription> applicationVersions = ebClient.describeApplicationVersions(davRequest).getApplicationVersions();
		
		for (ApplicationVersionDescription avd : applicationVersions) {
			int version = Integer.parseInt(avd.getVersionLabel());
			if (version > maxVersion)
				maxVersion = version;
		}
		
		if (maxVersion != 0) {
			System.out.println("Last version deployed was: " + String.valueOf(maxVersion));
		} else {
			System.out.println("This is the first version");
		}
		System.out.println();
		
		// now let's put the war file in S3, we already have a bucket for deployments
		System.out.println("Uploading WAR to S3...");
		AmazonS3Client s3Client = new AmazonS3Client(credentials);
		File warFile = new File(WAR_FILE_PATH);
		String keyname = APPLICATION_NAME + "-" + ENVIRONMENT_NAME + "-" + String.valueOf(maxVersion+1) + ".war";
        s3Client.putObject(new PutObjectRequest(DEPLOYMENTS_BUCKET_NAME, keyname, warFile));
            
        // wait until object appears on S3
        triesLeft = 10;
		while (true) {
			S3Object object = s3Client.getObject(new GetObjectRequest(DEPLOYMENTS_BUCKET_NAME, keyname));
		
			if (object != null) {
				System.out.println("Upload complete!");
				System.out.println();
				break;
			}
			
			triesLeft -= 1;
			if (triesLeft == 0) {
				System.out.println("Could not upload war file.");
				System.exit(1);
			}
			System.out.println("Waiting upload...");
			Thread.sleep(20000);
		}
        
        // once object is uploaded we can proceed creating a application version
		System.out.println("Creating application Version");
		S3Location sourceLocation = new S3Location(DEPLOYMENTS_BUCKET_NAME, keyname);
		CreateApplicationVersionRequest cavRequest = new CreateApplicationVersionRequest()
			.withApplicationName(APPLICATION_NAME)
			.withVersionLabel(String.valueOf(maxVersion+1))
			.withSourceBundle(sourceLocation);
		ApplicationVersionDescription avDescription = ebClient.createApplicationVersion(cavRequest).getApplicationVersion();
		System.out.println("Application version created!");
		System.out.println();
		
		// Checks if the environment already exists
		System.out.println("Checking if some environment already exists");
		DescribeEnvironmentsRequest deRequest = new DescribeEnvironmentsRequest();
		deRequest.withApplicationName(APPLICATION_NAME).withEnvironmentNames(ENVIRONMENT_NAME);
		DescribeEnvironmentsResult deResult = ebClient.describeEnvironments(deRequest);
		
		if (deResult.getEnvironments().size() == 0 || !deResult.getEnvironments().get(0).getEnvironmentName().equals(ENVIRONMENT_NAME)) {
			// Create a new Environment
			System.out.println("No environment found");
			System.out.println("Creating a new environment");
			List<ConfigurationOptionSetting> optionSettings = new ArrayList<ConfigurationOptionSetting>();
			optionSettings.add(new ConfigurationOptionSetting("aws:elasticbeanstalk:environment", "EnvironmentType", "LoadBalanced"));
			optionSettings.add(new ConfigurationOptionSetting("aws:autoscaling:launchconfiguration", "InstanceType", "t1.micro"));
			optionSettings.add(new ConfigurationOptionSetting("aws:autoscaling:launchconfiguration", "EC2KeyName", "ElasticBeanstalk"));
			optionSettings.add(new ConfigurationOptionSetting("aws:autoscaling:launchconfiguration", "IamInstanceProfile", "aws-elasticbeanstalk-ec2-role"));
			
			CreateEnvironmentRequest ceRequest = new CreateEnvironmentRequest(APPLICATION_NAME, ENVIRONMENT_NAME)
				.withOptionSettings(optionSettings)
				.withTier(new EnvironmentTier().withName("WebServer").withType("Standard").withVersion("1.0"))
				.withSolutionStackName("64bit Amazon Linux 2014.09 v1.0.8 running Tomcat 7 Java 7")
				.withCNAMEPrefix("hs2807-assignment1")
				.withVersionLabel(avDescription.getVersionLabel());
			ebClient.createEnvironment(ceRequest);
			Thread.sleep(30000);
			
			triesLeft = 20;
			while (true) {
				System.out.println("Waiting for the environment to be created (this may take a few minutes)");
				
				deRequest = new DescribeEnvironmentsRequest();
				deRequest.withApplicationName(APPLICATION_NAME).withEnvironmentNames(ENVIRONMENT_NAME);
				deResult = ebClient.describeEnvironments(deRequest);
				
				if (deResult.getEnvironments().size() > 0) {
					EnvironmentDescription environment = deResult.getEnvironments().get(0);
					if (environment.getStatus().equals(EnvironmentStatus.Ready.name())) {
						System.out.println("Environment ready!");
						System.out.println();
						break;
					}
				}
				
				if (triesLeft == 0) {
					System.out.println("Could not create environment.");
					System.exit(1);
				}
				triesLeft -= 1;
				Thread.sleep(30000);
			}
		}
		else {
			// update environment
			System.out.println("Found exiting environment");
			System.out.println("Updating environment (redeploying).");
			EnvironmentDescription environment = deResult.getEnvironments().get(0);
			UpdateEnvironmentRequest ueRequest = new UpdateEnvironmentRequest()
				.withEnvironmentName(environment.getEnvironmentName())
				.withVersionLabel(avDescription.getVersionLabel());
			ebClient.updateEnvironment(ueRequest);
			
			triesLeft = 20;
			while (true) {
				System.out.println("Waiting for the environment to update (this may take a few minutes)");
				
				deRequest = new DescribeEnvironmentsRequest();
				deRequest.withApplicationName(APPLICATION_NAME).withEnvironmentNames(ENVIRONMENT_NAME);
				deResult = ebClient.describeEnvironments(deRequest);
				
				if (deResult.getEnvironments().size() > 0) {
					EnvironmentDescription environmentDescription = deResult.getEnvironments().get(0);
					if (environmentDescription.getStatus().equals(EnvironmentStatus.Ready.name())) {
						System.out.println("Environment ready!");
						System.out.println();
						break;
					}
				}
				
				if (triesLeft == 0) {
					System.out.println("Could not update environment.");
					System.exit(1);
				}
				triesLeft -= 1;
				Thread.sleep(30000);
			}
		}
	}
}
