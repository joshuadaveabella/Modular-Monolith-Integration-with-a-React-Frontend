package edu.cit.abella.inventory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
        return ResponseEntity.ok(Map.of(
                "lowStockThreshold", lowStockThreshold,
                "items", inventoryService.getAllItems()
        ));
    }
}
