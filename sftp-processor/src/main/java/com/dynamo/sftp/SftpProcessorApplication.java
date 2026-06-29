package com.dynamo.sftp;

import com.dynamo.sftp.service.PipelineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SftpProcessorApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SftpProcessorApplication.class);

    private final PipelineService pipelineService;

    public SftpProcessorApplication(PipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    public static void main(String[] args) {
        SpringApplication.run(SftpProcessorApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("Starting SFTP processing pipeline...");
        pipelineService.execute();
        log.info("Pipeline completed.");
    }
}
