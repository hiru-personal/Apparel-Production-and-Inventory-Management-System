package org.example.pim_system.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.pim_system.model.InventoryItem;
import org.example.pim_system.repository.InventoryItemRepository;
import org.example.pim_system.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/needle/types")
public class NeedleTypeController {

    private static final String NEEDLE_CATEGORY = "Needles";

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<InventoryItem>> getNeedleTypes() {
        return ResponseEntity.ok(inventoryItemRepository.findByCategory(NEEDLE_CATEGORY));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createNeedleType(@RequestBody Map<String, String> payload,
                                                                  HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String typeName = payload.get("typeName");
            String unit = payload.getOrDefault("unit", "pcs");
            Integer initialQuantity = Integer.parseInt(payload.getOrDefault("initialQuantity", "0"));
            Integer minimumStockLevel = Integer.parseInt(payload.getOrDefault("minimumStockLevel", "0"));

            if (typeName == null || typeName.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Type name is required");
                return ResponseEntity.badRequest().body(response);
            }
            String normalizedTypeName = typeName.trim();
            if (inventoryItemRepository.existsByCategoryIgnoreCaseAndItemNameIgnoreCase(NEEDLE_CATEGORY, normalizedTypeName)) {
                response.put("success", false);
                response.put("message", "Needle type name already exists");
                return ResponseEntity.badRequest().body(response);
            }

            InventoryItem item = new InventoryItem();
            item.setItemName(normalizedTypeName);
            item.setCategory(NEEDLE_CATEGORY);
            item.setCurrentStock(Math.max(0, initialQuantity));
            item.setMinimumStockLevel(Math.max(0, minimumStockLevel));
            item.setUnit(unit == null || unit.isBlank() ? "pcs" : unit.trim());
            item.setItemCode(generateNeedleTypeCode());

            inventoryItemRepository.save(item);

            auditLogService.log(
                    "NEEDLE_TYPE_CREATED",
                    String.format("Created needle type: Name=%s, Initial Stock=%d %s, Min Level=%d",
                            normalizedTypeName, Math.max(0, initialQuantity), item.getUnit(), Math.max(0, minimumStockLevel)),
                    request
            );

            response.put("success", true);
            response.put("message", "Needle type created successfully");
            response.put("item", item);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating needle type: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateNeedleType(@PathVariable Long id,
                                                                  @RequestBody Map<String, String> payload,
                                                                  HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            InventoryItem existing = inventoryItemRepository.findById(id).orElse(null);
            if (existing == null || !NEEDLE_CATEGORY.equalsIgnoreCase(existing.getCategory())) {
                response.put("success", false);
                response.put("message", "Needle type not found");
                return ResponseEntity.status(404).body(response);
            }

            StringBuilder changes = new StringBuilder();

            if (payload.containsKey("typeName")) {
                String name = payload.get("typeName");
                if (name != null && !name.trim().isEmpty()) {
                    if (!name.trim().equals(existing.getItemName())) {
                        changes.append(String.format("Name: '%s' → '%s'. ", existing.getItemName(), name.trim()));
                    }
                    existing.setItemName(name.trim());
                }
            }
            if (payload.containsKey("unit")) {
                String unit = payload.get("unit");
                if (unit != null && !unit.trim().isEmpty()) {
                    if (!unit.trim().equals(existing.getUnit())) {
                        changes.append(String.format("Unit: '%s' → '%s'. ", existing.getUnit(), unit.trim()));
                    }
                    existing.setUnit(unit.trim());
                }
            }
            if (payload.containsKey("currentStock")) {
                int newStock = Math.max(0, Integer.parseInt(payload.get("currentStock")));
                if (existing.getCurrentStock() == null || existing.getCurrentStock() != newStock) {
                    changes.append(String.format("Stock: %d → %d. ",
                            existing.getCurrentStock() != null ? existing.getCurrentStock() : 0, newStock));
                }
                existing.setCurrentStock(newStock);
            }
            if (payload.containsKey("minimumStockLevel")) {
                int newMin = Math.max(0, Integer.parseInt(payload.get("minimumStockLevel")));
                if (existing.getMinimumStockLevel() == null || existing.getMinimumStockLevel() != newMin) {
                    changes.append(String.format("Min Level: %d → %d. ",
                            existing.getMinimumStockLevel() != null ? existing.getMinimumStockLevel() : 0, newMin));
                }
                existing.setMinimumStockLevel(newMin);
            }

            inventoryItemRepository.save(existing);

            String changeDetails = changes.length() > 0 ? changes.toString() : "No fields changed.";
            auditLogService.log(
                    "NEEDLE_TYPE_UPDATED",
                    String.format("Updated needle type #%d (%s). Changes: %s", id, existing.getItemName(), changeDetails),
                    request
            );

            response.put("success", true);
            response.put("message", "Needle type updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating needle type: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteNeedleType(@PathVariable Long id,
                                                                  HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            InventoryItem existing = inventoryItemRepository.findById(id).orElse(null);
            if (existing == null || !NEEDLE_CATEGORY.equalsIgnoreCase(existing.getCategory())) {
                response.put("success", false);
                response.put("message", "Needle type not found");
                return ResponseEntity.status(404).body(response);
            }

            String itemName = existing.getItemName();
            inventoryItemRepository.deleteById(id);

            auditLogService.log(
                    "NEEDLE_TYPE_DELETED",
                    String.format("Deleted needle type #%d: Name=%s", id, itemName),
                    request
            );

            response.put("success", true);
            response.put("message", "Needle type deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting needle type: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{id}/stock")
    public ResponseEntity<Map<String, Object>> adjustNeedleTypeStock(@PathVariable Long id,
                                                                       @RequestBody Map<String, String> payload,
                                                                       HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            InventoryItem existing = inventoryItemRepository.findById(id).orElse(null);
            if (existing == null || !NEEDLE_CATEGORY.equalsIgnoreCase(existing.getCategory())) {
                response.put("success", false);
                response.put("message", "Needle type not found");
                return ResponseEntity.status(404).body(response);
            }

            String transactionType = payload.get("transactionType");
            int quantity = Integer.parseInt(payload.getOrDefault("quantity", "0"));
            if (quantity <= 0) {
                response.put("success", false);
                response.put("message", "Quantity must be greater than 0");
                return ResponseEntity.badRequest().body(response);
            }

            int oldStock = existing.getCurrentStock() == null ? 0 : existing.getCurrentStock();
            int newStock;
            if ("add".equalsIgnoreCase(transactionType)) {
                newStock = oldStock + quantity;
            } else if ("remove".equalsIgnoreCase(transactionType) || "subtract".equalsIgnoreCase(transactionType)) {
                newStock = oldStock - quantity;
                if (newStock < 0) {
                    response.put("success", false);
                    response.put("message", "Cannot remove more than available stock. Current stock: " + oldStock);
                    return ResponseEntity.badRequest().body(response);
                }
            } else {
                response.put("success", false);
                response.put("message", "Invalid transaction type. Use 'add' or 'remove'.");
                return ResponseEntity.badRequest().body(response);
            }

            existing.setCurrentStock(newStock);
            inventoryItemRepository.save(existing);

            auditLogService.log(
                    "NEEDLE_STOCK_ADJUSTED",
                    String.format("Needle stock adjusted for '%s': %s %d. Stock changed from %d to %d",
                            existing.getItemName(),
                            "add".equalsIgnoreCase(transactionType) ? "Added" : "Removed",
                            quantity, oldStock, newStock),
                    request
            );

            response.put("success", true);
            response.put("message", "Stock updated successfully");
            response.put("oldStock", oldStock);
            response.put("newStock", newStock);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating stock: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private String generateNeedleTypeCode() {
        return "NDL-" + String.format("%03d", getNextNeedleTypeNumber());
    }

    private int getNextNeedleTypeNumber() {
        return inventoryItemRepository.findTopByCategoryOrderByItemCodeDesc(NEEDLE_CATEGORY)
                .map(item -> {
                    String code = item.getItemCode();
                    if (code == null) {
                        return 1;
                    }
                    int dashIndex = code.lastIndexOf('-');
                    String numberPart = dashIndex >= 0 && dashIndex < code.length() - 1
                            ? code.substring(dashIndex + 1)
                            : code;
                    try {
                        return Integer.parseInt(numberPart) + 1;
                    } catch (NumberFormatException ex) {
                        long count = inventoryItemRepository.findByCategory(NEEDLE_CATEGORY).size();
                        return (int) count + 1;
                    }
                })
                .orElse(1);
    }
}

