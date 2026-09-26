package edu.cit.abella.supplier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Maps OUR productId to LegacySupply's SupplierSku + PackSize. This table is
// the ONLY place a SupplierSku is allowed to exist outside this package -
// and it never leaves this package either, since it's package-private.
// Populate it with values from your own GET /catalog probe (see sql/schema.sql
// and INTEGRATION.md) - the seed values here are placeholders and will not
// work against your account's real catalog.
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

    String getProductId() {
        return productId;
    }

    String getSupplierSku() {
        return supplierSku;
    }

    int getPackSize() {
        return packSize;
    }
}
