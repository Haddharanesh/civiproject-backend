
package com.civic.civicbackend.controller;

import com.civic.civicbackend.model.Complaint;
import com.civic.civicbackend.model.Department;
import com.civic.civicbackend.model.User;
import com.civic.civicbackend.repository.UserRepository;
import com.civic.civicbackend.service.ComplaintService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/department")
public class DepartmentAdminController {

    private final ComplaintService complaintService;
    private final UserRepository userRepo;

    public DepartmentAdminController(ComplaintService complaintService, UserRepository userRepo) {
        this.complaintService = complaintService;
        this.userRepo = userRepo;
    }

    // 📋 Get complaints for logged-in department admin
@GetMapping("/complaints")
    public List<Complaint> getDepartmentComplaints(Authentication authentication) {

        String email = authentication.getName(); // logged-in user's email
        User user = userRepo.findByEmail(email);

        if (user == null || user.getDepartment() == null) {
            throw new RuntimeException("Department admin not properly assigned");
        }

        Department dept = user.getDepartment();

        return complaintService.getComplaintsByDepartment(dept);
    }

    // 📋 Filter complaints by status for department
    @GetMapping("/complaints/filter")
    public List<Complaint> filterComplaintsByStatus(
            @RequestParam String status,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userRepo.findByEmail(email);

        if (user == null || user.getDepartment() == null) {
            throw new RuntimeException("Department admin not properly assigned");
        }

        Department dept = user.getDepartment();
        return complaintService.getComplaintsByDepartmentAndStatus(dept, status);
    }
    @PutMapping("/complaints/{id}/status")
    public Complaint updateComplaintStatus(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userRepo.findByEmail(email);

        if (user == null || user.getDepartment() == null) {
            throw new RuntimeException("Unauthorized department admin");
        }

        Complaint complaint = complaintService.getComplaintById(id);

        // 🔒 Security Check: Complaint must belong to same department
        if (!complaint.getDepartment().getId().equals(user.getDepartment().getId())) {
            throw new RuntimeException("You cannot update complaints of another department");
        }

        // Service handles notification to user
        return complaintService.updateStatus(id, status);
    }

    @PutMapping("/complaints/{id}/remarks")
    public Complaint addRemarks(
            @PathVariable Long id,
            @RequestParam String remarks,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userRepo.findByEmail(email);

        if (user == null || user.getDepartment() == null) {
            throw new RuntimeException("Unauthorized department admin");
        }

        Complaint complaint = complaintService.getComplaintById(id);

        // 🔒 Ensure same department
        if (!complaint.getDepartment().getId().equals(user.getDepartment().getId())) {
            throw new RuntimeException("You cannot add remarks to another department's complaint");
        }

        // Service handles notification to user
        return complaintService.addRemarks(id, remarks);
    }

        @GetMapping("/dashboard/stats")
    public Map<String, Long> getDepartmentDashboardStats(Authentication authentication) {

        User user = userRepo.findByEmail(authentication.getName());

        if (user == null || user.getDepartment() == null) {
            throw new RuntimeException("Unauthorized");
        }

        return complaintService.getDepartmentStats(user.getDepartment());
    }

    // 📊 Priority breakdown report
    @GetMapping("/reports/priority")
    public Map<String, Long> getPriorityBreakdown(Authentication authentication) {

        User user = userRepo.findByEmail(authentication.getName());

        if (user == null || user.getDepartment() == null) {
            throw new RuntimeException("Unauthorized");
        }

        return complaintService.getPriorityBreakdown(user.getDepartment());
    }

}
