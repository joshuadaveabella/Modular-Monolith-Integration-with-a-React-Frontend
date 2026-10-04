package edu.cit.abella.events;

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

    public Long getSupplierOrderId() { return supplierOrderId; }
    public String getProductId() { return productId; }
    public int getUnitsDelivered() { return unitsDelivered; }
    public String getPoNumber() { return poNumber; }
}
