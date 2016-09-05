/*
 * Copyright Symsoft AB 1996-2015. All Rights Reserved.
 */
package se.symsoft.codecamp.smcdr;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import se.symsoft.codecamp.myservice.logutil.RequestLoggingFilter;
import se.symsoft.codecamp.smcdr.metrics.Metrics;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;

public class SmCdrWriter extends ResourceConfig implements Runnable {

    private static final int PORT = 8020;
    static final String TABLE_NAME = "smcdr";
    private static final String SQS_QUEUE_NAME = "MyTestQueue";
    private static Regions REGION = Regions.EU_WEST_1;

    private DynamoDBMapper dynamoDB;


    public SmCdrWriter() {
        super(CdrResource.class);
        register(RequestLoggingFilter.class);
        register(JacksonJsonProvider.class);
    }

    public void run() {
        // Init DynamoDB
        dynamoDB = initDynamoDB();
       Metrics.startGraphiteMetricsReporter();
        // Start SQS receiver
        SqsMessageReceiver sqsMessageReceiver = new SqsMessageReceiver(new AmazonSQSClient().withRegion(REGION), SQS_QUEUE_NAME,dynamoDB);
        new Thread(sqsMessageReceiver).start();

        // Start HTTP server
        try {
            startHttpServer();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        //sqsMessageReceiver.stop();
    }

    private DynamoDBMapper initDynamoDB() {
        AmazonDynamoDBClient amazonDynamoDBClient = new AmazonDynamoDBClient().withRegion(REGION);
        return new DynamoDBMapper(amazonDynamoDBClient);
    }


    private void startHttpServer() throws IOException {
        URI baseUri = UriBuilder.fromUri("http://0.0.0.0").port(PORT).build();
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, this);
        server.start();
    }

    public DynamoDBMapper getDynamoDB() {
        return dynamoDB;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        new Thread(new SmCdrWriter()).start();
    }
}
