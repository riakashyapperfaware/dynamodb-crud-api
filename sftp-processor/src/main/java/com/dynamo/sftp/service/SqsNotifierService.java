package com.dynamo.sftp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
public class SqsNotifierService {

    private static final Logger log = LoggerFactory.getLogger(SqsNotifierService.class);

    private final SqsClient sqsClient;

    @Value("${aws.sqs.queue-url}")
    private String queueUrl;

    public SqsNotifierService(@Value("${aws.region}") String awsRegion) {
        this.sqsClient = SqsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public void notify(String s3Key, int itemCount) {
        try {
            String message = String.format(
                "{\"event\":\"XML_UPLOADED\",\"s3Key\":\"%s\",\"itemCount\":%d,\"timestamp\":%d}",
                s3Key, itemCount, System.currentTimeMillis()
            );

            log.info("Sending SQS notification for uploaded file: {}", s3Key);

            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(message)
                    .build();

            sqsClient.sendMessage(request);
            log.info("SQS notification sent successfully. Message: {}", message);

        } catch (Exception e) {
            log.error("SQS notification failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send SQS notification", e);
        }
    }
}
