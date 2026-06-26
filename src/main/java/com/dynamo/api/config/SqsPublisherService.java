package com.dynamo.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;

@Service
public class SqsPublisherService {

    private final SqsClient sqsClient;
    private final String queueUrl;
    private final ObjectMapper objectMapper;

    public SqsPublisherService(@Value("${aws.region}") String awsRegion,
                                @Value("${aws.sqs.queue-url}") String queueUrl) {
        this.sqsClient = SqsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        this.queueUrl = queueUrl;
        this.objectMapper = new ObjectMapper();
    }

    public void publishEvent(String eventType, Object payload) {
        try {
            Map<String, Object> event = Map.of(
                    "eventType", eventType,
                    "timestamp", System.currentTimeMillis(),
                    "payload", payload
            );
            String messageBody = objectMapper.writeValueAsString(event);
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build();
            sqsClient.sendMessage(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish event: " + eventType, e);
        }
    }
}
