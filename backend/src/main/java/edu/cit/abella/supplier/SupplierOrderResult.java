package edu.cit.abella.supplier;

// Returned by SupplierGateway.placeReorder(). Note there is no SupplierSku,
// no StatusCode, no Uom here - only our own supplier_orders row id, our own
// enum, and the case/unit counts we calculated ourselves.
public class SupplierOrderResult {

    private final Long supplierOrderId;
    private final String buyerRef;
    private final String poNumber; // null until the supplier actually accepts it
    private final SupplierOrderStatus status;
    private final int cases;
    private final int units;

    public SupplierOrderResult(Long supplierOrderId, String buyerRef, String poNumber,
                               SupplierOrderStatus status, int cases, int units) {
        this.supplierOrderId = supplierOrderId;
        this.buyerRef = buyerRef;
        this.poNumber = poNumber;
        this.status = status;
        this.cases = cases;
        this.units = units;
    }

    public Long getSupplierOrderId() {
        return supplierOrderId;
    }

    public String getBuyerRef() {
        return buyerRef;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public SupplierOrderStatus getStatus() {
        return status;
    }

    public int getCases() {
        return cases;
    }

    public int getUnits() {
        return units;
    }
}
