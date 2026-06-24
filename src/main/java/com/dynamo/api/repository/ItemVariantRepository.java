package com.dynamo.api.repository;

import com.dynamo.api.model.ItemVariant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class ItemVariantRepository {

    private final DynamoDbTable<ItemVariant> table;

    public ItemVariantRepository(DynamoDbEnhancedClient enhancedClient,
                                  @Value("${aws.dynamodb.table-name}") String tableName) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(ItemVariant.class));
    }

    public ItemVariant save(ItemVariant itemVariant) {
        table.putItem(itemVariant);
        return itemVariant;
    }

    public ItemVariant findByItemIdAndVariantId(String itemId, String variantId) {
        Key key = Key.builder()
                .partitionValue(itemId)
                .sortValue(variantId)
                .build();
        return table.getItem(key);
    }

    public List<ItemVariant> findAll() {
        return table.scan().items().stream().collect(Collectors.toList());
    }

    public ItemVariant update(ItemVariant itemVariant) {
        table.putItem(itemVariant);
        return itemVariant;
    }

    public void delete(String itemId, String variantId) {
        Key key = Key.builder()
                .partitionValue(itemId)
                .sortValue(variantId)
                .build();
        table.deleteItem(key);
    }
}
