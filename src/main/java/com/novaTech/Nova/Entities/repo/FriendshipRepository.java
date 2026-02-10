package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.Enums.FriendshipStatus;
import com.novaTech.Nova.Entities.chats.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    

    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.requesterId = :userId OR f.addresseeId = :userId) " +
           "AND f.status = :status")
    List<Friendship> findByUserIdAndStatus(@Param("userId") UUID userId,
                                          @Param("status") FriendshipStatus status);
    
    List<Friendship> findByAddresseeIdAndStatus(UUID addresseeId, FriendshipStatus status);
    
    List<Friendship> findByRequesterIdAndStatus(UUID requesterId, FriendshipStatus status);


    /**
     * Find friendship between two users (regardless of who initiated)
     */
    @Query("SELECT f FROM Friendship f WHERE " +
            "(f.requesterId = :user1 AND f.addresseeId = :user2) OR " +
            "(f.requesterId = :user2 AND f.addresseeId = :user1)")
    Optional<Friendship> findByUsers(@Param("user1") UUID user1, @Param("user2") UUID user2);

    /**
     * Find all friendships for a user with specific status
     */


    /**
     * Find all accepted friendships for a user
     */
    @Query("SELECT f FROM Friendship f WHERE " +
            "(f.requesterId = :userId OR f.addresseeId = :userId) AND " +
            "f.status = 'ACCEPTED'")
    List<Friendship> findAcceptedFriendships(@Param("userId") UUID userId);

    /**
     * Count friends for a user
     */
    @Query("SELECT COUNT(f) FROM Friendship f WHERE " +
            "(f.requesterId = :userId OR f.addresseeId = :userId) AND " +
            "f.status = 'ACCEPTED'")
    long countFriends(@Param("userId") UUID userId);

    /**
     * Count pending incoming requests
     */
    @Query("SELECT COUNT(f) FROM Friendship f WHERE " +
            "f.addresseeId = :userId AND f.status = 'PENDING'")
    long countPendingRequests(@Param("userId") UUID userId);

    /**
     * Count pending outgoing requests
     */
    @Query("SELECT COUNT(f) FROM Friendship f WHERE " +
            "f.requesterId = :userId AND f.status = 'PENDING'")
    long countSentRequests(@Param("userId") UUID userId);

    /**
     * Check if two users are friends
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f WHERE " +
            "((f.requesterId = :user1 AND f.addresseeId = :user2) OR " +
            "(f.requesterId = :user2 AND f.addresseeId = :user1)) AND " +
            "f.status = 'ACCEPTED'")
    boolean areFriends(@Param("user1") UUID user1, @Param("user2") UUID user2);

    /**
     * Delete friendship between two users
     */
    @Query("DELETE FROM Friendship f WHERE " +
            "(f.requesterId = :user1 AND f.addresseeId = :user2) OR " +
            "(f.requesterId = :user2 AND f.addresseeId = :user1)")
    void deleteByUsers(@Param("user1") UUID user1, @Param("user2") UUID user2);
}
