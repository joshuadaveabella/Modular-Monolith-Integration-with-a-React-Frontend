package edu.cit.abella.channel;

class StockUpdateRequest {
    public String sellerSku;
    public int available;

    StockUpdateRequest(String sellerSku, int available) {
        this.sellerSku = sellerSku;
        this.available = available;
    }
}
