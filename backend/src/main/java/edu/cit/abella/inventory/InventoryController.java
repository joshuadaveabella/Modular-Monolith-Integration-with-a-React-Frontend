package edu.cit.abella.inventory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// The Inventory module exposes its own read endpoint rather than routing
// through the Order module - Order has no business proxying inventory data.
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @Value("${inventory.low-stock-threshold:5}")
    private int lowStockThreshold;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<?> getInventory() {
        // The threshold is returned alongside the items so the frontend
        // doesn't have to hardcode it to decide which rows to highlight.
        return ResponseEntity.ok(Map.of(
                "lowStockThreshold", lowStockThreshold,
                "items", inventoryService.getAllItems()
        ));
    }
}
