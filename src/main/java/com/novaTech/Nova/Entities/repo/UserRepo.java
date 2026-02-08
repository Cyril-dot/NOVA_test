package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.User;
import io.micrometer.core.instrument.binder.db.MetricsDSLContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepo extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    // Find user by email + MFA code + MFA secret
    Optional<User> findByEmailAndMfaCode(String email, Integer mfaCode);

    @Query(
            value = "SELECT * FROM users WHERE username ILIKE CONCAT('%', :username, '%')",
            nativeQuery = true
    )
    List<User> searchUsersNative(@Param("username") String username);

    Optional<User> findByUsername(String username);
}
