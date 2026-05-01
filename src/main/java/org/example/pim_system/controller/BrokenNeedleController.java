package org.example.pim_system.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.pim_system.model.BrokenNeedle;
import org.example.pim_system.model.InventoryItem;
import org.example.pim_system.repository.InventoryItemRepository;
import org.example.pim_system.repository.BrokenNeedleRepository;
import org.example.pim_system.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/needle")
public class BrokenNeedleController {

    private static final String NEEDLE_CATEGORY = "Needles";

    @Autowired
    private BrokenNeedleRepository brokenNeedleRepository;

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

    private LocalDateTime parseReportedAt(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return null;
        try {
            // Accept ISO datetime-local: 2026-03-25T14:30
            return LocalDateTime.parse(v);
        } catch (DateTimeParseException ignored) {
            // Try ISO date: 2026-03-25
            try {
                return LocalDate.parse(v).atStartOfDay();
            } catch (DateTimeParseException ignored2) {
                return null;
            }
        }
    }

    @PostMapping("/report-broken")
    public ResponseEntity<Map<String, Object>> reportBrokenNeedle(@RequestBody Map<String, String> needleData,
                                                                    HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
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

            BrokenNeedle brokenNeedle = new BrokenNeedle();
            brokenNeedle.setEmployee(needleData.get("employee"));
            brokenNeedle.setMachineNumber(needleData.get("machineNumber"));
            brokenNeedle.setNeedleType(needleData.get("needleType"));
            brokenNeedle.setQuantityIssued(quantity);
            brokenNeedle.setBreakageReason(needleData.get("breakageReason"));
            LocalDateTime reportedAt = parseReportedAt(needleData.get("reportedAt"));
            if (reportedAt != null) {
                brokenNeedle.setReportedAt(reportedAt);
            }
            
            String stockError = decrementNeedleStockFor(brokenNeedle.getNeedleType(), quantity);
            if (stockError != null) {
                response.put("success", false);
                response.put("message", stockError);
                return ResponseEntity.badRequest().body(response);
            }

            brokenNeedleRepository.save(brokenNeedle);

            auditLogService.log(
                    "BROKEN_NEEDLE_REPORTED",
                    String.format("Reported broken needle: Employee=%s, Machine=%s, Type=%s, Qty=%d, Reason=%s",
                            needleData.get("employee"), needleData.get("machineNumber"),
                            needleData.get("needleType"), quantity,
                            needleData.get("breakageReason")),
                    request
            );
            
            response.put("success", true);
            response.put("message", "Broken needle reported successfully!");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error reporting broken needle: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/broken-needles")
    public ResponseEntity<List<BrokenNeedle>> getAllBrokenNeedles() {
        List<BrokenNeedle> brokenNeedles = brokenNeedleRepository.findAllByOrderByReportedAtDesc();
        return ResponseEntity.ok(brokenNeedles);
    }

    @DeleteMapping("/broken-needles/{id}")
    public ResponseEntity<Map<String, Object>> deleteBrokenNeedle(@PathVariable Long id,
                                                                    HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        BrokenNeedle existing = brokenNeedleRepository.findById(id).orElse(null);
        if (existing == null) {
            response.put("success", false);
            response.put("message", "Broken needle record not found");
            return ResponseEntity.badRequest().body(response);
        }

        String employee = existing.getEmployee();
        String machine = existing.getMachineNumber();
        String needleType = existing.getNeedleType();

        brokenNeedleRepository.deleteById(id);

        auditLogService.log(
                "BROKEN_NEEDLE_DELETED",
                String.format("Deleted broken needle record #%d: Employee=%s, Machine=%s, Type=%s",
                        id, employee, machine, needleType),
                request
        );

        response.put("success", true);
        response.put("message", "Broken needle record deleted successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/broken-needles/{id}")
    public ResponseEntity<Map<String, Object>> updateBrokenNeedle(@PathVariable Long id,
                                                                    @RequestBody Map<String, String> needleData,
                                                                    HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        BrokenNeedle existing = brokenNeedleRepository.findById(id).orElse(null);
        if (existing == null) {
            response.put("success", false);
            response.put("message", "Broken needle record not found");
            return ResponseEntity.badRequest().body(response);
        }

        existing.setEmployee(needleData.getOrDefault("employee", existing.getEmployee()));
        existing.setMachineNumber(needleData.getOrDefault("machineNumber", existing.getMachineNumber()));
        existing.setNeedleType(needleData.getOrDefault("needleType", existing.getNeedleType()));
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
        existing.setBreakageReason(needleData.getOrDefault("breakageReason", existing.getBreakageReason()));
        LocalDateTime reportedAt = parseReportedAt(needleData.get("reportedAt"));
        if (reportedAt != null) {
            existing.setReportedAt(reportedAt);
        }

        brokenNeedleRepository.save(existing);

        auditLogService.log(
                "BROKEN_NEEDLE_UPDATED",
                String.format("Updated broken needle record #%d: Employee=%s, Machine=%s, Type=%s, Qty=%d",
                        id, existing.getEmployee(), existing.getMachineNumber(),
                        existing.getNeedleType(), quantityIssued),
                request
        );

        // Stock is intentionally not adjusted on edits to avoid double-counting.

        response.put("success", true);
        response.put("message", "Broken needle record updated successfully");
        return ResponseEntity.ok(response);
    }
}



