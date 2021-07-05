package com.example.polls.service;

import com.example.polls.exception.BadRequestException;
import com.example.polls.exception.ResourcesNotFoundException;
import com.example.polls.model.*;
import com.example.polls.payload.*;
import com.example.polls.repository.PollRepository;
import com.example.polls.repository.UserRepository;
import com.example.polls.repository.VoteRepository;
import com.example.polls.security.UserPrincipal;
import com.example.polls.util.AppConstants;
import com.example.polls.util.ModelMapper;
import org.dom4j.rule.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// controllers PollController and UserController use the PollService
// class to get the list of polls formatted in the form of PollResponse
// payloads that is returned to the clients
@Service
public class PollService {
    @Autowired
    UserRepository userRepository;

    @Autowired
    PollRepository pollRepository;

    @Autowired
    VoteRepository voteRepository;

    private static final Logger logger = LoggerFactory.getLogger(PollService.class);

    public PagedResponse<PollResponse> getAllPools(UserPrincipal user, int page, int size) {
        validatePageNumberAndSize(page, size);
        // Retrieve Polls
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Poll> polls = pollRepository.findAll(pageable);

        // When no poll exists yet
        if (polls.getTotalElements() == 0) {
            return new PagedResponse<>(Collections.emptyList(), polls.getNumber(),
                    polls.getSize(), polls.getTotalElements(), polls.getTotalPages(), polls.isLast());
        }

        // Convert Poll to PollResponse
        List<Long> pollIds = polls.map(Poll::getId).getContent();
        Map<Long, Long> choiceVotesMap = getChoiceVoteCountMap(pollIds);
        Map<Long, Long> pollUserVoteMap = getPollUserVoteMap(user, pollIds);

        List<PollResponse> pollResponses = polls.map(poll -> {
                    return ModelMapper.mapPollToPollResponse(poll, choiceVotesMap,
                            userRepository.findById(poll.getCreatedBy()).orElseThrow(
                                    () -> new ResourcesNotFoundException("Poll", "id", poll.getId())),
                            pollUserVoteMap.get(poll.getId()));

        }).getContent();

        // Convert List<PollResponse> to PageResponse<>
        PagedResponse<PollResponse> pagedPollResponses = new PagedResponse<>(
                pollResponses, polls.getNumber(),
                polls.getSize(), polls.getTotalElements(),
                polls.getTotalPages(), polls.isLast());

        return pagedPollResponses;
    }

    private void validatePageNumberAndSize(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page number cannot be less than zero");
        }
        if (size > AppConstants.MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size must not be greater than " + AppConstants.MAX_PAGE_SIZE);
        }
    }

    private Map<Long, Long> getPollUserVoteMap(UserPrincipal user, List<Long> pollIds) {
        List<Vote> votesByUser = voteRepository.findByUserIdAndPollIdIn(user.getId(), pollIds);
        Map<Long, Long> UserVoteMap = votesByUser.stream()
                .collect(Collectors.toMap(vote -> vote.getPoll().getId(), vote -> vote.getChoice().getId()));
        return  UserVoteMap;
    }

    private Map<Long, Long> getChoiceVoteCountMap(List<Long> pollIds) {
        List<ChoiceVoteCount> choiceVoteCounts = voteRepository.countByPollIdInGroupByChoiceId(pollIds);
        //we can use for loop, like looping over List, and construct a new Map.
        Map<Long, Long> choiceVoteCountMap = choiceVoteCounts.stream()
                .collect(Collectors.toMap(ChoiceVoteCount::getChoiceId, ChoiceVoteCount::getVoteCount));
        return choiceVoteCountMap;
    }


    public Poll createPoll(PollRequest pollRequest) {
        Poll poll = new Poll();
        poll.setQuestion(pollRequest.getQuestion());

        PollLength length = pollRequest.getPollLength();
        Instant now = Instant.now();
        Instant expireTime = now.plus(Duration.ofDays(length.getDays()))
                .plus(Duration.ofHours(length.getHours()));
        poll.setExpirationDateTime(expireTime);

//        List<Choice> choices = pollRequest.getChoices().stream().map(choiceRequest -> {
//                    return new Choice(choiceRequest.getText());
//                }).collect(Collectors.toList());
        pollRequest.getChoices().forEach(choiceRequest -> {
            poll.addChoice(new Choice(choiceRequest.getText()));
        });

        return pollRepository.save(poll);
    }

    public PollResponse getPollById(Long pollId, UserPrincipal user) {
        Poll poll = pollRepository.findById(pollId).orElseThrow(
                () -> new ResourcesNotFoundException("Poll", "id", pollId));

        // Retrieve the Vote count for this poll
        List<ChoiceVoteCount> votes = voteRepository.countByPollIdGroupByChoiceId(pollId);
        Map<Long, Long> choiceVotesMap = votes.stream()
                .collect(Collectors.toMap(ChoiceVoteCount::getChoiceId, ChoiceVoteCount::getVoteCount));

        // Retrieve the vote done by the user in this poll
        Vote userVote = null;
        if (user != null) {
            userVote = voteRepository.findByUserIdAndPollId(user.getId(), pollId);
        }
        return ModelMapper.mapPollToPollResponse(poll, choiceVotesMap,
                userRepository.findById(poll.getCreatedBy()).orElseThrow(
                        () -> new ResourcesNotFoundException("Poll", "id", poll.getId())),
                        userVote != null ? userVote.getChoice().getId() : null);
    }

    public PollResponse castVoteAndGetUpdatedPoll(Long pollId, VoteRequest voteRequest, UserPrincipal userPrincipal) {
    }

    public PagedResponse<PollResponse> getPollsCreatedBy(String username, UserPrincipal user, int page, int size) {
    }

    public PagedResponse<PollResponse> getPollsVotedBy(String username, UserPrincipal user, int page, int size) {
    }
}
