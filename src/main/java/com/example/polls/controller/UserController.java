package com.example.polls.controller;

import com.example.polls.exception.ResourcesNotFoundException;
import com.example.polls.model.*;
import com.example.polls.payload.*;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {
    @Autowired
    UserRepository userRepository;

    @Autowired
    PollRepository pollRepository;

    @Autowired
    VoteRepository voteRepository;

    @Autowired
    PollService pollService;

    private final static Logger logger = LoggerFactory.getLogger(UserController.class);

    // Get the currently logged in user.
    @GetMapping("/user/me")
    @PreAuthorize("hasRole('USER')")
    public UserSummary getCurrentUser(@CurrentUser UserPrincipal user) {
        return new UserSummary(user.getId(), user.getUsername(), user.getName());
    }

    // Check if a username is available for registration.
    @GetMapping("/user/checkUsernameAvailability")
    public UserIdentityAvailability checkUsernameAvailability(@RequestParam(value = "username") String username) {
        Boolean isAvailable = !userRepository.existsByUsername(username);
        return new UserIdentityAvailability(isAvailable);
    }

    // Check if an email is available for registration.
    @GetMapping("/user/checkEmailAvailability")
    public UserIdentityAvailability checkEmailAvailability(@RequestParam(value = "email") String email) {
        Boolean isAvailable = !userRepository.existsByEmail(email);
        return new UserIdentityAvailability(isAvailable);
    }

    //Get the public profile of a user.
    @GetMapping("/users/{username}")
    public UserProfile gerUserProfile(@PathVariable(value = "username") String username) {
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new ResourcesNotFoundException("User", "username", username)
        );

        // pollCount should be found by pollRepository, and voteCount
        // should be found by voteRepository.
        long pollCount = pollRepository.countByCreatedBy(user.getId());
        long voteCount = voteRepository.countByUserId(user.getId());

        UserProfile userProfile = new UserProfile(user.getId(), user.getUsername(), user.getName(),
               user.getCreatedAt(), pollCount, voteCount);

        return userProfile;
    }

    //Get a paginated list of polls created by a given user.
    @GetMapping("/users/{username}/polls")
    public PagedResponse<PollResponse> getPollsCreatedBy (@CurrentUser UserPrincipal user,
                                                          @PathVariable(value = "username") String username,
                                                          @RequestParam(value = "size", defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
                                                          @RequestParam(value = "page", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page) {

        return pollService.getPollsCreatedBy(username, user, page, size);
    }


    // Get a paginated list of polls in which a given user has voted.
    @GetMapping("/users/{username}/votes")
    public PagedResponse<PollResponse> getVotesCreatedBy (@CurrentUser UserPrincipal user,
                                                          @PathVariable(value = "username") String username,
                                                          @RequestParam(value = "size", defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
                                                          @RequestParam(value = "page", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page) {

        return pollService.getPollsVotedBy(username, user, page, size);
    }
}
