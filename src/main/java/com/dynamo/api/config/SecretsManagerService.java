package com.dynamo.api.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

@Service
public class SecretsManagerService {

    private final SecretsManagerClient secretsManagerClient;
    private final ObjectMapper objectMapper;

    public SecretsManagerService(@Value("${aws.region}") String awsRegion) {
        this.secretsManagerClient = SecretsManagerClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String getSecret(String secretName) {
        GetSecretValueRequest request = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();
        return secretsManagerClient.getSecretValue(request).secretString();
    }

    public String getSecretValue(String secretName, String key) {
        try {
            String secretString = getSecret(secretName);
            JsonNode jsonNode = objectMapper.readTree(secretString);
            return jsonNode.get(key).asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve secret key: " + key, e);
        }
    }
}
