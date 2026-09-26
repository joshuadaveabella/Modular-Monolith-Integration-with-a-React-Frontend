package edu.cit.abella.supplier;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Required columns per the assignment: id, product_id, buyer_ref,
// request_id, po_number, cases, units, status, created_at, updated_at.
// last_error is supplementary - it's not in the required list, but keeping
// a short reason on FAILED rows is useful for the "explain any class"
// checkpoint and doesn't remove or repurpose any required column.
@Entity
@Table(name = "supplier_orders")
class SupplierOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private String productId;

    // Unique per reorder, e.g. "RO-42". Derived from this row's own id, so
    // it is stable across retries AND across app restarts - it never
    // depends on anything held only in memory.
    @Column(name = "buyer_ref", nullable = false, unique = true)
    private String buyerRef;

    // Sent as X-Request-Id on every attempt for this row, including
    // scheduled retries. Also derived from the row id for the same reason.
    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;

    @Column(name = "po_number")
    private String poNumber; // null until LegacySupply accepts the order

    @Column(nullable = false)
    private int cases;

    @Column(nullable = false)
    private int units;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupplierOrderStatus status;

    @Column(name = "last_error", length = 300)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SupplierOrderEntity() {
    }

    SupplierOrderEntity(String productId, int cases, int units) {
        this.productId = productId;
        this.cases = cases;
        this.units = units;
        this.status = SupplierOrderStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    Long getId() {
        return id;
    }

    String getProductId() {
        return productId;
    }

    String getBuyerRef() {
        return buyerRef;
    }

    void setBuyerRef(String buyerRef) {
        this.buyerRef = buyerRef;
    }

    String getRequestId() {
        return requestId;
    }

    void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    String getPoNumber() {
        return poNumber;
    }

    void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    int getCases() {
        return cases;
    }

    int getUnits() {
        return units;
    }

    SupplierOrderStatus getStatus() {
        return status;
    }

    void setStatus(SupplierOrderStatus status) {
        this.status = status;
    }

    String getLastError() {
        return lastError;
    }

    void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
