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

public class DlqProcessor implements RequestHandler<SQSEvent, Void> {

    private final ItemVariantRepository repository;
    private final ObjectMapper objectMapper;

    public DlqProcessor() {
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
            String body = message.getBody();
            context.getLogger().log("DLQ received failed message: " + body);

            try {
                JsonNode root = objectMapper.readTree(body);
                JsonNode payloadNode = root.has("payload") ? root.get("payload") : root;
                ItemVariant itemVariant = objectMapper.treeToValue(payloadNode, ItemVariant.class);

                if (itemVariant.getItemId() == null || itemVariant.getVariantId() == null) {
                    context.getLogger().log("DLQ: message is unrecoverable — missing itemId or variantId. Skipping.");
                    continue;
                }

                repository.save(itemVariant);
                context.getLogger().log("DLQ: successfully reprocessed item: " + itemVariant.getItemId() + "/" + itemVariant.getVariantId());

            } catch (Exception e) {
                context.getLogger().log("DLQ: could not reprocess message, discarding. Reason: " + e.getMessage());
            }
        }
        return null;
    }
}
