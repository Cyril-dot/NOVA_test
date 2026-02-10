package com.novaTech.Nova.Services.MessagingChatService;

import com.novaTech.Nova.Entities.Enums.FriendshipStatus;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.chats.Friendship;
import com.novaTech.Nova.Entities.repo.FriendshipRepository;
import com.novaTech.Nova.Entities.repo.UserRepo;
import com.novaTech.Nova.Exceptions.BadRequestException;
import com.novaTech.Nova.Exceptions.ResourceNotFoundException;
import com.novaTech.Nova.Exceptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepo userRepository;
    private final WebSocketMessageService webSocketMessageService;

    /**
     * Find user by username or email
     */
    private User findUserByUsernameOrEmail(String identifier) {
        return userRepository.findByUsername(identifier)
                .orElseGet(() -> userRepository.findByEmail(identifier)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "User not found with username or email: " + identifier)));
    }

    /**
     * Send friend request by username or email
     */
    @Transactional
    public Friendship sendFriendRequestByIdentifier(UUID requesterId, String addresseeIdentifier) {
        log.info("Sending friend request from {} to {}", requesterId, addresseeIdentifier);

        // Find addressee by username or email
        User addressee = findUserByUsernameOrEmail(addresseeIdentifier);

        return sendFriendRequest(requesterId, addressee.getId());
    }

    /**
     * Original send friend request method
     */
    @Transactional
    public Friendship sendFriendRequest(UUID requesterId, UUID addresseeId) {
        if (requesterId.equals(addresseeId)) {
            throw new BadRequestException("Cannot send friend request to yourself");
        }

        friendshipRepository.findByUsers(requesterId, addresseeId).ifPresent(f -> {
            if (f.getStatus() == FriendshipStatus.PENDING) {
                throw new BadRequestException("Friend request already exists");
            } else if (f.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new BadRequestException("Users are already friends");
            }
        });

        Friendship friendship = Friendship.builder()
                .requesterId(requesterId)
                .addresseeId(addresseeId)
                .build();

        Friendship saved = friendshipRepository.save(friendship);

        // Notify via WebSocket
        webSocketMessageService.sendFriendRequest(addresseeId, saved);

        log.info("Friend request sent successfully: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Friendship acceptFriendRequest(Long friendshipId, UUID userId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));

        if (!friendship.getAddresseeId().equals(userId)) {
            throw new UnauthorizedException("Not authorized to accept this friend request");
        }

        if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
            throw new BadRequestException("Friend request already accepted");
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendship.setRespondedAt(LocalDateTime.now());

        Friendship saved = friendshipRepository.save(friendship);

        // Notify requester
        webSocketMessageService.sendFriendAccepted(friendship.getRequesterId(), saved);

        log.info("Friend request accepted: {}", saved.getId());
        return saved;
    }

    /**
     * Reject/decline friend request
     */
    @Transactional
    public void rejectFriendRequest(Long friendshipId, UUID userId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));

        if (!friendship.getAddresseeId().equals(userId)) {
            throw new UnauthorizedException("Not authorized to reject this friend request");
        }

        friendship.setStatus(FriendshipStatus.DECLINED);
        friendship.setRespondedAt(LocalDateTime.now());
        friendshipRepository.save(friendship);

        log.info("Friend request rejected: {}", friendshipId);
    }

    /**
     * Remove/unfriend a user by identifier
     */
    @Transactional
    public void removeFriendByIdentifier(UUID userId, String friendIdentifier) {
        User friend = findUserByUsernameOrEmail(friendIdentifier);
        removeFriend(userId, friend.getId());
    }

    /**
     * Remove/unfriend a user
     */
    @Transactional
    public void removeFriend(UUID userId, UUID friendId) {
        Friendship friendship = friendshipRepository.findByUsers(userId, friendId)
                .orElseThrow(() -> new ResourceNotFoundException("Friendship not found"));

        if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new BadRequestException("Users are not friends");
        }

        friendshipRepository.delete(friendship);
        log.info("Friendship removed between {} and {}", userId, friendId);
    }

    /**
     * Get all friends with their user details
     */
    public List<FriendshipDTO> getFriendsWithDetails(UUID userId) {
        List<Friendship> friendships = friendshipRepository.findByUserIdAndStatus(userId, FriendshipStatus.ACCEPTED);

        return friendships.stream().map(friendship -> {
            UUID friendId = friendship.getRequesterId().equals(userId)
                    ? friendship.getAddresseeId()
                    : friendship.getRequesterId();

            User friend = userRepository.findById(friendId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + friendId));

            return FriendshipDTO.builder()
                    .friendshipId(friendship.getId())
                    .userId(friend.getId())
                    .username(friend.getUsername())
                    .email(friend.getEmail())
                    .createdAt(friendship.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Get pending requests with requester details
     */
    public List<FriendRequestDTO> getPendingRequestsWithDetails(UUID userId) {
        List<Friendship> requests = friendshipRepository.findByAddresseeIdAndStatus(userId, FriendshipStatus.PENDING);

        return requests.stream().map(friendship -> {
            User requester = userRepository.findById(friendship.getRequesterId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + friendship.getRequesterId()));

            return FriendRequestDTO.builder()
                    .friendshipId(friendship.getId())
                    .requesterId(requester.getId())
                    .requesterUsername(requester.getUsername())
                    .requesterEmail(requester.getEmail())
                    .requestedAt(friendship.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Get sent friend requests with addressee details
     */
    public List<FriendRequestDTO> getSentRequestsWithDetails(UUID userId) {
        List<Friendship> requests = friendshipRepository.findByRequesterIdAndStatus(userId, FriendshipStatus.PENDING);

        return requests.stream().map(friendship -> {
            User addressee = userRepository.findById(friendship.getAddresseeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + friendship.getAddresseeId()));

            return FriendRequestDTO.builder()
                    .friendshipId(friendship.getId())
                    .requesterId(addressee.getId())
                    .requesterUsername(addressee.getUsername())
                    .requesterEmail(addressee.getEmail())
                    .requestedAt(friendship.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Check if users are friends by identifier
     */
    public boolean areFriendsByIdentifier(UUID userId, String friendIdentifier) {
        try {
            User friend = findUserByUsernameOrEmail(friendIdentifier);
            return areFriends(userId, friend.getId());
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    /**
     * Check if users are friends
     */
    public boolean areFriends(UUID user1, UUID user2) {
        return friendshipRepository.findByUsers(user1, user2)
                .map(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                .orElse(false);
    }

    /**
     * Original methods for backward compatibility
     */
    public List<Friendship> getFriends(UUID userId) {
        return friendshipRepository.findByUserIdAndStatus(userId, FriendshipStatus.ACCEPTED);
    }

    public List<Friendship> getPendingRequests(UUID userId) {
        return friendshipRepository.findByAddresseeIdAndStatus(userId, FriendshipStatus.PENDING);
    }

    // ===== DTOs =====

    @lombok.Data
    @lombok.Builder
    public static class FriendshipDTO {
        private Long friendshipId;
        private UUID userId;
        private String username;
        private String email;
        private LocalDateTime createdAt;
    }

    @lombok.Data
    @lombok.Builder
    public static class FriendRequestDTO {
        private Long friendshipId;
        private UUID requesterId;
        private String requesterUsername;
        private String requesterEmail;
        private LocalDateTime requestedAt;
    }
}