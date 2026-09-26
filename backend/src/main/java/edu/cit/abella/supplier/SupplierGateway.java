package edu.cit.abella.supplier;

// The module boundary. Inventory's AutoReorderListener depends on this
// interface and SupplierOrderResult/SupplierOrderStatus - nothing else in
// this package. Everything below this line in the package (XML classes,
// the HTTP client, session handling, the SKU mapping, the entity, the
// scheduled jobs) is package-private.
public interface SupplierGateway {

    // Takes OUR terms - our product id and how many of OUR units we need -
    // and returns our own result type. The unit -> case conversion and the
    // SupplierSku lookup both happen inside the implementation.
    SupplierOrderResult placeReorder(String productId, int unitsNeeded);
}
