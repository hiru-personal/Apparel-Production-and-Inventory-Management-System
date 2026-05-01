package org.example.pim_system.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "products_new")
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String productCode; // Product_Code (business key)
    
    @Column(nullable = false)
    private String productName; // Product_Name
    
    @Column(nullable = false)
    private Integer currentQuantity; // Current_Quantity (auto-reduced)
    
    @Column(nullable = false)
    private Integer target; // Target

    @Column(nullable = false)
    private Integer completedQuantity = 0; // Total completed units

    @Column(nullable = false)
    private Integer remainingQuantity = 0; // Remaining units to reach target
    
    @Column(nullable = false)
    private String status; // Status
    
    @Column(nullable = false)
    private LocalDate startingDate; // Starting_Date
    
    @Column(nullable = false)
    private LocalDate endingDate; // Ending_Date
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (completedQuantity == null) {
            completedQuantity = 0;
        }
        if (remainingQuantity == null) {
            // If target is set before persist, initialize remaining = target
            remainingQuantity = target != null ? target : 0;
        }
    }
    
    public Product() {
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getProductCode() {
        return productCode;
    }
    
    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public Integer getCurrentQuantity() {
        return currentQuantity;
    }
    
    public void setCurrentQuantity(Integer currentQuantity) {
        this.currentQuantity = currentQuantity;
    }
    
    public Integer getTarget() {
        return target;
    }
    
    public void setTarget(Integer target) {
        this.target = target;
    }

    public Integer getCompletedQuantity() {
        return completedQuantity;
    }

    public void setCompletedQuantity(Integer completedQuantity) {
        this.completedQuantity = completedQuantity;
    }

    public Integer getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(Integer remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDate getStartingDate() {
        return startingDate;
    }
    
    public void setStartingDate(LocalDate startingDate) {
        this.startingDate = startingDate;
    }
    
    public LocalDate getEndingDate() {
        return endingDate;
    }
    
    public void setEndingDate(LocalDate endingDate) {
        this.endingDate = endingDate;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
