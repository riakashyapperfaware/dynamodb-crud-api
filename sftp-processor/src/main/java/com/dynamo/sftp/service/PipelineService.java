package com.dynamo.sftp.service;

import com.dynamo.sftp.model.ProductItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PipelineService {

    private static final Logger log = LoggerFactory.getLogger(PipelineService.class);

    private final SftpService sftpService;
    private final FileParserService fileParserService;
    private final XmlGeneratorService xmlGeneratorService;
    private final S3UploaderService s3UploaderService;
    private final SqsNotifierService sqsNotifierService;

    public PipelineService(SftpService sftpService,
                           FileParserService fileParserService,
                           XmlGeneratorService xmlGeneratorService,
                           S3UploaderService s3UploaderService,
                           SqsNotifierService sqsNotifierService) {
        this.sftpService = sftpService;
        this.fileParserService = fileParserService;
        this.xmlGeneratorService = xmlGeneratorService;
        this.s3UploaderService = s3UploaderService;
        this.sqsNotifierService = sqsNotifierService;
    }

    public void execute() {
        try {
            // Step 1: Download from SFTP
            log.info("Step 1: Downloading file from SFTP...");
            String fileContent = sftpService.downloadFile();
            log.info("Step 1: Complete.");

            // Step 2: Parse flat file
            log.info("Step 2: Parsing flat file...");
            List<ProductItem> items = fileParserService.parse(fileContent);
            log.info("Step 2: Complete. Parsed {} items.", items.size());

            if (items.isEmpty()) {
                log.warn("No items parsed from file. Aborting pipeline.");
                return;
            }

            // Step 3: Generate XML
            log.info("Step 3: Generating XML...");
            String xmlContent = xmlGeneratorService.generate(items);
            log.info("Step 3: Complete.");

            // Step 4: Upload to S3
            log.info("Step 4: Uploading XML to S3...");
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "PROD_OUTPUT_" + timestamp + ".xml";
            String s3Key = s3UploaderService.upload(xmlContent, fileName);
            log.info("Step 4: Complete. S3 key: {}", s3Key);

            // Step 5: Send SQS notification
            log.info("Step 5: Sending SQS notification...");
            sqsNotifierService.notify(s3Key, items.size());
            log.info("Step 5: Complete.");

            log.info("Pipeline executed successfully. {} items processed.", items.size());

        } catch (Exception e) {
            log.error("Pipeline failed: {}", e.getMessage(), e);
            throw new RuntimeException("Pipeline execution failed", e);
        }
    }
}
