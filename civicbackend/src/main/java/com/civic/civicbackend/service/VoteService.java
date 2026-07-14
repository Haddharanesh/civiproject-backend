package com.civic.civicbackend.service;

import com.civic.civicbackend.model.Complaint;
import com.civic.civicbackend.model.Vote;
import com.civic.civicbackend.repository.ComplaintRepository;
import com.civic.civicbackend.repository.VoteRepository;
import org.springframework.stereotype.Service;

@Service
public class VoteService {

    private final VoteRepository voteRepo;
    private final ComplaintRepository complaintRepo;

    public VoteService(VoteRepository voteRepo, ComplaintRepository complaintRepo) {
        this.voteRepo = voteRepo;
        this.complaintRepo = complaintRepo;
    }

    public Complaint upvote(Long userId, Long complaintId) {

    if (voteRepo.existsByUserIdAndComplaintId(userId, complaintId)) {
        throw new RuntimeException("You already voted for this complaint");
    }

    Vote vote = new Vote();
    vote.setUserId(userId);
    vote.setComplaintId(complaintId);
    voteRepo.save(vote);

    Complaint c = complaintRepo.findById(complaintId)
            .orElseThrow(() -> new RuntimeException("Complaint not found"));

    c.setUpvotes(c.getUpvotes() + 1);
    return complaintRepo.save(c);
}

}
