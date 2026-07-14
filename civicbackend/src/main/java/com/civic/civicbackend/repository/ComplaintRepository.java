package com.civic.civicbackend.repository;

import com.civic.civicbackend.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.civic.civicbackend.model.Department;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByStatus(String status);

    List<Complaint> findAllByOrderByIdDesc();

    List<Complaint> findByUserId(Long userId);

    List<Complaint> findByDepartment(Department department);

    long countByDepartment(Department department);

    long countByDepartmentAndStatus(Department department, String status);

    long countByDepartmentAndPriority(Department department, String priority);


    long countByStatus(String status);

    List<Complaint> findByDepartmentAndStatus(Department department, String status);

    @Query("SELECT c.priority, COUNT(c) FROM Complaint c WHERE c.department = :department GROUP BY c.priority")
    List<Object[]> getPriorityBreakdown(@Param("department") Department department);

}
