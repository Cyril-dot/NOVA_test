package com.novaTech.Nova.Security.entity;

import com.novaTech.Nova.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepo extends JpaRepository<RefreshToken, Long> {

    // Find refresh token by User entity
    Optional<RefreshToken> findByUser(User user);

    // Upsert refresh token for a user (PostgreSQL syntax)
    @Modifying
    @Transactional
    @Query(
            value = """
            INSERT INTO refresh_tokens (user_id, token, expiry_date)
            VALUES (:userId, :token, :expiry)
            ON CONFLICT (user_id)
            DO UPDATE SET
                token = EXCLUDED.token,
                expiry_date = EXCLUDED.expiry_date
        """,
            nativeQuery = true
    )
    void upsertUserRefreshToken(
            @Param("userId") UUID userId,
            @Param("token") String token,
            @Param("expiry") Instant expiry
    );
}
