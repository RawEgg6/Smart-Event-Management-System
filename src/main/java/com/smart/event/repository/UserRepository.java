package com.smart.event.repository;

import com.smart.event.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.smart.event.entity.Role;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

}
