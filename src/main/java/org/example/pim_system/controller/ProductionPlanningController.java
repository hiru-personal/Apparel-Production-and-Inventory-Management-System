package org.example.pim_system.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.pim_system.model.ProductionPlanningPlan;
import org.example.pim_system.model.Product;
import org.example.pim_system.repository.ProductionPlanningPlanRepository;
import org.example.pim_system.repository.ProductRepository;
import org.example.pim_system.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/production-planning")
public class ProductionPlanningController {

    @Autowired
    private ProductionPlanningPlanRepository productionPlanningPlanRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AuditLogService auditLogService;

    private void applyCompletionSideEffects(ProductionPlanningPlan plan) {
        if (plan == null || plan.getProductCode() == null) return;
        if (plan.getQuantity() == null) return;

        Product product = productRepository.findByProductCode(plan.getProductCode()).orElse(null);
        if (product == null) return;

        int qty = plan.getQuantity();
        if (qty <= 0) return;
        int current = product.getCurrentQuantity() != null ? product.getCurrentQuantity() : 0;
        int completed = product.getCompletedQuantity() != null ? product.getCompletedQuantity() : 0;
        int target = product.getTarget() != null ? product.getTarget() : 0;

        int updatedCurrent = current + qty;
        int updatedCompleted = completed + qty;
        int updatedRemaining = target - updatedCurrent;
        if (updatedRemaining < 0) updatedRemaining = 0;

        product.setCurrentQuantity(updatedCurrent);
        product.setCompletedQuantity(updatedCompleted);
        product.setRemainingQuantity(updatedRemaining);
        productRepository.save(product);
    }

    @GetMapping("/plans")
    public ResponseEntity<List<ProductionPlanningPlan>> getAllPlans() {
        List<ProductionPlanningPlan> plans = productionPlanningPlanRepository.findAllByOrderByCreatedAtDesc();
        LocalDate today = LocalDate.now();

        // Only auto-complete overdue plans.
        // Do not auto-reset non-overdue plans back to Planning because status is user-editable in Daily View.
        for (ProductionPlanningPlan p : plans) {
            boolean shouldBeComplete = p.getEndDate() != null && p.getEndDate().isBefore(today);
            String currentStatus = p.getStatus();

            if (shouldBeComplete && !"Complete".equalsIgnoreCase(currentStatus)) {
                p.setStatus("Complete");
                productionPlanningPlanRepository.save(p);
                applyCompletionSideEffects(p);
            }
        }

        return ResponseEntity.ok(plans);
    }

    @PostMapping("/plans")
    public ResponseEntity<Map<String, Object>> createPlan(@RequestBody Map<String, String> payload,
                                                          HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String productCode = payload != null ? payload.get("productCode") : null;
            String quantityStr = payload != null ? payload.get("quantity") : null;
            String endDateStr = payload != null ? payload.get("endDate") : null;
            String assignedMachine = payload != null ? payload.get("assignedMachine") : null;
            String note = payload != null ? payload.get("note") : null;

            if (productCode == null || productCode.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Product is required.");
                return ResponseEntity.badRequest().body(response);
            }
            if (quantityStr == null || quantityStr.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Quantity is required.");
                return ResponseEntity.badRequest().body(response);
            }
            if (endDateStr == null || endDateStr.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "End date is required.");
                return ResponseEntity.badRequest().body(response);
            }
            if (assignedMachine == null || assignedMachine.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Assigned machine is required.");
                return ResponseEntity.badRequest().body(response);
            }

