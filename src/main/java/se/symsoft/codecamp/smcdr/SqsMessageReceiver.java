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
            	QueueItem item = Jackson.fromJsonString(message.getBody(), QueueItem.class);
            	SmcdrData smcdrData = new SmcdrData();
            	smcdrData.setOriginator(item.getOriginator());
            	smcdrData.setDestination(item.getDestination());
            	smcdrData.setDatetime(new Date(System.currentTimeMillis()).toString());
            	smcdrData.setUserData(item.getUserData());
            	
            	dynamoDB.save(smcdrData);
            	Metrics.METRIC_REGISTRY.counter("smcdr").inc();
            	
            	
                System.out.println("    Body:          " + message.getBody());
                // We must delete the message from the queue
                sqs.deleteMessage(new DeleteMessageRequest().withQueueUrl(queueName).withReceiptHandle(message.getReceiptHandle()));
            }

        }
    }

    public void stop() {
        running = false;
    }
}
