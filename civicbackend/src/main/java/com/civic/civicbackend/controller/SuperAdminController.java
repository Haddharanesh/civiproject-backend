package com.civic.civicbackend.controller;

import com.civic.civicbackend.model.Complaint;
import com.civic.civicbackend.model.Department;
import com.civic.civicbackend.model.User;
import com.civic.civicbackend.service.SuperAdminService;
import com.civic.civicbackend.dto.AdminRequest;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/superadmin")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    public SuperAdminController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
    }

    // GET /users - Return all users
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return superAdminService.getAllUsers();
    }

    // PUT /promote/{userId} - Change role to DEPARTMENT_ADMIN
    @PutMapping("/promote/{userId}")
    public User promoteUser(@PathVariable Long userId) {
        return superAdminService.promoteUser(userId);
    }

    // PUT /demote/{userId} - Change role to USER
    @PutMapping("/demote/{userId}")
    public User demoteUser(@PathVariable Long userId) {
        return superAdminService.demoteUser(userId);
    }

    // GET /departments - Return all departments
    @GetMapping("/departments")
    public List<Department> getAllDepartments() {
        return superAdminService.getAllDepartments();
    }

    @PostMapping("/create-admin")
    public User createDepartmentAdmin(@RequestBody AdminRequest request) {
        return superAdminService.createDepartmentAdmin(request);
}
    // POST /departments - Create new department
    @PostMapping("/departments")
    public Department createDepartment(@RequestBody Department department) {
        return superAdminService.createDepartment(department.getName(), department.getDescription());
    }

    // DELETE /departments/{id} - Delete department
    @DeleteMapping("/departments/{id}")
    public void deleteDepartment(@PathVariable Long id) {
        superAdminService.deleteDepartment(id);
    }

    // GET /complaints - Return all complaints
    @GetMapping("/complaints")
    public List<Complaint> getAllComplaints() {
        return superAdminService.getAllComplaints();
    }

    // DELETE /complaints/{id} - Delete any complaint
    @DeleteMapping("/complaints/{id}")
    public void deleteComplaint(@PathVariable Long id) {
        superAdminService.deleteComplaint(id);
    }
}
