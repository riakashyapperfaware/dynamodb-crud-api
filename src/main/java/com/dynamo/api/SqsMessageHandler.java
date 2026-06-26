package com.dynamo.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.dynamo.api.model.ItemVariant;
import com.dynamo.api.repository.ItemVariantRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class SqsMessageHandler implements RequestHandler<SQSEvent, Void> {

    private final ItemVariantRepository repository;
    private final ObjectMapper objectMapper;

    public SqsMessageHandler() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
                .region(Region.of(System.getenv("AWS_REGION") != null ? System.getenv("AWS_REGION") : "us-east-1"))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();

        this.repository = new ItemVariantRepository(enhancedClient, "ItemVariants");
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        for (SQSEvent.SQSMessage message : event.getRecords()) {
            try {
                String body = message.getBody();
                context.getLogger().log("Processing message: " + body);

                JsonNode root = objectMapper.readTree(body);
                String eventType = root.has("eventType") ? root.get("eventType").asText() : "UNKNOWN";
                context.getLogger().log("Event type: " + eventType);

                JsonNode payloadNode = root.has("payload") ? root.get("payload") : root;
                ItemVariant itemVariant = objectMapper.treeToValue(payloadNode, ItemVariant.class);
                repository.save(itemVariant);

                context.getLogger().log("Saved item: " + itemVariant.getItemId() + "/" + itemVariant.getVariantId());
            } catch (Exception e) {
                context.getLogger().log("Error processing message: " + e.getMessage());
                throw new RuntimeException("Failed to process SQS message", e);
            }
        }
        return null;
    }
}
