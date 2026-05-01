package org.example.pim_system.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.pim_system.model.MissingNeedle;
import org.example.pim_system.model.InventoryItem;
import org.example.pim_system.repository.InventoryItemRepository;
import org.example.pim_system.repository.MissingNeedleRepository;
import org.example.pim_system.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/needle")
public class MissingNeedleController {

    private static final String NEEDLE_CATEGORY = "Needles";

    @Autowired
    private MissingNeedleRepository missingNeedleRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private AuditLogService auditLogService;

    private String decrementNeedleStockFor(String needleType, int quantity) {
        if (needleType == null || needleType.isBlank()) return null;
        if (quantity <= 0) return null;

        String targetItemName = needleType.trim();

        List<InventoryItem> items = inventoryItemRepository.findByCategory(NEEDLE_CATEGORY);
        InventoryItem target = null;
        for (InventoryItem i : items) {
            if (i.getItemName() != null && i.getItemName().equalsIgnoreCase(targetItemName)) {
                target = i;
                break;
            }
        }

        if (target == null) return null;
        int oldStock = target.getCurrentStock() == null ? 0 : target.getCurrentStock();
        if (oldStock < quantity) {
            return "Cannot remove more than available stock. Current stock: " + oldStock;
        }

        target.setCurrentStock(oldStock - quantity);
        inventoryItemRepository.save(target);
        return null;
    }

