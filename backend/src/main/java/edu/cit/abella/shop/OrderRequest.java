package edu.cit.abella.shop;

public class OrderRequest {

    private String productId;
    private Integer quantity; // boxed so we can tell "missing" apart from 0

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
