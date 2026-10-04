package edu.cit.abella.supplier;

public class SupplierOrderResult {

    private final Long supplierOrderId;
    private final String buyerRef;
    private final String poNumber;
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

    public Long getSupplierOrderId() { return supplierOrderId; }
    public String getBuyerRef() { return buyerRef; }
    public String getPoNumber() { return poNumber; }
    public SupplierOrderStatus getStatus() { return status; }
    public int getCases() { return cases; }
    public int getUnits() { return units; }
}
