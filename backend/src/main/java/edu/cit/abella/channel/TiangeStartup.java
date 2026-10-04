package edu.cit.abella.channel;

import edu.cit.abella.inventory.InventoryItem;
import edu.cit.abella.inventory.InventoryService;
import edu.cit.abella.supplier.SupplierGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// Task 1 requires the first heartbeat to go out "before any other call."
// Task 2 requires listings before the shop is "live." This class enforces
// that order explicitly, rather than relying on @Scheduled timing, which
// gives no such guarantee.
@Component
class TiangeStartup {

    private static final Logger log = LoggerFactory.getLogger(TiangeStartup.class);

    // The three products seeded since Lab 1. Hardcoded rather than
    // configurable - matches the assignment's "publish at least 3 of your
    // products" and this project's existing seed data.
    private static final List<String> LISTED_PRODUCTS = List.of("P100", "P200", "P300");

    private final TiangeHttpClient httpClient;
    private final TiangeProperties properties;
    private final InventoryService inventoryService;
    private final SupplierGateway supplierGateway;

    TiangeStartup(TiangeHttpClient httpClient, TiangeProperties properties,
                 InventoryService inventoryService, SupplierGateway supplierGateway) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.inventoryService = inventoryService;
        this.supplierGateway = supplierGateway;
    }

    @EventListener(ApplicationReadyEvent.class)
    void onReady() {
        sendFirstHeartbeat();
        List<ListingRequest> listings = publishListings();
        publishInitialStock(listings);
    }

    private void sendFirstHeartbeat() {
        try {
            ChannelRetrying.call(properties.maxAttempts, () -> {
                httpClient.heartbeat();
                return null;
            });
            log.info("First heartbeat sent");
        } catch (Exception e) {
            log.error("First heartbeat failed - self-check may not show 'App online' yet: {}", e.getMessage());
        }
    }

    private List<ListingRequest> publishListings() {
        List<ListingRequest> listings = new ArrayList<>();

        for (String productId : LISTED_PRODUCTS) {
            InventoryItem item;
            try {
                item = inventoryService.getItem(productId);
            } catch (Exception e) {
                log.warn("Skipping Tiangge listing for {} - not found in inventory", productId);
                continue;
            }

            String supplierSku = supplierGateway.getSupplierSkuFor(productId).orElse(null);
            if (supplierSku == null) {
                log.warn("Skipping Tiangge listing for {} - no row in supplier_sku_mapping (see INTEGRATION.md)",
                        productId);
                continue;
            }

            listings.add(new ListingRequest(productId, item.getName(), supplierSku));
        }

        if (listings.isEmpty()) {
            log.error("No Tiangge listings could be published - check supplier_sku_mapping before going live");
            return listings;
        }

        try {
            ChannelRetrying.call(properties.maxAttempts, () -> {
                httpClient.publishListings(listings);
                return null;
            });
            log.info("Published {} listing(s) to Tiangge", listings.size());
        } catch (Exception e) {
            log.error("Failed to publish listings: {}", e.getMessage());
        }

        return listings;
    }

    private void publishInitialStock(List<ListingRequest> listings) {
        if (listings.isEmpty()) {
            return;
        }

        List<StockUpdateRequest> updates = listings.stream()
                .map(l -> new StockUpdateRequest(l.sellerSku, inventoryService.getItem(l.sellerSku).getStock()))
                .toList();

        try {
            ChannelRetrying.call(properties.maxAttempts, () -> {
                httpClient.publishStock(updates);
                return null;
            });
            log.info("Published initial stock for {} product(s)", updates.size());
        } catch (Exception e) {
            log.error("Failed to publish initial stock: {}", e.getMessage());
        }
    }
}
