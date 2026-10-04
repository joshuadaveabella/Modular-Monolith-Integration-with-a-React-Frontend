package edu.cit.abella.channel;

class ListingRequest {
    public String sellerSku;   // our own productId, e.g. "P100"
    public String title;
    public String supplierSku; // from Lab 3's supplier_sku_mapping

    ListingRequest(String sellerSku, String title, String supplierSku) {
        this.sellerSku = sellerSku;
        this.title = title;
        this.supplierSku = supplierSku;
    }
}
