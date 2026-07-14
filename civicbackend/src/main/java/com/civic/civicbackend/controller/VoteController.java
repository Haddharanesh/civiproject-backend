package com.civic.civicbackend.controller;

import com.civic.civicbackend.model.Complaint;
import com.civic.civicbackend.service.VoteService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/votes")
@CrossOrigin
public class VoteController {

    private final VoteService service;

    public VoteController(VoteService service) {
        this.service = service;
    }

    @PostMapping
    public Complaint vote(@RequestParam Long userId, @RequestParam Long complaintId) {
        return service.upvote(userId, complaintId);
    }
}
