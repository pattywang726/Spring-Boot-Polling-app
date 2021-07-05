package com.example.polls.payload;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

public class PollRequest {
    @NotBlank // a constrained String is valid as long
    // as it's not null and the trimmed length is greater than zero
    @Size(max = 140)
    private String question;

    @NotNull // a constrained CharSequence, Collection, Map, or
    // Array is valid as long as it's not null, but it can be empty
    @Size(min = 2, max = 6)
    private List<ChoiceRequest> choices;

    @NotNull
    @Valid
    private PollLength pollLength; // How long the poll is valid for voting.

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<ChoiceRequest> getChoices() {
        return choices;
    }

    public void setChoices(List<ChoiceRequest> choices) {
        this.choices = choices;
    }

    public PollLength getPollLength() {
        return pollLength;
    }

    public void setPollLength(PollLength pollLength) {
        this.pollLength = pollLength;
    }

}
