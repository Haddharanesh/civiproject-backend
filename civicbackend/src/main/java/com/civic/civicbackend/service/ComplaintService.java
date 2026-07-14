package com.civic.civicbackend.service;

import com.civic.civicbackend.model.Complaint;
import com.civic.civicbackend.model.User;
import com.civic.civicbackend.repository.ComplaintRepository;
import com.civic.civicbackend.repository.UserRepository;
import com.civic.civicbackend.model.Department;
import com.civic.civicbackend.repository.DepartmentRepository;


import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class ComplaintService {

    private final ComplaintRepository repo;
    private final UserRepository userRepo;
    private final DepartmentRepository departmentRepo;
    private final NotificationService notificationService;

    public ComplaintService(ComplaintRepository repo, UserRepository userRepo, 
                          DepartmentRepository departmentRepo, NotificationService notificationService) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.departmentRepo = departmentRepo;
        this.notificationService = notificationService;
    }

    // 🚀 CREATE COMPLAINT + AUTO ROUTING + AUTO PRIORITY + NOTIFICATION
    public Complaint createComplaint(Complaint complaint, String email) {
        // Find logged-in user
        User user = userRepo.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        complaint.setUser(user);
        complaint.setStatus("NEW");

        // AUTO ROUTING - Description-based first, then category fallback
        Department department = null;

        // Try description-based routing first
        String descriptionDept = routeDepartmentFromDescription(complaint.getDescription());
        if (descriptionDept != null) {
            department = departmentRepo.findByName(descriptionDept).orElse(null);
        }

        // Fallback to category-based routing
        if (department == null) {
            String deptName = switch (complaint.getCategory().toLowerCase()) {
                case "road", "pothole", "street" -> "Public Works Department";
                case "waste", "garbage", "cleanliness" -> "Sanitation Department";
                case "electricity", "streetlight" -> "Electricity Board";
                case "water", "drainage", "sewage" -> "Water Supply Department";
                default -> "General Municipal Department";
            };

            department = departmentRepo.findByName(deptName)
                    .orElseThrow(() -> new RuntimeException("Department not found: " + deptName));
        }

        complaint.setDepartment(department);

        // AUTO PRIORITY
        String priority = calculatePriority(complaint.getDescription());
        complaint.setPriority(priority);

        // AUTO TITLE
        if (complaint.getTitle() == null || complaint.getTitle().trim().isEmpty()) {
            String generatedTitle = generateTitleFromDescription(complaint.getDescription());
            complaint.setTitle(generatedTitle);
        }

        // Save complaint
        Complaint savedComplaint = repo.save(complaint);

        // Notify department about new complaint
        String notificationMessage = String.format("New complaint: %s (Category: %s, Priority: %s)", 
            savedComplaint.getTitle(), savedComplaint.getCategory(), savedComplaint.getPriority());
        notificationService.createDepartmentNotification(
            department.getId(),
            notificationMessage,
            "NEW_COMPLAINT",
            savedComplaint.getId()
        );

        return savedComplaint;
    }

    /**
     * Route department based on complaint description keywords
     * Returns null if no match found
     */
    private String routeDepartmentFromDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return null;
        }
        
        String desc = description.toLowerCase();
        
        // Electricity Board keywords
        if (desc.contains("street light") || desc.contains("light") || 
            desc.contains("electric") || desc.contains("power") || 
            desc.contains("transformer") || desc.contains("wire")) {
            return "Electricity Board";
        }
        
        // Water Supply Department keywords
        if (desc.contains("water") || desc.contains("leakage") || 
            desc.contains("pipe") || desc.contains("drainage") || 
            desc.contains("sewage") || desc.contains("tap") || desc.contains("flood")) {
            return "Water Supply Department";
        }
        
        // Sanitation Department keywords
        if (desc.contains("garbage") || desc.contains("waste") || 
            desc.contains("trash") || desc.contains("cleaning") || 
            desc.contains("dirty") || desc.contains("smell")) {
            return "Sanitation Department";
        }
        
        // Public Works Department keywords
        if (desc.contains("road") || desc.contains("pothole") || 
            desc.contains("street") || desc.contains("bridge") || 
            desc.contains("footpath")) {
            return "Public Works Department";
        }
        
        return null;
    }

    private String calculatePriority(String description) {
        if (description == null) return "MEDIUM";
        String descLower = description.toLowerCase();
        if (descLower.contains("fire") || descLower.contains("danger") ||
            descLower.contains("accident") || descLower.contains("emergency") ||
            descLower.contains("urgent") || descLower.contains("critical") ||
            descLower.contains("flood") || descLower.contains("collapse")) {
            return "HIGH";
        }
        if (descLower.contains("minor") || descLower.contains("small") ||
            descLower.contains("suggestion") || descLower.contains("please")) {
            return "LOW";
        }
        return "MEDIUM";
    }

    private String generateTitleFromDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "Untitled Issue";
        }
        if (description.length() <= 50) {
            return capitalizeFirstLetter(description.trim());
        }
        String[] words = description.trim().split("\\s+");
        StringBuilder titleBuilder = new StringBuilder();
        int maxWords = Math.min(8, words.length);
        for (int i = 0; i < maxWords; i++) {
            if (i > 0) titleBuilder.append(" ");
            titleBuilder.append(words[i]);
            if (i >= 5 && titleBuilder.length() > 45) break;
        }
        String title = titleBuilder.toString().replaceAll("[.,;:!?\\s]+$", "");
        title = capitalizeFirstLetter(title);
        if (title.length() > 50) {
            title = title.substring(0, 50).replaceAll("[.,;:!?\\s]+$", "");
        }
        return title;
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public List<Complaint> getAllComplaints() {
        return repo.findAll();
    }

    public Complaint getComplaintById(Long id) {
        return repo.findById(id).orElseThrow();
    }

    // UPDATE STATUS + NOTIFY USER
    public Complaint updateStatus(Long id, String status) {
        Complaint c = getComplaintById(id);
        String oldStatus = c.getStatus();
        c.setStatus(status);
        Complaint saved = repo.save(c);

        // Notify user about status change
        String message = String.format("Your complaint '%s' status changed from %s to %s", 
            c.getTitle(), oldStatus.replace("_", " "), status.replace("_", " "));
        notificationService.createUserNotification(
            c.getUser().getId(),
            message,
            "STATUS_UPDATE",
            c.getId()
        );

        return saved;
    }

    // ADD REMARKS + NOTIFY USER
    public Complaint addRemarks(Long id, String remarks) {
        Complaint c = getComplaintById(id);
        c.setRemarks(remarks);
        Complaint saved = repo.save(c);

        // Notify user about remark
        String message = String.format("Department added a remark to your complaint '%s': %s", 
            c.getTitle(), remarks.length() > 100 ? remarks.substring(0, 100) + "..." : remarks);
        notificationService.createUserNotification(
            c.getUser().getId(),
            message,
            "REMARK",
            c.getId()
        );

        return saved;
    }

    public List<Complaint> getByStatus(String status) {
        return repo.findByStatus(status);
    }

    public Map<String, Long> getStats() {
        return Map.of(
            "NEW", repo.countByStatus("NEW"),
            "IN_PROGRESS", repo.countByStatus("IN_PROGRESS"),
            "RESOLVED", repo.countByStatus("RESOLVED")
        );
    }

    public Complaint uploadImage(Long id, MultipartFile file) throws IOException {
        Complaint c = getComplaintById(id);
        String uploadDir = "uploads/";
        new File(uploadDir).mkdirs();
        String filePath = uploadDir + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        file.transferTo(new File(filePath));
        c.setImageUrl(filePath);
        return repo.save(c);
    }

    public List<Complaint> getPublicComplaints() {
        return repo.findAll();
    }

    public List<Complaint> getComplaintsByUserEmail(String email) {
        User user = userRepo.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return repo.findByUserId(user.getId());
    }

    public List<Complaint> getComplaintsByDepartment(Department department) {
        return repo.findByDepartment(department);
    }

    public Complaint save(Complaint complaint) {
        return repo.save(complaint);
    }

    public Map<String, Long> getDepartmentStats(Department department) {
        return Map.of(
            "TOTAL", repo.countByDepartment(department),
            "NEW", repo.countByDepartmentAndStatus(department, "NEW"),
            "IN_PROGRESS", repo.countByDepartmentAndStatus(department, "IN_PROGRESS"),
            "RESOLVED", repo.countByDepartmentAndStatus(department, "RESOLVED"),
            "HIGH_PRIORITY", repo.countByDepartmentAndPriority(department, "HIGH")
        );
    }

    public List<Complaint> getComplaintsByDepartmentAndStatus(Department department, String status) {
        return repo.findByDepartmentAndStatus(department, status);
    }

    public Map<String, Long> getPriorityBreakdown(Department department) {
        List<Object[]> results = repo.getPriorityBreakdown(department);
        Map<String, Long> breakdown = new java.util.HashMap<>();
        for (Object[] row : results) {
            String priority = (String) row[0];
            Long count = (Long) row[1];
            breakdown.put(priority, count);
        }
        return breakdown;
    }
}

