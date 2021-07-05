package com.example.polls.payload;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class UserProfile {
    private Long id;
    private String username;
    private String name;
//    private String email;
    private Instant joinedAt;
    private Long pollCount;
    private Long voteCount;
}
