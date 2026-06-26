package com.dynamo.api.controller;

import com.dynamo.api.config.SecretsManagerService;
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

    public ItemVariantController(ItemVariantRepository repository,
                                  SecretsManagerService secretsManagerService) {
        this.repository = repository;
        this.secretsManagerService = secretsManagerService;
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
        return ResponseEntity.ok(updated);
    }

    // DELETE
    @DeleteMapping("/{itemId}/{variantId}")
    public ResponseEntity<Void> delete(@PathVariable String itemId,
                                        @PathVariable String variantId) {
        repository.delete(itemId, variantId);
        return ResponseEntity.noContent().build();
    }
}
