package com.dynamo.api.controller;

import com.dynamo.api.config.S3Service;
import com.dynamo.api.config.SecretsManagerService;
import com.dynamo.api.config.SqsPublisherService;
import com.dynamo.api.model.ItemVariant;
import com.dynamo.api.repository.ItemVariantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/item-variants")
public class ItemVariantController {

    private final ItemVariantRepository repository;
    private final SecretsManagerService secretsManagerService;
    private final S3Service s3Service;
    private final SqsPublisherService sqsPublisherService;

    public ItemVariantController(ItemVariantRepository repository,
                                  SecretsManagerService secretsManagerService,
                                  S3Service s3Service,
                                  SqsPublisherService sqsPublisherService) {
        this.repository = repository;
        this.secretsManagerService = secretsManagerService;
        this.s3Service = s3Service;
        this.sqsPublisherService = sqsPublisherService;
    }

    // SECRETS MANAGER - fetch API key at runtime
    @GetMapping("/config/api-key")
    public ResponseEntity<String> getApiKey() {
        String apiKey = secretsManagerService.getSecretValue("item-variant-api/config", "apiKey");
        return ResponseEntity.ok("API Key fetched from Secrets Manager: " + apiKey);
    }

    // CREATE
    @PostMapping
    public ResponseEntity<ItemVariant> create(@RequestBody ItemVariant itemVariant) {
        ItemVariant saved = repository.save(itemVariant);
        sqsPublisherService.publishEvent("ProductCreatedEvent", saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<ItemVariant>> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    // READ ONE
    @GetMapping("/{itemId}/{variantId}")
    public ResponseEntity<ItemVariant> getOne(@PathVariable String itemId,
                                               @PathVariable String variantId) {
        ItemVariant item = repository.findByItemIdAndVariantId(itemId, variantId);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    // UPDATE
    @PutMapping("/{itemId}/{variantId}")
    public ResponseEntity<ItemVariant> update(@PathVariable String itemId,
                                               @PathVariable String variantId,
                                               @RequestBody ItemVariant itemVariant) {
        itemVariant.setItemId(itemId);
        itemVariant.setVariantId(variantId);
        ItemVariant updated = repository.update(itemVariant);
        sqsPublisherService.publishEvent("ProductUpdatedEvent", updated);
        return ResponseEntity.ok(updated);
    }

    // DELETE
    @DeleteMapping("/{itemId}/{variantId}")
    public ResponseEntity<Void> delete(@PathVariable String itemId,
                                        @PathVariable String variantId) {
        repository.delete(itemId, variantId);
        return ResponseEntity.noContent().build();
    }

    // S3 - Upload product details
    @PostMapping("/{itemId}/{variantId}/details")
    public ResponseEntity<String> uploadDetails(@PathVariable String itemId,
                                                 @PathVariable String variantId,
                                                 @RequestBody String content) {
        s3Service.uploadDetails(itemId, variantId, content);
        return ResponseEntity.status(HttpStatus.CREATED).body("Details uploaded to S3 for " + itemId + "/" + variantId);
    }

    // S3 - Get pre-signed URL to access product details
    @GetMapping("/{itemId}/{variantId}/details")
    public ResponseEntity<String> getDetailsUrl(@PathVariable String itemId,
                                                 @PathVariable String variantId) {
        String url = s3Service.generatePresignedUrl(itemId, variantId);
        return ResponseEntity.ok(url);
    }
}
