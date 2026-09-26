package edu.cit.abella.supplier;

// Our own lifecycle, deliberately not a copy of LegacySupply's StatusCode
// values (10/20/30/40). A future supplier swap only needs a new mapping
// into this enum, not a schema change.
public enum SupplierOrderStatus {
    PENDING,        // queued locally, not yet accepted by the supplier
    SUBMITTED,      // sent, accepted (LegacySupply StatusCode 10)
    IN_PROGRESS,    // being picked (StatusCode 20)
    SHIPPED,        // (StatusCode 30)
    DELIVERED,      // (StatusCode 40) - triggers restock
    FAILED,         // a non-transient error; will not be retried automatically
    UNKNOWN         // an order status code we didn't expect - see INTEGRATION.md
}
