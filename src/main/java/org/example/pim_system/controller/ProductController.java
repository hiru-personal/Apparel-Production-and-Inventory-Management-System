package org.example.pim_system.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.pim_system.model.Product;
import org.example.pim_system.repository.ProductRepository;
import org.example.pim_system.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AuditLogService auditLogService;
    
    @GetMapping// Get all products ordered by product code
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productRepository.findAllByOrderByProductCodeAsc();
        // Loop through each product to calculate remaining quantity
        for (Product p : products) {
            int target = p.getTarget() != null ? p.getTarget() : 0;
            int current = p.getCurrentQuantity() != null ? p.getCurrentQuantity() : 0;
            int expectedRemaining = target - current;//calculate target remaining
            if (expectedRemaining < 0) expectedRemaining = 0;
            Integer existingRemaining = p.getRemainingQuantity();
            if (existingRemaining == null || existingRemaining != expectedRemaining) {
                p.setRemainingQuantity(expectedRemaining);
                productRepository.save(p);
            }
        }
        return ResponseEntity.ok(products);
    }
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody Map<String, String> data,
                                                             HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        //demo button
        String productCode = data != null ? data.get("productCode") : null;
        String productName = data != null ? data.get("productName") : null;
        String currentQuantityStr = data != null ? data.get("currentQuantity") : null;
        String targetStr = data != null ? data.get("target") : null;
        String status = data != null ? data.get("status") : null;
        String startingDateStr = data != null ? data.get("startingDate") : null;
        String endingDateStr = data != null ? data.get("endingDate") : null;
        //required things in product management
        if (productCode == null || productCode.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Product code is required.");
            return ResponseEntity.badRequest().body(response);
        }
        if (productName == null || productName.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Product name is required.");
            return ResponseEntity.badRequest().body(response);
        }
        if (targetStr == null || targetStr.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Target is required.");
            return ResponseEntity.badRequest().body(response);
        }
        if (startingDateStr == null || startingDateStr.trim().isEmpty()
                || endingDateStr == null || endingDateStr.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Starting and ending dates are required.");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (productRepository.existsByProductCode(productCode.trim())) {
            response.put("success", false);
            response.put("message", "A product with this code already exists.");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            Integer target = Integer.parseInt(targetStr.trim());
            Integer currentQuantity = (currentQuantityStr == null || currentQuantityStr.trim().isEmpty())
                    ? 0
                    : Integer.parseInt(currentQuantityStr.trim());
            LocalDate startingDate = LocalDate.parse(startingDateStr.trim());
            LocalDate endingDate = LocalDate.parse(endingDateStr.trim());
            LocalDate today = LocalDate.now();
                //validation parts (starting date and end date)
            if (startingDate.isBefore(today)) {
                response.put("success", false);
                response.put("message", "Starting date must be today or a future date.");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (endingDate.isBefore(startingDate)) {
                response.put("success", false);
                response.put("message", "Ending date must be on or after starting date.");
                return ResponseEntity.badRequest().body(response);
            }

            if (endingDate.isBefore(today)) {
                response.put("success", false);
                response.put("message", "Ending date must be today or a future date.");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (status == null || status.trim().isEmpty()) {
                status = "PLANNED";
            }

            Product product = new Product();
            product.setProductCode(productCode.trim());
            product.setProductName(productName.trim());
            product.setCurrentQuantity(currentQuantity);
            product.setTarget(target);
            int remaining = target - currentQuantity;// calculate remaining quantity
            if (remaining < 0) remaining = 0;
            product.setRemainingQuantity(remaining);
            product.setStatus(status.trim());
            product.setStartingDate(startingDate);
            product.setEndingDate(endingDate);
            //save product in DB
            Product saved = productRepository.save(product);
            auditLogService.log( //writes audit log
                    "PRODUCT_CREATED",
                    String.format("Created product %s (%s), target=%d, current=%d, status=%s, start=%s, end=%s",
                            saved.getProductCode(),
                            saved.getProductName(),
                            saved.getTarget() != null ? saved.getTarget() : 0,
                            saved.getCurrentQuantity() != null ? saved.getCurrentQuantity() : 0,
                            saved.getStatus(),
                            saved.getStartingDate(),
                            saved.getEndingDate()),
                    request
            ); //success response
            response.put("success", true);
            response.put("message", "Product added successfully.");
            return ResponseEntity.ok(response);
        } catch (Exception e) { //error response
            response.put("success", false);
            response.put("message", "Error adding product: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProduct(@PathVariable Long id,
                                                             @RequestBody Map<String, String> data,
                                                             HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        Product existing = productRepository.findById(id).orElse(null);
        if (existing == null) {
            response.put("success", false);
            response.put("message", "Product not found.");
            return ResponseEntity.badRequest().body(response);
        }
        
        String productCode = data != null ? data.get("productCode") : null;
        String productName = data != null ? data.get("productName") : null;
        String currentQuantityStr = data != null ? data.get("currentQuantity") : null;
        String targetStr = data != null ? data.get("target") : null;
        String status = data != null ? data.get("status") : null;
        String startingDateStr = data != null ? data.get("startingDate") : null;
        String endingDateStr = data != null ? data.get("endingDate") : null;
        
        if (productCode == null || productCode.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Product code is required.");
            return ResponseEntity.badRequest().body(response);
        }
        if (productName == null || productName.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Product name is required.");
            return ResponseEntity.badRequest().body(response);
        }
        if (targetStr == null || targetStr.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Target is required.");
            return ResponseEntity.badRequest().body(response);
        }
        if (startingDateStr == null || startingDateStr.trim().isEmpty()
                || endingDateStr == null || endingDateStr.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Starting and ending dates are required.");
            return ResponseEntity.badRequest().body(response);
        }
        
        // If code changed, ensure uniqueness
        if (!existing.getProductCode().equals(productCode.trim())
                && productRepository.existsByProductCode(productCode.trim())) {
            response.put("success", false);
            response.put("message", "A product with this code already exists.");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            Integer target = Integer.parseInt(targetStr.trim());
            Integer currentQuantity = (currentQuantityStr == null || currentQuantityStr.trim().isEmpty())
                    ? existing.getCurrentQuantity()
                    : Integer.parseInt(currentQuantityStr.trim());
            LocalDate startingDate = LocalDate.parse(startingDateStr.trim());
            LocalDate endingDate = LocalDate.parse(endingDateStr.trim());
            LocalDate today = LocalDate.now();

            if (startingDate.isBefore(today)) {
                response.put("success", false);
                response.put("message", "Starting date must be today or a future date.");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (endingDate.isBefore(startingDate)) {
                response.put("success", false);
                response.put("message", "Ending date must be on or after starting date.");
                return ResponseEntity.badRequest().body(response);
            }

            if (endingDate.isBefore(today)) {
                response.put("success", false);
                response.put("message", "Ending date must be today or a future date.");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (status == null || status.trim().isEmpty()) {
                status = existing.getStatus();
            }
            
            existing.setProductCode(productCode.trim());
            existing.setProductName(productName.trim());
            existing.setCurrentQuantity(currentQuantity);
            existing.setTarget(target);
            int remaining = target - currentQuantity;
            if (remaining < 0) remaining = 0;
            existing.setRemainingQuantity(remaining);
            existing.setStatus(status.trim());
            existing.setStartingDate(startingDate);
            existing.setEndingDate(endingDate);
            
            Product saved = productRepository.save(existing);
            auditLogService.log(
                    "PRODUCT_UPDATED",
                    String.format("Updated product #%d %s (%s), target=%d, current=%d, remaining=%d, status=%s, start=%s, end=%s",
                            saved.getId(),
                            saved.getProductCode(),
                            saved.getProductName(),
                            saved.getTarget() != null ? saved.getTarget() : 0,
                            saved.getCurrentQuantity() != null ? saved.getCurrentQuantity() : 0,
                            saved.getRemainingQuantity() != null ? saved.getRemainingQuantity() : 0,
                            saved.getStatus(),
                            saved.getStartingDate(),
                            saved.getEndingDate()),
                    request
            );
            
            response.put("success", true);
            response.put("message", "Product updated successfully.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating product: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProduct(@PathVariable Long id,
                                                             HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            response.put("success", false);
            response.put("message", "Product not found.");
            return ResponseEntity.badRequest().body(response);
        }
        productRepository.deleteById(id);
        auditLogService.log(
                "PRODUCT_DELETED",
                String.format("Deleted product #%d %s (%s)",
                        product.getId(),
                        product.getProductCode(),
                        product.getProductName()),
                request
        );
        response.put("success", true);
        response.put("message", "Product deleted successfully.");
        return ResponseEntity.ok(response);
    }
}
