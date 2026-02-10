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

    @Query("SELECT u FROM User u WHERE u.email = :email")
    User findByUserEmail(@Param("email")String email);



    /**
     * Find user by username or email (exact match)
     */
    @Query("SELECT u FROM User u WHERE u.username = :identifier OR u.email = :identifier")
    Optional<User> findByUsernameOrEmail(@Param("identifier") String identifier);

    /**
     * Search users by username or email (partial match - case insensitive)
     */
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> searchByUsernameOrEmail(@Param("searchTerm") String searchTerm);

    /**
     * Search users by username (partial match - case insensitive)
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))")
    List<User> searchByUsername(@Param("username") String username);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find users by username starting with (for autocomplete)
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT(:prefix, '%'))")
    List<User> findByUsernameStartingWith(@Param("prefix") String prefix);
}
