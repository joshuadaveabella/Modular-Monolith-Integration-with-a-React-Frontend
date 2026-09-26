package edu.cit.abella.events;

// Published by the supplier module's OrderStatusPoller when a purchase
// order transitions to Delivered. Deliberately generic - it carries only
// our own productId and a plain unit count, nothing from LegacySupply's
// vocabulary (no StatusCode, no SupplierSku, no Uom). This is what lets
// Inventory restock without ever importing anything from the supplier
// package: Inventory only needs to know "this many units of this product
// arrived," not who shipped them or how.
public class SupplierOrderDeliveredEvent {

    private final Long supplierOrderId;
    private final String productId;
    private final int unitsDelivered;
    private final String poNumber;

    public SupplierOrderDeliveredEvent(Long supplierOrderId, String productId,
                                       int unitsDelivered, String poNumber) {
        this.supplierOrderId = supplierOrderId;
        this.productId = productId;
        this.unitsDelivered = unitsDelivered;
        this.poNumber = poNumber;
    }

    public Long getSupplierOrderId() {
        return supplierOrderId;
    }

    public String getProductId() {
        return productId;
    }

    public int getUnitsDelivered() {
        return unitsDelivered;
    }

    public String getPoNumber() {
        return poNumber;
    }
}
