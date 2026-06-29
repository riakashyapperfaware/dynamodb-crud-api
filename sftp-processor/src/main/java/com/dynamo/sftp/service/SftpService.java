package com.dynamo.sftp.service;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.stream.Collectors;

@Service
public class SftpService {

    private static final Logger log = LoggerFactory.getLogger(SftpService.class);

    @Value("${sftp.host}")
    private String host;

    @Value("${sftp.port}")
    private int port;

    @Value("${sftp.username}")
    private String username;

    @Value("${sftp.password}")
    private String password;

    @Value("${sftp.remote-path}")
    private String remotePath;

    public String downloadFile() {
        Session session = null;
        ChannelSftp channel = null;

        try {
            log.info("Connecting to SFTP server: {}:{}", host, port);
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            log.info("SFTP session connected.");

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            log.info("SFTP channel opened. Downloading file: {}", remotePath);

            InputStream inputStream = channel.get(remotePath);
            String content = new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .collect(Collectors.joining("\n"));

            log.info("File downloaded successfully. Size: {} bytes", content.length());
            return content;

        } catch (Exception e) {
            log.error("SFTP download failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to download file from SFTP", e);
        } finally {
            if (channel != null) channel.disconnect();
            if (session != null) session.disconnect();
            log.info("SFTP connection closed.");
        }
    }
}
