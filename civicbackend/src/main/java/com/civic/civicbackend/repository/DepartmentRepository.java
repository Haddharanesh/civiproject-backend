package com.civic.civicbackend.repository;

import com.civic.civicbackend.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByName(String name);
    
    // Case-insensitive department lookup for AI predictions
    Optional<Department> findByNameIgnoreCase(String name);
}
