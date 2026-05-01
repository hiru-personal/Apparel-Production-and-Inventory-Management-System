package org.example.pim_system.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets")
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String details; // full structured ticket text

    // Backwards compatibility with older schema where 'description' was non-null
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String adminReply;

    private String repliedBy;

    private LocalDateTime repliedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (subject == null || subject.isBlank()) {
            subject = title;
        }
        if (status == null || status.isBlank()) {
            status = "OPEN";
        }
        if (description == null || description.isBlank()) {
            description = details != null ? details : "";
        }
        if (details == null || details.isBlank()) {
            details = description != null ? description : "";
        }
    }

    public SupportTicket() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
        // keep description in sync
        if (details != null && !details.isBlank()) {
            this.description = details;
        }
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAdminReply() {
        return adminReply;
    }

    public void setAdminReply(String adminReply) {
        this.adminReply = adminReply;
    }

    public String getRepliedBy() {
        return repliedBy;
    }

    public void setRepliedBy(String repliedBy) {
        this.repliedBy = repliedBy;
    }

    public LocalDateTime getRepliedAt() {
        return repliedAt;
    }

    public void setRepliedAt(LocalDateTime repliedAt) {
        this.repliedAt = repliedAt;
    }
}

