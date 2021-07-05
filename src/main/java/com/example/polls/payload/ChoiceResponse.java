package com.example.polls.payload;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChoiceResponse {
    private String text;
    private long id;
    private long voteCount; // indicate how many votes voted for this Choice
}
