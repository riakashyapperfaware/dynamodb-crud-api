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
            log.info("Generating XML for {} items", items.size());

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

                Element primary = doc.createElement("PrimaryInformation");
                primary.setAttribute("Description", item.getTitle());
                primary.setAttribute("ShortDescription", item.getTitle());
                primary.setAttribute("ExtendedDescription", "");
                primary.setAttribute("ManufacturerName", item.getArtist());
                primary.setAttribute("DefaultProductClass", "Music");
                primary.setAttribute("UnitCost", "");
                primary.setAttribute("CountryOfOrigin", item.getCountryOfOrigin());
                primary.setAttribute("IsModelItem", "Y");
                primary.setAttribute("TaxableFlag", "N");
                primary.setAttribute("ItemType", "Music");
                itemEl.appendChild(primary);

                Element classification = doc.createElement("ClassificationCodes");
                classification.setAttribute("TaxProductCode", item.getTaxProductCode());
                classification.setAttribute("Model", "");
                itemEl.appendChild(classification);

                Element attrList = doc.createElement("AdditionalAttributeList");
                attrList.appendChild(createAttribute(doc, "digital", "false"));
                attrList.appendChild(createAttribute(doc, "music", "true"));
                attrList.appendChild(createAttribute(doc, "taxcode", item.getTaxProductCode()));
                itemEl.appendChild(attrList);

                Element extn = doc.createElement("Extn");
                extn.setAttribute("ExtnVariantId", "");
                extn.setAttribute("ExtnSku", item.getSku());
                extn.setAttribute("ExtnInventoryItemId", "");
                extn.setAttribute("ExtnInventoryPolicy", "");
                extn.setAttribute("ExtnStreetDate", item.getStreetDate());
                extn.setAttribute("ExtnShopName", "test.store");
                extn.setAttribute("ExtnVendor", item.getArtist());
                itemEl.appendChild(extn);

                root.appendChild(itemEl);
                log.info("Added item to XML: {}", item.getItemId());
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));

            String xml = writer.toString();
            log.info("XML generation complete. Size: {} bytes", xml.length());
            return xml;

        } catch (Exception e) {
            log.error("XML generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate XML", e);
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
