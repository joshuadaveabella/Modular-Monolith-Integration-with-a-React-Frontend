package edu.cit.abella.supplier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "supplier_sku_mapping")
class SupplierSkuMappingEntity {

    @Id
    @Column(name = "product_id")
    private String productId;

    @Column(name = "supplier_sku", nullable = false)
    private String supplierSku;

    @Column(name = "pack_size", nullable = false)
    private int packSize;

    protected SupplierSkuMappingEntity() {
    }

    String getProductId() { return productId; }
    String getSupplierSku() { return supplierSku; }
    int getPackSize() { return packSize; }
}
