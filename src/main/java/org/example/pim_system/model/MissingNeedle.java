package org.example.pim_system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "missing_needles")
public class MissingNeedle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String lastSeenBy;
    
    // Stored for backward compatibility; the UI may no longer collect this value.
    @Column(nullable = true)
    private String needleId;

    @Column(nullable = true)
    private String needleType;

    // UI now records machine number for missing needles (dropdown in Needle Management).
    @Column(nullable = true)
    private String machineNumber;

    @Column
    private Integer quantityIssued;
    
    @Column(nullable = false)
    private LocalDateTime lastSeenDateTime;
    
    @Column(columnDefinition = "TEXT")
    private String searchActionsTaken;
    
    @Column(nullable = false)
    private LocalDateTime reportedAt;
    
    @Column(nullable = false)
    private String status = "MISSING";
    
    @PrePersist
    protected void onCreate() {
        reportedAt = LocalDateTime.now();
        if (quantityIssued == null || quantityIssued <= 0) {
            quantityIssued = 1;
        }
    }
    
    public MissingNeedle() {
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getLastSeenBy() {
        return lastSeenBy;
    }
    
    public void setLastSeenBy(String lastSeenBy) {
        this.lastSeenBy = lastSeenBy;
    }
    
    public String getNeedleId() {
        return needleId;
    }
    
    public void setNeedleId(String needleId) {
        this.needleId = needleId;
    }

    public String getMachineNumber() {
        return machineNumber;
    }

    public void setMachineNumber(String machineNumber) {
        this.machineNumber = machineNumber;
    }

    public String getNeedleType() {
        return needleType;
    }

    public void setNeedleType(String needleType) {
        this.needleType = needleType;
    }

    public Integer getQuantityIssued() {
        return quantityIssued;
    }

    public void setQuantityIssued(Integer quantityIssued) {
        this.quantityIssued = quantityIssued;
    }
    
    public LocalDateTime getLastSeenDateTime() {
        return lastSeenDateTime;
    }
    
    public void setLastSeenDateTime(LocalDateTime lastSeenDateTime) {
        this.lastSeenDateTime = lastSeenDateTime;
    }
    
    public String getSearchActionsTaken() {
        return searchActionsTaken;
    }
    
    public void setSearchActionsTaken(String searchActionsTaken) {
        this.searchActionsTaken = searchActionsTaken;
    }
    
    public LocalDateTime getReportedAt() {
        return reportedAt;
    }
    
    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}



