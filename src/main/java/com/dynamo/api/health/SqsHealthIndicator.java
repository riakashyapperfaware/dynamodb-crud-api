package com.dynamo.api.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

@Component
public class SqsHealthIndicator implements HealthIndicator {

    private final SqsClient sqsClient;
    private final String queueUrl;

    public SqsHealthIndicator(@Value("${aws.region}") String region,
                              @Value("${aws.sqs.queue-url}") String queueUrl) {
        this.sqsClient = SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        this.queueUrl = queueUrl;
    }

    @Override
    public Health health() {
        try {
            sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl)
                    .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
                    .build());
            return Health.up().withDetail("service", "SQS").withDetail("queue", queueUrl).build();
        } catch (Exception e) {
            return Health.down().withDetail("service", "SQS").withDetail("error", e.getMessage()).build();
        }
    }
}