    @PostMapping("/report-missing")
    public ResponseEntity<Map<String, Object>> reportMissingNeedle(@RequestBody Map<String, String> needleData,
                                                                     HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            MissingNeedle missingNeedle = new MissingNeedle();
            missingNeedle.setLastSeenBy(needleData.get("lastSeenBy"));
            // UI no longer collects Needle ID/Number; keep a backward-compatible default.
            String needleId = needleData.get("needleId");
            missingNeedle.setNeedleId(needleId != null ? needleId : "N/A");
            missingNeedle.setNeedleType(needleData.get("needleType"));
            missingNeedle.setMachineNumber(needleData.get("machineNumber"));

            int quantity = 1;
            String qtyStr = needleData.get("quantity");
            if (qtyStr != null && !qtyStr.trim().isEmpty()) {
                quantity = Integer.parseInt(qtyStr.trim());
            }
            if (quantity <= 0) {
                response.put("success", false);
                response.put("message", "Quantity must be greater than 0.");
                return ResponseEntity.badRequest().body(response);
            }
            missingNeedle.setQuantityIssued(quantity);
            
            // Parse datetime string
            String lastSeenDateTimeStr = needleData.get("lastSeenDateTime");
            if (lastSeenDateTimeStr != null && !lastSeenDateTimeStr.isEmpty()) {
                if (lastSeenDateTimeStr.length() == 16) {
                    // Format: yyyy-MM-ddTHH:mm
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
                    missingNeedle.setLastSeenDateTime(LocalDateTime.parse(lastSeenDateTimeStr, formatter));
                } else {
                    // Format: yyyy-MM-ddTHH:mm:ss (with seconds)
                    missingNeedle.setLastSeenDateTime(LocalDateTime.parse(lastSeenDateTimeStr));
                }
            } else {
                missingNeedle.setLastSeenDateTime(LocalDateTime.now());
            }
            
            missingNeedle.setSearchActionsTaken(needleData.get("searchActionsTaken"));
            missingNeedle.setStatus("MISSING");

            // Machine number is now required from the UI.
            String machineNumber = missingNeedle.getMachineNumber();
            if (machineNumber == null || machineNumber.isBlank()) {
                response.put("success", false);
                response.put("message", "Machine number is required.");
                return ResponseEntity.badRequest().body(response);
            }
            
            String stockError = decrementNeedleStockFor(missingNeedle.getNeedleType(), quantity);
            if (stockError != null) {
                response.put("success", false);
                response.put("message", stockError);
                return ResponseEntity.badRequest().body(response);
            }

            missingNeedleRepository.save(missingNeedle);

            auditLogService.log(
                    "MISSING_NEEDLE_REPORTED",
                    String.format("CRITICAL: Reported missing needle: Last Seen By=%s, Machine=%s, Type=%s, Qty=%d, Search Actions=%s",
                            needleData.get("lastSeenBy"), needleData.get("machineNumber"),
                            needleData.get("needleType"), quantity,
                            needleData.get("searchActionsTaken")),
                    request
            );
            
            response.put("success", true);
            response.put("message", "Missing needle reported successfully! This is a CRITICAL issue.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error reporting missing needle: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/missing-needles")
    public ResponseEntity<List<MissingNeedle>> getAllMissingNeedles() {
        List<MissingNeedle> missingNeedles = missingNeedleRepository.findAllByOrderByReportedAtDesc();
        return ResponseEntity.ok(missingNeedles);
    }

    @DeleteMapping("/missing-needles/{id}")
    public ResponseEntity<Map<String, Object>> deleteMissingNeedle(@PathVariable Long id,
                                                                     HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        MissingNeedle existing = missingNeedleRepository.findById(id).orElse(null);
        if (existing == null) {
            response.put("success", false);
            response.put("message", "Missing needle record not found");
            return ResponseEntity.badRequest().body(response);
        }

        String lastSeenBy = existing.getLastSeenBy();
        String machine = existing.getMachineNumber();
        String needleType = existing.getNeedleType();

        missingNeedleRepository.deleteById(id);

        auditLogService.log(
                "MISSING_NEEDLE_DELETED",
                String.format("Deleted missing needle record #%d: Last Seen By=%s, Machine=%s, Type=%s",
                        id, lastSeenBy, machine, needleType),
                request
        );

        response.put("success", true);
        response.put("message", "Missing needle record deleted successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/missing-needles/{id}")
    public ResponseEntity<Map<String, Object>> updateMissingNeedle(@PathVariable Long id,
                                                                     @RequestBody Map<String, String> needleData,
                                                                     HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        MissingNeedle existing = missingNeedleRepository.findById(id).orElse(null);
        if (existing == null) {
            response.put("success", false);
            response.put("message", "Missing needle record not found");
            return ResponseEntity.badRequest().body(response);
        }

        existing.setLastSeenBy(needleData.getOrDefault("lastSeenBy", existing.getLastSeenBy()));
        existing.setNeedleId(
                needleData.getOrDefault("needleId", existing.getNeedleId() != null ? existing.getNeedleId() : "N/A")
        );
        existing.setNeedleType(needleData.getOrDefault("needleType", existing.getNeedleType()));
        existing.setMachineNumber(needleData.getOrDefault("machineNumber", existing.getMachineNumber()));

        String qtyStr = needleData.getOrDefault(
                "quantity",
                existing.getQuantityIssued() != null ? existing.getQuantityIssued().toString() : "1"
        );
        int quantityIssued = 1;
        try {
            quantityIssued = Integer.parseInt(qtyStr);
        } catch (Exception ignored) {
            quantityIssued = 1;
        }
        if (quantityIssued <= 0) quantityIssued = 1;
        existing.setQuantityIssued(quantityIssued);

        String lastSeenDateTimeStr = needleData.get("lastSeenDateTime");
        if (lastSeenDateTimeStr != null && !lastSeenDateTimeStr.isEmpty()) {
            if (lastSeenDateTimeStr.length() == 16) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
                existing.setLastSeenDateTime(LocalDateTime.parse(lastSeenDateTimeStr, formatter));
            } else {
                existing.setLastSeenDateTime(LocalDateTime.parse(lastSeenDateTimeStr));
            }
        }

        existing.setSearchActionsTaken(needleData.getOrDefault("searchActionsTaken", existing.getSearchActionsTaken()));
        existing.setStatus(needleData.getOrDefault("status", existing.getStatus()));

        // Machine number should always be present for new records from the UI.
        if (existing.getMachineNumber() == null || existing.getMachineNumber().isBlank()) {
            existing.setMachineNumber("N/A");
        }

        missingNeedleRepository.save(existing);

        auditLogService.log(
                "MISSING_NEEDLE_UPDATED",
                String.format("Updated missing needle record #%d: Last Seen By=%s, Machine=%s, Type=%s, Qty=%d, Status=%s",
                        id, existing.getLastSeenBy(), existing.getMachineNumber(),
                        existing.getNeedleType(), quantityIssued, existing.getStatus()),
                request
        );

        // Note: stock is intentionally not adjusted on edits to avoid double-counting.

        response.put("success", true);
        response.put("message", "Missing needle record updated successfully");
        return ResponseEntity.ok(response);
    }
}



