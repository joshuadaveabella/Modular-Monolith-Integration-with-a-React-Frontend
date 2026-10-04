package edu.cit.abella.supplier;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "supplier_orders")
class SupplierOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "buyer_ref", nullable = false, unique = true)
    private String buyerRef;

    @Column(name = "request_id", nullable = false, unique = true)
    private String requestId;

    @Column(name = "po_number")
    private String poNumber;

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
        // Generated here, before the row even exists, so a single save() has
        // everything it needs - no dependency on a generated id.
        this.buyerRef = "RO-" + java.util.UUID.randomUUID();
        this.requestId = "req-" + java.util.UUID.randomUUID();
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

    Long getId() { return id; }
    String getProductId() { return productId; }
    String getBuyerRef() { return buyerRef; }
    void setBuyerRef(String buyerRef) { this.buyerRef = buyerRef; }
    String getRequestId() { return requestId; }
    void setRequestId(String requestId) { this.requestId = requestId; }
    String getPoNumber() { return poNumber; }
    void setPoNumber(String poNumber) { this.poNumber = poNumber; }
    int getCases() { return cases; }
    int getUnits() { return units; }
    SupplierOrderStatus getStatus() { return status; }
    void setStatus(SupplierOrderStatus status) { this.status = status; }
    String getLastError() { return lastError; }
    void setLastError(String lastError) { this.lastError = lastError; }
}
