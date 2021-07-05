package com.example.polls.repository;


import com.example.polls.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Add Custom Method to JPARepository with Generics : If a repository is
    // to be injected Spring Data creates a proxy implementing all the methods
    // declared in your repository. In that proxy Spring Data will analyse the
    // method call, decide which of the cases above applies and execute it

    Optional<User> findByEmail(String email);
    // If a value is present, and the value matches the given predicate, return an
    // Optional describing the value, otherwise return an empty Optional.

    Optional<User> findByUsernameOrEmail(String username, String email);

    List<User> findByIdIn(List<Long> userIds);

    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);
}

