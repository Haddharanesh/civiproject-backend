package com.civic.civicbackend.service;

import com.civic.civicbackend.model.Complaint;
import com.civic.civicbackend.model.Department;
import com.civic.civicbackend.model.User;
import com.civic.civicbackend.repository.ComplaintRepository;
import com.civic.civicbackend.repository.DepartmentRepository;
import com.civic.civicbackend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.civic.civicbackend.dto.AdminRequest;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SuperAdminService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ComplaintRepository complaintRepository;
private final PasswordEncoder encoder;
    
    public SuperAdminService(UserRepository userRepository, 
                        DepartmentRepository departmentRepository,
                        ComplaintRepository complaintRepository,
                        PasswordEncoder encoder) {
    this.userRepository = userRepository;
    this.departmentRepository = departmentRepository;
    this.complaintRepository = complaintRepository;
    this.encoder = encoder;
}

    // GET /users - Return all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // PUT /promote/{userId} - Change role to DEPARTMENT_ADMIN
    public User promoteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Cannot modify SUPER_ADMIN
        if ("SUPER_ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Cannot modify SUPER_ADMIN");
        }
        
        // If already DEPARTMENT_ADMIN, do nothing
        if ("DEPARTMENT_ADMIN".equals(user.getRole())) {
            return user;
        }
        
        // Set role to DEPARTMENT_ADMIN
        user.setRole("DEPARTMENT_ADMIN");
        return userRepository.save(user);
    }

    // PUT /demote/{userId} - Change role to USER
    public User demoteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Cannot demote a SUPER_ADMIN
        if ("SUPER_ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Cannot demote SUPER_ADMIN");
        }
        
        // Only allow demoting DEPARTMENT_ADMIN to USER
        if ("USER".equals(user.getRole())) {
            return user;
        }
        
        user.setRole("USER");
        return userRepository.save(user);
    }

    // GET /departments - Return all departments
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    // POST /departments - Create new department
    public Department createDepartment(String name, String description) {
        Department department = new Department();
        department.setName(name);
        department.setDescription(description);
        return departmentRepository.save(department);
    }

    // DELETE /departments/{id} - Delete department
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));
        
        // Check if any user has this department assigned
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            if (user.getDepartment() != null && user.getDepartment().getId().equals(id)) {
                throw new RuntimeException("Department has assigned admins");
            }
        }
        
        // Check if any complaint belongs to this department
        List<Complaint> complaints = complaintRepository.findByDepartment(department);
        if (!complaints.isEmpty()) {
            throw new RuntimeException("Department has complaints");
        }
        
        departmentRepository.deleteById(id);
    }

    // GET /complaints - Return all complaints
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    // DELETE /complaints/{id} - Delete any complaint
    public void deleteComplaint(Long id) {
        if (!complaintRepository.existsById(id)) {
            throw new RuntimeException("Complaint not found");
        }
        complaintRepository.deleteById(id);
    }

    public User createDepartmentAdmin(AdminRequest request) {

    // 🔍 Check email already exists
    if (userRepository.findByEmail(request.getEmail()) != null) {
        throw new RuntimeException("Email already registered");
    }

    // 📌 Get department
    Department department = departmentRepository.findById(request.getDepartmentId())
            .orElseThrow(() -> new RuntimeException("Department not found"));

    // 👤 Create new user
    User user = new User();
    user.setName(request.getName());
    user.setEmail(request.getEmail());

    // 🔐 Encrypt password (VERY IMPORTANT)
    user.setPassword(encoder.encode(request.getPassword()));

    // 🎯 Assign role
    user.setRole("DEPARTMENT_ADMIN");

    // 🔗 Link department
    user.setDepartment(department);

    return userRepository.save(user);
}
}

