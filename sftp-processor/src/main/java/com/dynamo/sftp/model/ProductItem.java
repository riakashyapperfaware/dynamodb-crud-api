package com.dynamo.sftp.model;

public class ProductItem {

    private String itemId;
    private String sku;
    private String artist;
    private String title;
    private String streetDate;
    private String countryOfOrigin;
    private String taxProductCode;
    private String vendor;

    public ProductItem() {}

    public ProductItem(String itemId, String sku, String artist, String title,
                       String streetDate, String countryOfOrigin,
                       String taxProductCode, String vendor) {
        this.itemId = itemId;
        this.sku = sku;
        this.artist = artist;
        this.title = title;
        this.streetDate = streetDate;
        this.countryOfOrigin = countryOfOrigin;
        this.taxProductCode = taxProductCode;
        this.vendor = vendor;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStreetDate() { return streetDate; }
    public void setStreetDate(String streetDate) { this.streetDate = streetDate; }

    public String getCountryOfOrigin() { return countryOfOrigin; }
    public void setCountryOfOrigin(String countryOfOrigin) { this.countryOfOrigin = countryOfOrigin; }

    public String getTaxProductCode() { return taxProductCode; }
    public void setTaxProductCode(String taxProductCode) { this.taxProductCode = taxProductCode; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }
}
