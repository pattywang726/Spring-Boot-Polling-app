package com.example.polls.controller;

import com.example.polls.payload.*;
import com.example.polls.model.*;
import com.example.polls.repository.PollRepository;
import com.example.polls.repository.UserRepository;
import com.example.polls.repository.VoteRepository;
import com.example.polls.security.CurrentUser;
import com.example.polls.security.UserPrincipal;
import com.example.polls.service.PollService;
import com.example.polls.util.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;


@Controller
@RequestMapping("/api/polls")
public class PollController {
    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PollService poolService;

    private static final Logger logger = LoggerFactory.getLogger(PollController.class);

    // Get a paginated list of polls sorted by their creation time.
    @GetMapping
    public PagedResponse<PollResponse> getPolls(@CurrentUser UserPrincipal user,
                                                @RequestParam(value = "page", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
                                                @RequestParam(value = "size", defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        return poolService.getAllPools(user, page, size);
    }

    // Create a Poll.
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createPoll(@Valid @RequestBody PollRequest pollRequest) {
        Poll poll = poolService.createPoll(pollRequest);

        // Re-direct the path to the /api/polls/{pollId}..
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/{pollId}")
                .buildAndExpand(poll.getId()).toUri();

        // and also return the Body --> ApiResponse
        return ResponseEntity.created(location)
                .body(new ApiResponse(true, "Poll Created Successfully!"));
    }

    // Get a Poll by pollId. Get a paginated list of polls, and pull one out.
    @GetMapping("/{pollId}")
    public PollResponse getPollById (@CurrentUser UserPrincipal user,
                                     @PathVariable Long pollId) {
        return poolService.getPollById(pollId, user);
    }

    //Vote for a choice in a poll.
    @PostMapping("/{pollId}/votes")
    public PollResponse castVote (@CurrentUser UserPrincipal userPrincipal,
                                  @PathVariable Long pollId,
                                  @Valid @RequestBody VoteRequest voteRequest) {
        return poolService.castVoteAndGetUpdatedPoll(pollId, voteRequest, userPrincipal);
    }

}