            int qty;
            try {
                qty = Integer.parseInt(quantityStr.trim());
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "Quantity must be a valid number.");
                return ResponseEntity.badRequest().body(response);
            }
            if (qty <= 0) {
                response.put("success", false);
                response.put("message", "Quantity must be greater than 0.");
                return ResponseEntity.badRequest().body(response);
            }

            LocalDate endDate;
            try {
                endDate = LocalDate.parse(endDateStr.trim());
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "End date format must be YYYY-MM-DD.");
                return ResponseEntity.badRequest().body(response);
            }

            Optional<Product> productOpt = productRepository.findByProductCode(productCode.trim());
            if (!productOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Selected product does not exist.");
                return ResponseEntity.badRequest().body(response);
            }

            Product product = productOpt.get();
            LocalDate today = LocalDate.now();
            LocalDate productStartDate = product.getStartingDate();
            LocalDate productEndDate = product.getEndingDate();

            if (productStartDate != null && endDate.isBefore(productStartDate)) {
                response.put("success", false);
                response.put("message", String.format(
                        "Plan date must be on or after product starting date (%s).",
                        productStartDate
                ));
                return ResponseEntity.badRequest().body(response);
            }
            if (productEndDate != null && endDate.isAfter(productEndDate)) {
                response.put("success", false);
                response.put("message", String.format(
                        "Plan date must be on or before product ending date (%s).",
                        productEndDate
                ));
                return ResponseEntity.badRequest().body(response);
            }
            if (endDate.isBefore(today)) {
                response.put("success", false);
                response.put("message", "Plan date must be today or a future date.");
                return ResponseEntity.badRequest().body(response);
            }

            // Keep plan statuses (and product completed quantities) in sync before validating capacity.
            List<ProductionPlanningPlan> existingPlansForProduct = productionPlanningPlanRepository
                    .findAllByProductCodeOrderByCreatedAtDesc(productCode.trim());

            for (ProductionPlanningPlan p : existingPlansForProduct) {
                boolean shouldBeComplete = p.getEndDate() != null && p.getEndDate().isBefore(today);
                String currentStatus = p.getStatus();

                if (shouldBeComplete && !"Complete".equalsIgnoreCase(currentStatus)) {
                    p.setStatus("Complete");
                    productionPlanningPlanRepository.save(p);

                    // Only apply side-effects when transitioning into Complete.
                    applyCompletionSideEffects(p);
                }
            }

            // Re-load product after any completion side-effects.
            product = productRepository.findByProductCode(productCode.trim()).orElse(product);

            // Validate against remaining stock (deducted at plan creation time).
            int target = product.getTarget() != null ? product.getTarget() : 0;
            int current = product.getCurrentQuantity() != null ? product.getCurrentQuantity() : 0;
            int fallbackRemaining = target - current;
            if (fallbackRemaining < 0) fallbackRemaining = 0;
            int availableForNewPlan = product.getRemainingQuantity() != null
                    ? product.getRemainingQuantity()
                    : fallbackRemaining;

            if (qty > availableForNewPlan) {
                response.put("success", false);
                response.put("message", String.format(
                        "Quantity exceeds available capacity for %s. Available: %d.",
                        productCode.trim(),
                        availableForNewPlan
                ));
                return ResponseEntity.badRequest().body(response);
            }

            ProductionPlanningPlan plan = new ProductionPlanningPlan();
            plan.setProductCode(product.getProductCode());
            plan.setProductName(product.getProductName());
            plan.setQuantity(qty);
            plan.setEndDate(endDate);
            plan.setAssignedMachine(assignedMachine.trim());
            plan.setNote(note != null ? note.trim() : null);

            // Auto-status based on end date.
            // "End date has passed" => endDate < today
            if (endDate.isBefore(LocalDate.now())) {
                plan.setStatus("Complete");
            } else {
                plan.setStatus("Planning");
            }

            ProductionPlanningPlan saved = productionPlanningPlanRepository.save(plan);

            // If plan is created as Complete, immediately increase completed/current progress.
            if ("Complete".equalsIgnoreCase(saved.getStatus())) {
                applyCompletionSideEffects(saved);
            }
            auditLogService.log(
                    "PRODUCTION_PLAN_CREATED",
                    String.format("Created production plan #%d for %s (%s), qty=%d, endDate=%s, machine=%s, status=%s",
                            saved.getId(),
                            saved.getProductCode(),
                            saved.getProductName(),
                            saved.getQuantity() != null ? saved.getQuantity() : 0,
                            saved.getEndDate(),
                            saved.getAssignedMachine(),
                            saved.getStatus()),
                    request
            );

            response.put("success", true);
            response.put("message", "Production plan added successfully.");
            response.put("plan", saved);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error saving production plan: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/plans/{id}")
    public ResponseEntity<Map<String, Object>> updatePlan(@PathVariable Long id,
                                                          @RequestBody Map<String, String> payload,
                                                          HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<ProductionPlanningPlan> planOpt = productionPlanningPlanRepository.findById(id);
            if (!planOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Plan not found.");
                return ResponseEntity.badRequest().body(response);
            }

            ProductionPlanningPlan plan = planOpt.get();
            if ("Complete".equalsIgnoreCase(plan.getStatus())) {
                response.put("success", false);
                response.put("message", "Completed plans cannot be edited.");
                return ResponseEntity.badRequest().body(response);
            }

            String quantityStr = payload != null ? payload.get("quantity") : null;
            String endDateStr = payload != null ? payload.get("endDate") : null;
            String assignedMachine = payload != null ? payload.get("assignedMachine") : null;
            String note = payload != null ? payload.get("note") : null;

            if (quantityStr == null || quantityStr.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Quantity is required.");
                return ResponseEntity.badRequest().body(response);
            }
            if (endDateStr == null || endDateStr.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "End date is required.");
                return ResponseEntity.badRequest().body(response);
            }
            if (assignedMachine == null || assignedMachine.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Assigned machine is required.");
                return ResponseEntity.badRequest().body(response);
            }

            int qty;
            try {
                qty = Integer.parseInt(quantityStr.trim());
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "Quantity must be a valid number.");
                return ResponseEntity.badRequest().body(response);
            }
            if (qty <= 0) {
                response.put("success", false);
                response.put("message", "Quantity must be greater than 0.");
                return ResponseEntity.badRequest().body(response);
            }

            LocalDate endDate;
            try {
                endDate = LocalDate.parse(endDateStr.trim());
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "End date format must be YYYY-MM-DD.");
                return ResponseEntity.badRequest().body(response);
            }

            Product product = productRepository.findByProductCode(plan.getProductCode()).orElse(null);
            if (product == null) {
                response.put("success", false);
                response.put("message", "Linked product does not exist.");
                return ResponseEntity.badRequest().body(response);
            }

            LocalDate today = LocalDate.now();
            LocalDate productStartDate = product.getStartingDate();
            LocalDate productEndDate = product.getEndingDate();
            if (productStartDate != null && endDate.isBefore(productStartDate)) {
                response.put("success", false);
                response.put("message", String.format(
                        "Plan date must be on or after product starting date (%s).",
                        productStartDate
                ));
                return ResponseEntity.badRequest().body(response);
            }
            if (productEndDate != null && endDate.isAfter(productEndDate)) {
                response.put("success", false);
                response.put("message", String.format(
                        "Plan date must be on or before product ending date (%s).",
                        productEndDate
                ));
                return ResponseEntity.badRequest().body(response);
            }
            if (endDate.isBefore(today)) {
                response.put("success", false);
                response.put("message", "Plan date must be today or a future date.");
                return ResponseEntity.badRequest().body(response);
            }

            String previousStatus = plan.getStatus();
            String nextStatus = previousStatus;
            if (endDate.isBefore(today)) {
                nextStatus = "Complete";
            } else if (nextStatus == null || nextStatus.trim().isEmpty()) {
                nextStatus = "Planning";
            }

            plan.setQuantity(qty);
            plan.setEndDate(endDate);
            plan.setAssignedMachine(assignedMachine.trim());
            plan.setNote(note != null ? note.trim() : null);
            plan.setStatus(nextStatus);
            ProductionPlanningPlan saved = productionPlanningPlanRepository.save(plan);

            if ("Complete".equalsIgnoreCase(nextStatus) && !"Complete".equalsIgnoreCase(previousStatus)) {
                applyCompletionSideEffects(saved);
            }
            auditLogService.log(
                    "PRODUCTION_PLAN_UPDATED",
                    String.format("Updated production plan #%d for %s, qty=%d, endDate=%s, machine=%s, status=%s",
                            saved.getId(),
                            saved.getProductCode(),
                            saved.getQuantity() != null ? saved.getQuantity() : 0,
                            saved.getEndDate(),
                            saved.getAssignedMachine(),
                            saved.getStatus()),
                    request
            );

            response.put("success", true);
            response.put("message", "Production plan updated successfully.");
            response.put("plan", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating production plan: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PatchMapping("/plans/{id}/status")
    public ResponseEntity<Map<String, Object>> updatePlanStatus(@PathVariable Long id,
                                                                @RequestBody Map<String, String> payload,
                                                                HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<ProductionPlanningPlan> planOpt = productionPlanningPlanRepository.findById(id);
            if (!planOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Plan not found.");
                return ResponseEntity.badRequest().body(response);
            }

            ProductionPlanningPlan plan = planOpt.get();
            String requestedStatus = payload != null ? payload.get("status") : null;
            if (requestedStatus == null || requestedStatus.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Status is required.");
                return ResponseEntity.badRequest().body(response);
            }

            String normalizedStatus = requestedStatus.trim();
            if (!"Planning".equalsIgnoreCase(normalizedStatus) && !"Complete".equalsIgnoreCase(normalizedStatus)) {
                response.put("success", false);
                response.put("message", "Status must be either Planning or Complete.");
                return ResponseEntity.badRequest().body(response);
            }

            String previousStatus = plan.getStatus();
            String nextStatus = "Complete".equalsIgnoreCase(normalizedStatus) ? "Complete" : "Planning";

            if ("Complete".equalsIgnoreCase(previousStatus) && "Planning".equalsIgnoreCase(nextStatus)) {
                response.put("success", false);
                response.put("message", "Completed plans cannot be moved back to Planning.");
                return ResponseEntity.badRequest().body(response);
            }

            if (previousStatus != null && previousStatus.equalsIgnoreCase(nextStatus)) {
                response.put("success", true);
                response.put("message", "Status unchanged.");
                response.put("plan", plan);
                return ResponseEntity.ok(response);
            }

            plan.setStatus(nextStatus);
            ProductionPlanningPlan saved = productionPlanningPlanRepository.save(plan);

            if ("Complete".equalsIgnoreCase(nextStatus) && !"Complete".equalsIgnoreCase(previousStatus)) {
                applyCompletionSideEffects(saved);
            }
            auditLogService.log(
                    "PRODUCTION_PLAN_STATUS_UPDATED",
                    String.format("Changed production plan #%d status from %s to %s",
                            saved.getId(),
                            previousStatus,
                            nextStatus),
                    request
            );

            response.put("success", true);
            response.put("message", "Plan status updated successfully.");
            response.put("plan", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating plan status: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/plans/{id}")
    public ResponseEntity<Map<String, Object>> deletePlan(@PathVariable Long id,
                                                          HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<ProductionPlanningPlan> planOpt = productionPlanningPlanRepository.findById(id);
            if (!planOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Plan not found.");
                return ResponseEntity.badRequest().body(response);
            }

            ProductionPlanningPlan plan = planOpt.get();
            productionPlanningPlanRepository.delete(plan);
            auditLogService.log(
                    "PRODUCTION_PLAN_DELETED",
                    String.format("Deleted production plan #%d for %s (%s), qty=%d",
                            plan.getId(),
                            plan.getProductCode(),
                            plan.getProductName(),
                            plan.getQuantity() != null ? plan.getQuantity() : 0),
                    request
            );

            response.put("success", true);
            response.put("message", "Production plan deleted successfully.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting production plan: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

