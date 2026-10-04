package edu.cit.abella.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory")
class InventoryEntity {

    @Id
    @Column(name = "product_id")
    private String productId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int stock;

    protected InventoryEntity() {
    }

    String getProductId() { return productId; }
    String getName() { return name; }
    int getStock() { return stock; }
    void setStock(int stock) { this.stock = stock; }
}
