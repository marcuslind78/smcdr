/*
 * Copyright Symsoft AB 1996-2015. All Rights Reserved.
 */
package se.symsoft.codecamp.smcdr;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.util.json.Jackson;


import se.symsoft.codecamp.smcdr.metrics.Metrics;

import java.util.Date;
import java.util.List;

public class SqsMessageReceiver implements Runnable {
    private static final String SMCDR = "smcdr";
    private final AmazonSQSClient sqs;
    private final String queueName;
    private boolean running;
	private DynamoDBMapper dynamoDB;

    public SqsMessageReceiver(AmazonSQSClient sqs, String queueName, DynamoDBMapper dynamoDB) {
        this.sqs = sqs;
        this.queueName = queueName;
        this.dynamoDB = dynamoDB;
    }

    public void run() {
        running = true;
        while(running) {
            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueName).
                withMaxNumberOfMessages(10).withWaitTimeSeconds(10);
            List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();

            for (Message message : messages) {
                System.out.println("Received message with: " + message.getBody());
                QueueItem item = Jackson.fromJsonString(message.getBody(), QueueItem.class);
            	SmCdrData smCdrData = new SmCdrData();
            	smCdrData.setOriginator(item.getOriginator());
            	smCdrData.setDestination(item.getDestination());
            	smCdrData.setDatetime(new Date(System.currentTimeMillis()).toString());
            	smCdrData.setUserData(item.getUserData());
            	
            	dynamoDB.save(smCdrData);
            	Metrics.METRIC_REGISTRY.counter(SMCDR).inc();
            	
                // We must delete the message from the queue
                sqs.deleteMessage(new DeleteMessageRequest().withQueueUrl(queueName).withReceiptHandle(message.getReceiptHandle()));
            }
        }
    }

    public void stop() {
        running = false;
    }
}
