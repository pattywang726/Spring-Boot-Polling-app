package com.example.polls.util;

import com.example.polls.model.Poll;
import com.example.polls.model.User;
import com.example.polls.payload.ChoiceResponse;
import com.example.polls.payload.PollResponse;
import com.example.polls.payload.UserSummary;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModelMapper {

    public static PollResponse mapPollToPollResponse(Poll poll, Map<Long, Long> choiceVotesMap,
                                                    User creator, Long userVote) {
        PollResponse pollResponse = new PollResponse();
        pollResponse.setId(poll.getId());
        pollResponse.setQuestion(poll.getQuestion());
        pollResponse.setCreationDateTime(poll.getCreatedAt());
        pollResponse.setExpirationDateTime(poll.getExpirationDateTime());

        Instant now = Instant.now();
        pollResponse.setIsExpired(poll.getExpirationDateTime().isBefore(now));

        List<ChoiceResponse> choiceResponses = poll.getChoices().stream().map(choice -> {
            ChoiceResponse choiceResponse = new ChoiceResponse();
            choiceResponse.setId(choice.getId());
            choiceResponse.setText(choice.getText());

            if (choiceVotesMap.containsKey(choice.getId())) {
                Long count = choiceVotesMap.get(choice.getId());
                choiceResponse.setVoteCount(count);
            } else {
                choiceResponse.setVoteCount(0);
            }
            return choiceResponse;

        }).collect(Collectors.toList());
        pollResponse.setChoices(choiceResponses);

        UserSummary userSummary = new UserSummary(creator.getId(), creator.getName(),
                creator.getUsername());
        pollResponse.setCreatedBy(userSummary);

        // Which @JsonInclude is NotNull -> the choice that the currently
        // logged in user has voted for
        if (userVote != null) {
            pollResponse.setSelectedChoice(userVote);
        }

        long totalVotes = pollResponse.getChoices().stream().mapToLong(ChoiceResponse::getVoteCount).sum();
        // the totalVotes can also be obtained by looping over getChoice().
        pollResponse.setTotalVotes(totalVotes);

        return pollResponse;
    }
}
