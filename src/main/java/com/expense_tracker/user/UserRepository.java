package com.expense_tracker.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByEmail(String email);

    boolean existsByName(String name);
}
