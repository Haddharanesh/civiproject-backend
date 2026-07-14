package com.civic.civicbackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long departmentId;

    private String message;

    private String type; // STATUS_UPDATE, REMARK, NEW_COMPLAINT

    private Long complaintId;

    private boolean isRead = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters

    public Long getId() { return id; }

    public Long getUserId() { return userId; }

    public void setUserId(Long userId) { this.userId = userId; }

    public Long getDepartmentId() { return departmentId; }

    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getMessage() { return message; }

    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }

    public void setType(String type) { this.type = type; }

    public Long getComplaintId() { return complaintId; }

    public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }

    public boolean isRead() { return isRead; }

    public void setRead(boolean read) { isRead = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}

