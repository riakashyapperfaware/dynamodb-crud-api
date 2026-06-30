package com.dynamo.sftp.service;

import com.dynamo.sftp.model.ProductItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FileParserService {

    private static final Logger log = LoggerFactory.getLogger(FileParserService.class);
    private static final String DELIMITER = "\\|";
    private static final String DATA_ROW_PREFIX = "N";

    public List<ProductItem> parse(String fileContent) {
        List<ProductItem> items = new ArrayList<>();
        String[] lines = fileContent.split("\n");

        log.info("Parsing file with {} lines", lines.length);

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || !line.startsWith(DATA_ROW_PREFIX)) {
                log.info("Skipping line: {}", line);
                continue;
            }

            try {
                String[] fields = line.split(DELIMITER, -1);

                // Field positions from actual PROD_INPUT.TXT:
                // 0=N, 1=PROD, 2=TESTUS, 3=date, 4=itemId, 5=sku,
                // 6=artist, 7=title, 8=qty, 9=streetDate, 10=empty, 11=P, 12=country,
                // 13=format, 14=?, 15=., 16=002, 17=empty, 18=taxcode, 19=code, 20=vendor
                String itemId   = fields.length > 4  ? fields[4].trim()  : "";
                String sku      = fields.length > 5  ? fields[5].trim()  : "";
                String artist   = fields.length > 6  ? fields[6].trim()  : "";
                String title    = fields.length > 7  ? fields[7].trim()  : "";
                String streetDate = fields.length > 9  ? fields[9].trim()  : "";
                String country  = fields.length > 12 ? fields[12].trim() : "";
                String taxcode  = fields.length > 18 ? fields[18].trim() : "";
                String vendor   = fields.length > 20 ? fields[20].trim() : "";

                ProductItem item = new ProductItem(itemId, sku, artist, title,
                        streetDate, country, taxcode, vendor);
                items.add(item);

                log.info("Parsed item: itemId={}, title={}", itemId, title);

            } catch (Exception e) {
                log.error("Failed to parse line: {}", line, e);
            }
        }

        log.info("Parsing complete. Total items parsed: {}", items.size());
        return items;
    }
}
