package org.example.pim_system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String itemCode;
    
    @Column(nullable = false)
    private String itemName;
    
    @Column(nullable = false)
    private String category;
    
    @Column(nullable = false)
    private Integer currentStock;
    
    @Column(nullable = false)
    private Integer minimumStockLevel;
    
    @Column(nullable = false)
    private String unit;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public InventoryItem() {
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getItemCode() {
        return itemCode;
    }
    
    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }
    
    public String getItemName() {
        return itemName;
    }
    
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public Integer getCurrentStock() {
        return currentStock;
    }
    
    public void setCurrentStock(Integer currentStock) {
        this.currentStock = currentStock;
    }
    
    public Integer getMinimumStockLevel() {
        return minimumStockLevel;
    }
    
    public void setMinimumStockLevel(Integer minimumStockLevel) {
        this.minimumStockLevel = minimumStockLevel;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    // Helper method to check if stock is low
    public boolean isLowStock() {
        return currentStock <= minimumStockLevel;
    }
    
    // Helper method to get status
    public String getStatus() {
        if (currentStock <= minimumStockLevel) {
            return "Low Stock";
        } else if (currentStock <= minimumStockLevel * 1.5) {
            return "Adequate";
        } else {
            return "Adequate";
        }
    }
}

