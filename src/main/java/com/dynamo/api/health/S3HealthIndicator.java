package com.dynamo.api.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

@Component
public class S3HealthIndicator implements HealthIndicator {

    private final S3Client s3Client;
    private final String bucketName;

    public S3HealthIndicator(@Value("${aws.region}") String region,
                             @Value("${aws.s3.bucket-name}") String bucketName) {
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        this.bucketName = bucketName;
    }

    @Override
    public Health health() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            return Health.up().withDetail("service", "S3").withDetail("bucket", bucketName).build();
        } catch (Exception e) {
            return Health.down().withDetail("service", "S3").withDetail("error", e.getMessage()).build();
        }
    }
}
