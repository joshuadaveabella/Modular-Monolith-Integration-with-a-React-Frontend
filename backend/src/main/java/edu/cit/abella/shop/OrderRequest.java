package edu.cit.abella.shop;

import java.util.List;

public class OrderRequest {

    private List<LineItem> items;

    public List<LineItem> getItems() {
        return items;
    }

    public void setItems(List<LineItem> items) {
        this.items = items;
    }

    public static class LineItem {

        private String productId;
        private Integer quantity; // boxed so "missing" is distinguishable from 0

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
}
