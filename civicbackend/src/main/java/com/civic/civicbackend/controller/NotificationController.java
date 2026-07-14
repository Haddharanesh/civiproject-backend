package com.civic.civicbackend.controller;

import com.civic.civicbackend.model.Notification;
import com.civic.civicbackend.model.User;
import com.civic.civicbackend.repository.UserRepository;
import com.civic.civicbackend.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin
public class NotificationController {

    private final NotificationService service;
    private final UserRepository userRepo;

    public NotificationController(NotificationService service, UserRepository userRepo) {
        this.service = service;
        this.userRepo = userRepo;
    }

    // Get notifications for logged-in user
    @GetMapping("/my")
    public List<Notification> getMyNotifications(Authentication authentication) {
        User user = userRepo.findByEmail(authentication.getName());
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return service.getUserNotifications(user.getId());
    }

    // Get notifications for department admin
    @GetMapping("/department")
    public List<Notification> getDepartmentNotifications(Authentication authentication) {
        User user = userRepo.findByEmail(authentication.getName());
        if (user == null || user.getDepartment() == null) {
            throw new RuntimeException("Department not found");
        }
        return service.getDepartmentNotifications(user.getDepartment().getId());
    }

    // Get unread count
    @GetMapping("/unread/count")
    public Map<String, Long> getUnreadCount(Authentication authentication) {
        User user = userRepo.findByEmail(authentication.getName());
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        
        long userUnread = service.getUnreadUserCount(user.getId());
        long deptUnread = 0;
        
        if (user.getDepartment() != null) {
            deptUnread = service.getUnreadDepartmentCount(user.getDepartment().getId());
        }
        
        return Map.of(
            "userUnread", userUnread,
            "departmentUnread", deptUnread
        );
    }

    // Mark single notification as read
    @PutMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id) {
        service.markAsRead(id);
    }

    // Mark all user notifications as read
    @PutMapping("/mark-all-read")
    public void markAllAsRead(Authentication authentication) {
        User user = userRepo.findByEmail(authentication.getName());
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        service.markAllUserAsRead(user.getId());
    }

    // Mark all department notifications as read
    @PutMapping("/department/mark-all-read")
    public void markAllDepartmentAsRead(Authentication authentication) {
        User user = userRepo.findByEmail(authentication.getName());
        if (user == null || user.getDepartment() == null) {
            throw new RuntimeException("Department not found");
        }
        service.markAllDepartmentAsRead(user.getDepartment().getId());
    }
}

