package org.example.pim_system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "broken_needles")
public class BrokenNeedle {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String employee;
    
    @Column(nullable = false)
    private String machineNumber;
    
    @Column(nullable = false)
    private String needleType;
    
    @Column
    private Integer quantityIssued;

    @Column(columnDefinition = "TEXT")
    private String breakageReason;
    
    @Column(nullable = false)
    private LocalDateTime reportedAt;
    
    @PrePersist
    protected void onCreate() {
        if (reportedAt == null) {
            reportedAt = LocalDateTime.now();
        }
        if (quantityIssued == null || quantityIssued <= 0) {
            quantityIssued = 1;
        }
    }
    
    public BrokenNeedle() {
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getEmployee() {
        return employee;
    }
    
    public void setEmployee(String employee) {
        this.employee = employee;
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
    
    public String getBreakageReason() {
        return breakageReason;
    }
    
    public void setBreakageReason(String breakageReason) {
        this.breakageReason = breakageReason;
    }
    
    public LocalDateTime getReportedAt() {
        return reportedAt;
    }
    
    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }
}



