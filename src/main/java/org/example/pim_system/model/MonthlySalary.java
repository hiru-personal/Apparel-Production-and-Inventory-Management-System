package org.example.pim_system.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "monthly_salaries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "salary_month"})
)
public class MonthlySalary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "salary_month", nullable = false, length = 7)
    private String salaryMonth;

    @Column(nullable = false)
    private Double basicSalary;

    @Column(nullable = false)
    private Double overtimeHours;

    @Column(nullable = false)
    private Double overtimeAmount;

    @Column(nullable = false)
    private Double allowances;

    @Column(nullable = false)
    private Double advances;

    @Column(nullable = false)
    private Double grossSalary;

    @Column(nullable = false)
    private Double netSalary;

    @Column(nullable = false)
    private LocalDateTime calculatedAt;

    @PrePersist
    protected void onCreate() {
        calculatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        calculatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public String getSalaryMonth() {
        return salaryMonth;
    }

    public void setSalaryMonth(String salaryMonth) {
        this.salaryMonth = salaryMonth;
    }

    public Double getBasicSalary() {
        return basicSalary;
    }

    public void setBasicSalary(Double basicSalary) {
        this.basicSalary = basicSalary;
    }

    public Double getOvertimeHours() {
        return overtimeHours;
    }

    public void setOvertimeHours(Double overtimeHours) {
        this.overtimeHours = overtimeHours;
    }

    public Double getOvertimeAmount() {
        return overtimeAmount;
    }

    public void setOvertimeAmount(Double overtimeAmount) {
        this.overtimeAmount = overtimeAmount;
    }

    public Double getAllowances() {
        return allowances;
    }

    public void setAllowances(Double allowances) {
        this.allowances = allowances;
    }

    public Double getAdvances() {
        return advances;
    }

    public void setAdvances(Double advances) {
        this.advances = advances;
    }

    public Double getGrossSalary() {
        return grossSalary;
    }

    public void setGrossSalary(Double grossSalary) {
        this.grossSalary = grossSalary;
    }

    public Double getNetSalary() {
        return netSalary;
    }

    public void setNetSalary(Double netSalary) {
        this.netSalary = netSalary;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
