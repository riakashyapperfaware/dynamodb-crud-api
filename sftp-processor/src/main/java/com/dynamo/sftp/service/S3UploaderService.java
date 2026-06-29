package com.dynamo.sftp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3UploaderService {

    private static final Logger log = LoggerFactory.getLogger(S3UploaderService.class);

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.key-prefix}")
    private String keyPrefix;

    public S3UploaderService(@Value("${aws.region}") String awsRegion) {
        this.s3Client = S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public String upload(String xmlContent, String fileName) {
        String key = keyPrefix + "/" + fileName;

        try {
            log.info("Uploading XML to S3 bucket: {}, key: {}", bucketName, key);

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("application/xml")
                    .build();

            s3Client.putObject(request, RequestBody.fromString(xmlContent));

            log.info("XML uploaded successfully to S3: {}/{}", bucketName, key);
            return key;

        } catch (Exception e) {
            log.error("S3 upload failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload XML to S3", e);
        }
    }
}
