package com.dynamo.sftp.service;

import com.dynamo.sftp.model.ProductItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.List;

@Service
public class XmlGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(XmlGeneratorService.class);

    public String generate(List<ProductItem> items) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("ItemList");
            doc.appendChild(root);

            for (ProductItem item : items) {
                Element itemEl = doc.createElement("Item");
                itemEl.setAttribute("ItemID", item.getItemId());
                itemEl.setAttribute("OrganizationCode", "TEST");
                itemEl.setAttribute("UnitOfMeasure", "EACH");
                itemEl.setAttribute("Action", "Manage");

                // PrimaryInformation
                Element primaryInfo = doc.createElement("PrimaryInformation");
                primaryInfo.setAttribute("Description", item.getTitle());
                primaryInfo.setAttribute("ShortDescription", item.getTitle());
                primaryInfo.setAttribute("ExtendedDescription", "");
                primaryInfo.setAttribute("ManufacturerName", item.getArtist());
                primaryInfo.setAttribute("DefaultProductClass", "Music");
                primaryInfo.setAttribute("UnitCost", "");
                primaryInfo.setAttribute("CountryOfOrigin", item.getCountryOfOrigin());
                primaryInfo.setAttribute("IsModelItem", "Y");
                primaryInfo.setAttribute("TaxableFlag", "N");
                primaryInfo.setAttribute("ItemType", "Music");
                itemEl.appendChild(primaryInfo);

                // ClassificationCodes
                Element classification = doc.createElement("ClassificationCodes");
                classification.setAttribute("TaxProductCode", item.getTaxProductCode());
                classification.setAttribute("Model", "");
                itemEl.appendChild(classification);

                // AdditionalAttributeList
                Element attrList = doc.createElement("AdditionalAttributeList");
                attrList.appendChild(createAttribute(doc, "digital", "false"));
                attrList.appendChild(createAttribute(doc, "music", "true"));
                attrList.appendChild(createAttribute(doc, "taxcode", item.getTaxProductCode()));
                itemEl.appendChild(attrList);

                // Extn
                Element extn = doc.createElement("Extn");
                extn.setAttribute("ExtnVariantId", "");
                extn.setAttribute("ExtnSku", item.getSku());
                extn.setAttribute("ExtnInventoryItemId", "");
                extn.setAttribute("ExtnInventoryPolicy", "");
                extn.setAttribute("ExtnStreetDate", item.getStreetDate());
                extn.setAttribute("ExtnShopName", "test.store");
                extn.setAttribute("ExtnVendor", item.getVendor());
                itemEl.appendChild(extn);

                root.appendChild(itemEl);
                log.info("Generated XML element for item: {}", item.getItemId());
            }

            // Convert to String
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));

            String xml = writer.getBuffer().toString();
            log.info("XML generation complete. Total items: {}", items.size());
            return xml;

        } catch (Exception e) {
            log.error("Failed to generate XML", e);
            throw new RuntimeException("XML generation failed", e);
        }
    }

    private Element createAttribute(Document doc, String name, String value) {
        Element attr = doc.createElement("AdditionalAttribute");
        attr.setAttribute("Name", name);
        attr.setAttribute("Value", value);
        attr.setAttribute("AttributeDomainID", "ItemAttribute");
        attr.setAttribute("AttributeGroupID", "ProductMetafield");
        return attr;
    }
}
