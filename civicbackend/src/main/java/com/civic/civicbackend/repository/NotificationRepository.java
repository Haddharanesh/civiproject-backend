package com.civic.civicbackend.repository;

import com.civic.civicbackend.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserId(Long userId);

    List<Notification> findByDepartmentId(Long departmentId);

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByDepartmentIdOrderByCreatedAtDesc(Long departmentId);

    long countByUserIdAndIsReadFalse(Long userId);

    long countByDepartmentIdAndIsReadFalse(Long departmentId);
}
