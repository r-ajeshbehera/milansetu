package com.rajesh.milansetu.repository;

import com.rajesh.milansetu.entity.Role;
import com.rajesh.milansetu.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("select u from User u where (:role is null or u.role = :role)")
    Page<User> search(@Param("role") Role role, Pageable pageable);
}
