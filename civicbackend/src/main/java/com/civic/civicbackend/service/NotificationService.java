package com.civic.civicbackend.service;

import com.civic.civicbackend.model.Notification;
import com.civic.civicbackend.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository repo;

    public NotificationService(NotificationRepository repo) {
        this.repo = repo;
    }

    // Create notification for user
    public void createUserNotification(Long userId, String message, String type, Long complaintId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setMessage(message);
        n.setType(type);
        n.setComplaintId(complaintId);
        repo.save(n);
    }

    // Create notification for department
    public void createDepartmentNotification(Long departmentId, String message, String type, Long complaintId) {
        Notification n = new Notification();
        n.setDepartmentId(departmentId);
        n.setMessage(message);
        n.setType(type);
        n.setComplaintId(complaintId);
        repo.save(n);
    }

    // Get user notifications sorted by date
    public List<Notification> getUserNotifications(Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // Get department notifications sorted by date
    public List<Notification> getDepartmentNotifications(Long departmentId) {
        return repo.findByDepartmentIdOrderByCreatedAtDesc(departmentId);
    }

    // Get unread count for user
    public long getUnreadUserCount(Long userId) {
        return repo.countByUserIdAndIsReadFalse(userId);
    }

    // Get unread count for department
    public long getUnreadDepartmentCount(Long departmentId) {
        return repo.countByDepartmentIdAndIsReadFalse(departmentId);
    }

    // Mark notification as read
    public void markAsRead(Long notificationId) {
        repo.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            repo.save(n);
        });
    }

    // Mark all user notifications as read
    public void markAllUserAsRead(Long userId) {
        List<Notification> notifications = repo.findByUserId(userId);
        notifications.forEach(n -> n.setRead(true));
        repo.saveAll(notifications);
    }

    // Mark all department notifications as read
    public void markAllDepartmentAsRead(Long departmentId) {
        List<Notification> notifications = repo.findByDepartmentId(departmentId);
        notifications.forEach(n -> n.setRead(true));
        repo.saveAll(notifications);
    }
}

