package edu.cit.abella.supplier;

import java.util.Optional;

// The module boundary. Two methods added for Lab 4:
//
// hasOpenOrder() lets OrderService (shop module) decide BACKORDERED vs
// REJECTED without ever seeing a SupplierSku or a LegacySupply status
// code - just a boolean "is something already on the way."
//
// getSupplierSkuFor() exists because Tiangge's own contract requires a
// listing to name the LegacySupply SupplierSku it restocks from - the
// channel module needs this value by design, not because a boundary was
// loosened for convenience.
public interface SupplierGateway {

    SupplierOrderResult placeReorder(String productId, int unitsNeeded);

    boolean hasOpenOrder(String productId);

    // New: total units still incoming across all open purchase orders for
    // this product - used to check whether there's actually enough on the
    // way to cover ANOTHER backorder, not just whether one exists.
    int getOpenOrderUnits(String productId);

    Optional<String> getSupplierSkuFor(String productId);
}

