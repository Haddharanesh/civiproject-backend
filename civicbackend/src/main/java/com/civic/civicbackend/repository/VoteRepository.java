package com.civic.civicbackend.repository;

import com.civic.civicbackend.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    boolean existsByUserIdAndComplaintId(Long userId, Long complaintId);
}
