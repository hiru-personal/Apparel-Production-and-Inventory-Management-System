package org.example.pim_system.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.pim_system.model.InventoryItem;
import org.example.pim_system.repository.InventoryItemRepository;
import org.example.pim_system.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private AuditLogService auditLogService;

    @PostMapping("/add-item")
    public ResponseEntity<Map<String, Object>> addInventoryItem(@RequestBody Map<String, String> itemData,
                                                                  HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            InventoryItem item = new InventoryItem();
            item.setItemName(itemData.get("itemName"));
            item.setCategory(itemData.get("category"));
            item.setCurrentStock(Integer.parseInt(itemData.get("initialQuantity")));
            item.setMinimumStockLevel(Integer.parseInt(itemData.get("minimumStockLevel")));
            item.setUnit(itemData.get("unit"));

            String itemCode = saveWithUniqueCode(item);

            auditLogService.log(
                    "INVENTORY_ITEM_CREATED",
                    String.format("Added new inventory item: Code=%s, Name=%s, Category=%s, Initial Stock=%s %s, Min Level=%s",
                            itemCode, itemData.get("itemName"), itemData.get("category"),
                            itemData.get("initialQuantity"), itemData.get("unit"),
                            itemData.get("minimumStockLevel")),
                    request
            );
            
            response.put("success", true);
            response.put("message", "Item added to inventory successfully! Item Code: " + itemCode);
            response.put("itemCode", itemCode);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error adding item to inventory: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/items")
    public ResponseEntity<List<InventoryItem>> getAllItems() {
        List<InventoryItem> items = inventoryItemRepository.findAllByOrderByItemNameAsc();
        return ResponseEntity.ok(items);
    }
    
    @PutMapping("/update-stock")
    public ResponseEntity<Map<String, Object>> updateStock(@RequestBody Map<String, String> stockData,
                                                             HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String itemCode = stockData.get("itemCode");
            String transactionType = stockData.get("transactionType");
            Integer quantity = Integer.parseInt(stockData.get("quantity"));
            // String reason = stockData.get("reason"); // Reserved for future audit logging
            
            InventoryItem item = inventoryItemRepository.findByItemCode(itemCode)
                    .orElseThrow(() -> new RuntimeException("Item not found with code: " + itemCode));
            
            int oldStock = item.getCurrentStock();
            int newStock;
            
            if ("add".equalsIgnoreCase(transactionType)) {
                newStock = oldStock + quantity;
            } else if ("remove".equalsIgnoreCase(transactionType)) {
                newStock = oldStock - quantity;
                if (newStock < 0) {
                    response.put("success", false);
                    response.put("message", "Cannot remove more stock than available. Current stock: " + oldStock);
                    return ResponseEntity.badRequest().body(response);
                }
            } else {
                response.put("success", false);
                response.put("message", "Invalid transaction type. Use 'add' or 'remove'.");
                return ResponseEntity.badRequest().body(response);
            }
            
            item.setCurrentStock(newStock);
            inventoryItemRepository.save(item);

            auditLogService.log(
                    "INVENTORY_STOCK_UPDATED",
                    String.format("Stock updated for item %s (%s): %s %d %s. Stock changed from %d to %d",
                            itemCode, item.getItemName(),
                            "add".equalsIgnoreCase(transactionType) ? "Added" : "Removed",
                            quantity, item.getUnit(), oldStock, newStock),
                    request
            );
            
            response.put("success", true);
            response.put("message", String.format("Stock updated successfully! %s %d %s. New stock: %d %s", 
                transactionType.equalsIgnoreCase("add") ? "Added" : "Removed", 
                quantity, item.getUnit(), newStock, item.getUnit()));
            response.put("oldStock", oldStock);
            response.put("newStock", newStock);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating stock: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/item/{itemCode}")
    public ResponseEntity<InventoryItem> getItemByCode(@PathVariable String itemCode) {
        return inventoryItemRepository.findByItemCode(itemCode)
                .map(item -> ResponseEntity.ok(item))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/item/{id}")
    public ResponseEntity<Map<String, Object>> deleteItem(@PathVariable Long id,
                                                            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            InventoryItem item = inventoryItemRepository.findById(id).orElse(null);
            if (item == null) {
                response.put("success", false);
                response.put("message", "Item not found!");
                return ResponseEntity.status(404).body(response);
            }

            String itemCode = item.getItemCode();
            String itemName = item.getItemName();
            
            inventoryItemRepository.deleteById(id);

            auditLogService.log(
                    "INVENTORY_ITEM_DELETED",
                    String.format("Deleted inventory item: Code=%s, Name=%s, Category=%s, Last Stock=%d",
                            itemCode, itemName, item.getCategory(), item.getCurrentStock()),
                    request
            );
            
            response.put("success", true);
            response.put("message", "Item deleted successfully!");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting item: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    private String saveWithUniqueCode(InventoryItem item) {
        int nextNumber = getNextItemNumber();
        for (int attempt = 0; attempt < 100; attempt++) {
            String itemCode = "ITM-" + String.format("%03d", nextNumber);

            // Fast-path check before save
            if (inventoryItemRepository.findByItemCode(itemCode).isPresent()) {
                nextNumber++;
                continue;
            }

            item.setItemCode(itemCode);
            try {
                inventoryItemRepository.save(item);
                return itemCode;
            } catch (DataIntegrityViolationException ex) {
                // A concurrent insert may have used this code first; increment and retry.
                nextNumber++;
            }
        }

        throw new IllegalStateException("Could not generate a unique inventory item code. Please try again.");
    }

    private int getNextItemNumber() {
        // Compute max numeric suffix across all existing codes, then increment.
        int maxNumber = 0;
        List<InventoryItem> items = inventoryItemRepository.findAll();
        for (InventoryItem item : items) {
            String code = item.getItemCode();
            if (code == null || code.trim().isEmpty()) {
                continue;
            }
            code = code.trim();
            int dashIndex = code.lastIndexOf('-');
            String numberPart = dashIndex >= 0 && dashIndex < code.length() - 1
                    ? code.substring(dashIndex + 1)
                    : code;
            try {
                int parsed = Integer.parseInt(numberPart.trim());
                if (parsed > maxNumber) {
                    maxNumber = parsed;
                }
            } catch (NumberFormatException ignored) {
                // Ignore unexpected item code formats and continue.
            }
        }
        return maxNumber + 1;
    }
}

