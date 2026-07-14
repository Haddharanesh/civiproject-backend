package com.civic.civicbackend.controller;

import com.civic.civicbackend.model.Complaint;
import com.civic.civicbackend.model.User;
import com.civic.civicbackend.repository.UserRepository;
import com.civic.civicbackend.service.ComplaintService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin
public class ComplaintController {

    private final ComplaintService service;
    private final UserRepository userRepo;

    public ComplaintController(ComplaintService service, UserRepository userRepo) {
        this.service = service;
        this.userRepo = userRepo;
    }

    // 🚀 CREATE COMPLAINT WITH IMAGE + AUTO ROUTING
    @PostMapping(consumes = {"multipart/form-data"})
    public Complaint createComplaint(
            @RequestParam String description,
            @RequestParam String category,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false) MultipartFile image,
            org.springframework.security.core.Authentication authentication   // 👈 ADD THIS
    ) throws IOException {

        System.out.println("CONTROLLER CREATE HIT");

        Complaint complaint = new Complaint();
        complaint.setDescription(description);
        complaint.setCategory(category);
        complaint.setLatitude(latitude);
        complaint.setLongitude(longitude);

        // 📸 Save image if present
        if (image != null && !image.isEmpty()) {
            String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
            Path uploadPath = Paths.get("uploads");
            Files.createDirectories(uploadPath);
            Files.write(uploadPath.resolve(fileName), image.getBytes());
            complaint.setImageUrl("uploads/" + fileName);
        }
        String email = authentication.getName(); 
        // 🔥 IMPORTANT — use createComplaint (auto department routing happens here)
        return service.createComplaint(complaint, email);
    }

    // 🔍 GET ONE
    @GetMapping("/{id}")
    public Complaint getComplaintById(@PathVariable Long id) {
        return service.getComplaintById(id);
    }

    // 🔄 UPDATE STATUS (Admin)
    @PutMapping("/{id}/status")
    public Complaint updateStatus(@PathVariable Long id, @RequestParam String status) {
        return service.updateStatus(id, status);
    }

    // 🖼 Upload/Replace Image Later
    @PostMapping("/{id}/image")
    public Complaint uploadImage(@PathVariable Long id, @RequestParam MultipartFile file) throws IOException {
        return service.uploadImage(id, file);
    }

    // 📊 FILTER BY STATUS
    @GetMapping("/filter")
    public List<Complaint> filterByStatus(@RequestParam String status) {
        return service.getByStatus(status);
    }

    // 📈 DASHBOARD STATS
    @GetMapping("/stats")
    public Map<String, Long> getStats() {
        return service.getStats();
    }

    // 🗂 ADMIN LIST
    @GetMapping
    public List<Complaint> getAllComplaints() {
        return service.getAllComplaints();
    }

    // 🌍 PUBLIC FEED
    @GetMapping("/public")
    public List<Complaint> getPublicComplaints() {
        return service.getPublicComplaints();
    }
    @GetMapping("/my")
    public List<Complaint> getMyComplaints(org.springframework.security.core.Authentication authentication) {
        String email = authentication.getName();
        return service.getComplaintsByUserEmail(email);
    }
    @GetMapping("/complaints/filter")
public List<Complaint> getFilteredComplaints(
        @RequestParam String status,
        Authentication authentication) {

    User user = userRepo.findByEmail(authentication.getName());

    if (user == null || user.getDepartment() == null) {
        throw new RuntimeException("Unauthorized");
    }

    return service.getComplaintsByDepartmentAndStatus(
            user.getDepartment(), status);
}



}
