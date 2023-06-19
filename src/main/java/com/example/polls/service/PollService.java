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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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

    public PollResponse castVoteAndGetUpdatedPoll(Long pollId, VoteRequest voteRequest, UserPrincipal currentUser) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResourcesNotFoundException("Poll", "id", pollId));
        if (poll.getExpirationDateTime().isBefore(Instant.now())) {
            throw new BadRequestException("Sorry! This Poll has already expired");
        }

        User user = userRepository.getById(currentUser.getId());

        Choice selectedChoice = poll.getChoices().stream()
                .filter(choice -> choice.getId().equals(voteRequest.getChoiceId()))
                .findFirst()
                .orElseThrow(() -> new ResourcesNotFoundException("Choice", "id", voteRequest.getChoiceId()));

        Vote vote = new Vote();
        vote.setPoll(poll);
        vote.setUser(user);
        vote.setChoice(selectedChoice);

        try {
            vote = voteRepository.save(vote);
        } catch (DataIntegrityViolationException ex) {
            logger.info("User {} has already voted in PolL {}", currentUser.getId(), pollId);
            throw new BadRequestException("Sorry! You have already cast your vote in this poll!");
        }

        // After the vote has been saved, then update the Poll Response
        List<ChoiceVoteCount> votes = voteRepository.countByPollIdGroupByChoiceId(pollId);

        Map<Long, Long> choiceVotesMap = votes.stream()
                .collect(Collectors.toMap(ChoiceVoteCount::getChoiceId, ChoiceVoteCount::getVoteCount));

        // Retrieve the user(poll creator) details
        User creator = userRepository.findById(poll.getCreatedBy())
                .orElseThrow(() -> new ResourcesNotFoundException("User", "id", poll.getCreatedBy()));
        return ModelMapper.mapPollToPollResponse(poll, choiceVotesMap, creator, vote.getChoice().getId());
    }

    public PagedResponse<PollResponse> getPollsCreatedBy(String username, UserPrincipal currentUser, int page, int size) {
        validatePageNumberAndSize(page, size);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourcesNotFoundException("User", "username", username));

        // Obtain all the polls created by the given username
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Poll> polls = pollRepository.findByCreatedBy(user.getId(), pageable);

        // no polls found under this user
        if (polls.getTotalElements() == 0) {
            return new PagedResponse<>(Collections.emptyList(), polls.getNumber(), polls.getSize(),
                    polls.getTotalElements(), polls.getTotalPages(), polls.isLast());
        }
        // map polls to pollResponses
        List<Long> pollIds = polls.map(Poll::getId).getContent();
        Map<Long, Long> choiceVoteCountMap = getChoiceVoteCountMap(pollIds);
        Map<Long, Long> pollUserVoteMap = getPollUserVoteMap(currentUser, pollIds);

        List<PollResponse> pollResponses = polls.map(poll -> {
            return ModelMapper.mapPollToPollResponse(poll, choiceVoteCountMap,
                    user,
                    pollUserVoteMap == null ? null : pollUserVoteMap.getOrDefault(poll.getId(), null));
        }).getContent();

        return new PagedResponse<>(pollResponses, polls.getNumber(), polls.getSize(),
                polls.getTotalElements(), polls.getTotalPages(), polls.isLast());
    }

    public PagedResponse<PollResponse> getPollsVotedBy(String username, UserPrincipal currentUser, int page, int size) {
        validatePageNumberAndSize(page, size);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourcesNotFoundException("User", "username", username));

        // Obtain all the polls voted by the given username
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Long> votedPollIds = voteRepository.findVotedPollIdsByUserId(user.getId(), pageable);

        // no polls voted by this user
        if (votedPollIds.getTotalElements() == 0) {
            return new PagedResponse<>(Collections.emptyList(), votedPollIds.getNumber(), votedPollIds.getSize(),
                    votedPollIds.getTotalElements(), votedPollIds.getTotalPages(), votedPollIds.isLast());
        }

        List<Long> pollIds = votedPollIds.getContent();
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        List<Poll> polls = pollRepository.findByIdIn(pollIds, sort);

        // map polls to pollResponses
        Map<Long, Long> choiceVoteCountMap = getChoiceVoteCountMap(pollIds);  //choiceId -> count
        Map<Long, Long> pollUserVoteMap = getPollUserVoteMap(currentUser, pollIds);  //pollId -> voteId
        Map<Long, User> creatorMap = getPollCreatorMap(polls); //userId -> User

        List<PollResponse> pollResponses = polls.stream().map(poll -> {
            return ModelMapper.mapPollToPollResponse(poll, choiceVoteCountMap,
                    creatorMap.get(poll.getCreatedBy()),
                    pollUserVoteMap == null ? null : pollUserVoteMap.getOrDefault(poll.getId(), null));
        }).collect(Collectors.toList());

        return new PagedResponse<>(pollResponses, votedPollIds.getNumber(), votedPollIds.getSize(),
                votedPollIds.getTotalElements(), votedPollIds.getTotalPages(), votedPollIds.isLast());
    }

    private void validatePageNumberAndSize(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page number cannot be less than zero");
        }
        if (size > AppConstants.MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size must not be greater than " + AppConstants.MAX_PAGE_SIZE);
        }
    }

    private Map<Long, Long> getPollUserVoteMap(UserPrincipal currentUser, List<Long> pollIds) {
        // Retrieve votes done by the logged in user; if no logged in user, return null Map.
        Map<Long, Long> userVoteMap = null;
        if (currentUser != null)  {
            List<Vote> userVotes = voteRepository.findByUserIdAndPollIdIn(currentUser.getId(), pollIds);

            userVoteMap = userVotes.stream()
                    .collect(Collectors.toMap(vote -> vote.getPoll().getId(), vote -> vote.getChoice().getId()));
        }
        return userVoteMap;
    }

    private Map<Long, Long> getChoiceVoteCountMap(List<Long> pollIds) {
        List<ChoiceVoteCount> choiceVoteCounts = voteRepository.countByPollIdInGroupByChoiceId(pollIds);
        //we can use for loop, like looping over List, and construct a new Map.
        Map<Long, Long> choiceVoteCountMap = choiceVoteCounts.stream()
                .collect(Collectors.toMap(ChoiceVoteCount::getChoiceId, ChoiceVoteCount::getVoteCount));
        return choiceVoteCountMap;
    }

    Map<Long, User> getPollCreatorMap(List<Poll> polls) {
        // get the poll creator details corresponding to each poll in the list
        List<Long> creatorIds = polls.stream()
                .map(Poll::getCreatedBy)
                .distinct()
                .collect(Collectors.toList());

        List<User> creators = userRepository.findByIdIn(creatorIds);
        Map<Long, User> creatorMap = creators.stream().
                collect(Collectors.toMap(User::getId, Function.identity()));
        return creatorMap;
    }
}
