package com.novaTech.Nova.Services;

import com.novaTech.Nova.DTO.*;
import com.novaTech.Nova.Entities.Enums.SearchMethod;
import com.novaTech.Nova.Entities.Enums.TeamStatus;
import com.novaTech.Nova.Entities.Team;
import com.novaTech.Nova.Entities.TeamMember;
import com.novaTech.Nova.Entities.User;
import com.novaTech.Nova.Entities.repo.TeamMemberRepository;
import com.novaTech.Nova.Entities.repo.TeamRepository;
import com.novaTech.Nova.Entities.repo.UserRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@CacheConfig(cacheNames = {"users", "teams"})
public class UserService {
    private final UserRepo userRepo;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final EmailService emailService;

    // to search for users , that is active users by thier username
    @Transactional(readOnly = true)
    @Cacheable(
            key = "#userId + '_search_' + #request.username",
            unless = "#result == null"
    )
    public List<SearchUserResponse> searchUser(SearchUserRequest request, UUID userId) {

        // Validate the requesting user exists
        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User with id {} not found", userId);
                    return new EntityNotFoundException("User with id " + userId + " not found");
                });

        // Search for users
        List<User> foundUsers = userRepo.searchUsersNative(request.getUsername());

        if (foundUsers.isEmpty()) {
            log.warn("User with name {} not found", request.getUsername());
            return new ArrayList<>();
        }

        // Map User entities to SearchUserResponse DTOs
        List<SearchUserResponse> responses = foundUsers.stream()
                .map(foundUser -> SearchUserResponse.builder()
                        .userName(foundUser.getUsername())
                        .userFirstName(foundUser.getFirstName())
                        .userLastName(foundUser.getLastName())
                        .userEmail(foundUser.getEmail())
                        .userProfilePic(foundUser.getProfileImage())
                        .build())
                .toList();

        log.info("Found {} users matching '{}'", responses.size(), request.getUsername());

        return responses;
    }

    // to create a team
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "teams", key = "'user:' + #userId + '_teams'"),
            @CacheEvict(cacheNames = "users", allEntries = true)  // Clear user search cache
    })
    public TeamResponse createTeam(CreateTeamRequest request, UUID userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User with id {} not found", userId);
                    return new EntityNotFoundException("User with id " + userId + " not found");
                });

        Team team = Team.builder()
                .name(request.getName())
                .description(request.getDescription())
                .user(user)
                .createdAt(LocalDateTime.now())
                .members(new HashSet<>())
                .build();

        // Save team first to get ID
        team = teamRepository.save(team);

        // Add owner as ADMIN
        TeamMember ownerMember = TeamMember.builder()
                .team(team)
                .user(user)
                .role(TeamStatus.ADMIN)
                .joinedAt(LocalDateTime.now())
                .build();

        team.getMembers().add(ownerMember);

        // Add other members if any
        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            List<User> membersToAdd = userRepo.findAllById(request.getMemberIds());
            for (User memberUser : membersToAdd) {
                // Skip if owner is in the list
                if (memberUser.getId().equals(user.getId())) continue;

                TeamMember member = TeamMember.builder()
                        .team(team)
                        .user(memberUser)
                        .role(TeamStatus.MEMBER)
                        .joinedAt(LocalDateTime.now())
                        .build();
                team.getMembers().add(member);

                // Send email to each added member
                    log.info("Sending team member added email to: {}", memberUser.getEmail());
                    emailService.addMemberMail(
                            memberUser.getEmail(),
                            team.getName(),
                            team.getDescription(),
                            TeamStatus.MEMBER
                    );
                    log.info("Team member added email sent successfully to: {}", memberUser.getEmail());
            }
        }

        // Save again to cascade members
        team = teamRepository.save(team);

        // Send team creation email to owner
        try {
            log.info("Sending team creation email to owner: {}", user.getEmail());
            emailService.teamCreatedSuccessfully(
                    user.getEmail(),
                    team.getName(),
                    team.getDescription(),
                    user.getUsername(),
                    TeamStatus.ADMIN
            );
            log.info("Team creation email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send team creation email to: {}. Error: {}",
                    user.getEmail(), e.getMessage(), e);
            // Team is still created successfully even if email fails
        }

        return mapToResponse(team);
    }

    // add team members
    @Transactional
    @Caching(evict = {
            @CacheEvict(key = "'team:' + #teamId"),
            @CacheEvict(key = "'team:' + #teamId + '_members'"),
            @CacheEvict(cacheNames = "users", allEntries = true)
    })
    public TeamResponse addMember(UUID teamId, AddTeamRequest request, UUID userId) {
        log.info("Request to add member to team {} by user {}", teamId, userId);

        User requester = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Requester with id {} not found", userId);
                    return new EntityNotFoundException("Requester not found");
                });

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> {
                    log.warn("Team with id {} not found", teamId);
                    return new EntityNotFoundException("Team not found");
                });

        // Check if requester is ADMIN of the team
        boolean isAdmin = team.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(userId) && m.getRole() == TeamStatus.ADMIN);

        if (!isAdmin && !team.getUser().getId().equals(userId)) {
            log.warn("User {} is not authorized to add members to team {}", userId, teamId);
            throw new RuntimeException("Only team admins can add members");
        }

        User memberUser;
        if (request.searchMethod() == SearchMethod.EMAIL) {
            memberUser = userRepo.findByEmail(request.memberIdentifier())
                    .orElseThrow(() -> {
                        log.warn("User with email {} not found", request.memberIdentifier());
                        return new EntityNotFoundException("Member not found with email: " + request.memberIdentifier());
                    });
        } else if (request.searchMethod() == SearchMethod.USERNAME) {
            memberUser = userRepo.findByUsername(request.memberIdentifier())
                    .orElseThrow(() -> {
                        log.warn("User with username {} not found", request.memberIdentifier());
                        return new EntityNotFoundException("Member not found with username: " + request.memberIdentifier());
                    });
        } else {
            log.warn("Invalid search method: {}", request.searchMethod());
            throw new RuntimeException("Invalid search method");
        }

        // Check if already a member
        boolean alreadyMember = team.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(memberUser.getId()));

        if (alreadyMember) {
            log.warn("User {} is already a member of team {}", memberUser.getId(), teamId);
            throw new RuntimeException("User is already a member of this team");
        }

        TeamMember member = TeamMember.builder()
                .user(memberUser)
                .team(team)
                .role(TeamStatus.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build();

        team.getMembers().add(member);
        teamRepository.save(team);

        // Send email to the new member

            log.info("Sending team member added email to: {}", memberUser.getEmail());
            emailService.addMemberMail(
                    memberUser.getEmail(),
                    team.getName(),
                    team.getDescription(),
                    TeamStatus.MEMBER
            );
            log.info("Team member added email sent successfully to: {}", memberUser.getEmail());

        log.info("Successfully added user {} to team {}", memberUser.getId(), teamId);

        return mapToResponse(team);
    }

    // to view team members
    @Cacheable(key = "'team:' + #teamId + '_members'")
    public TeamResponse viewTeamMembers(UUID teamId, UUID userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));

        // Check if user is a member
        boolean isMember = team.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(userId));

        if (!isMember) {
            log.warn("User with id {} is not a member of team with id {}", userId, teamId);
            throw new RuntimeException("User is not a member of this team");
        }

        return mapToResponse(team);
    }

    // number of teams joined by a user
    @Cacheable(key = "'user:' + #userId + '_teamCount'")
    public int numberOfTeamsJoined(UUID userId){
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        List<TeamMember> memberships = teamMemberRepository.findByUser(user);

        log.info("User with id {} has joined {} teams", userId, memberships.size());

        return memberships.size();
    }

    // to delete team
    @Transactional
    @Caching(evict = {
            @CacheEvict(key = "'team:' + #teamId"),
            @CacheEvict(key = "'team:' + #teamId + '_members'"),
            @CacheEvict(cacheNames = "users", allEntries = true),
            @CacheEvict(key = "'user:' + #userId + '_teams'"),
            @CacheEvict(key = "'user:' + #userId + '_teamCount'")
    })
    public void deleteTeam(UUID teamId, UUID userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));

        // to check if the user is an admin
        boolean isAdmin = team.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(userId) && m.getRole() == TeamStatus.ADMIN);

        if (!team.getUser().getId().equals(user.getId()) && !isAdmin) {
            throw new RuntimeException("Only the owner can delete the team");
        }

        // Send email to all team members before deletion
        String teamName = team.getName();
        for (TeamMember member : team.getMembers()) {

                log.info("Sending team deletion notification email to: {}", member.getUser().getEmail());
                emailService.deleteTeam(member.getUser().getEmail(), teamName);
                log.info("Team deletion email sent successfully to: {}", member.getUser().getEmail());
        }

        teamRepository.delete(team);
        log.info("Team with id {} deleted by user {}", teamId, userId);
    }

    // to remove team members
    @Transactional
    @Caching(evict = {
            @CacheEvict(key = "'team:' + #teamId"),
            @CacheEvict(key = "'team:' + #teamId + '_members'"),
            @CacheEvict(key = "'user:' + #memberId + '_teams'"),
            @CacheEvict(key = "'user:' + #memberId + '_teamCount'")
    })
    public TeamResponse removeMember(UUID teamId, UUID memberId, UUID userId) {
        User requester = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Requester not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));

        // Check if requester is ADMIN
        boolean isAdmin = team.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(requester.getId()) && m.getRole() == TeamStatus.ADMIN);

        if (!isAdmin && !team.getUser().getId().equals(userId)) {
            throw new RuntimeException("Only team admins can remove members");
        }

        // Find the member to remove
        TeamMember memberToRemove = team.getMembers().stream()
                .filter(m -> m.getUser().getId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Member not found in team"));

        // Prevent removing the owner
        if (memberToRemove.getUser().getId().equals(team.getUser().getId())) {
            throw new RuntimeException("Owner cannot be removed from the team");
        }

        User removedUser = memberToRemove.getUser();
        String teamName = team.getName();
        String adminName = requester.getFirstName() + " " + requester.getLastName();

        team.getMembers().remove(memberToRemove);
        teamRepository.save(team);

        // Send email to the removed member
            log.info("Sending team member removed email to: {}", removedUser.getEmail());
            emailService.removeMemberMail(removedUser.getEmail(), teamName, adminName);
            log.info("Team member removed email sent successfully to: {}", removedUser.getEmail());

        log.info("User {} removed from team {} by {}", memberId, teamId, userId);

        return mapToResponse(team);
    }

    // to update team member status/role
    @Transactional
    @Caching(evict = {
            @CacheEvict(key = "'team:' + #teamId"),
            @CacheEvict(key = "'team:' + #teamId + '_members'")
    })
    public TeamResponse updateMemberRole(UUID teamId, UUID memberId, TeamStatus newRole, UUID userId) {
        User requester = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Requester not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));

        // Check if requester is ADMIN
        boolean isAdmin = team.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(requester.getId()) && m.getRole() == TeamStatus.ADMIN);

        if (!isAdmin && !team.getUser().getId().equals(userId)) {
            throw new RuntimeException("Only team admins can update member roles");
        }

        // Find the member to update
        TeamMember memberToUpdate = team.getMembers().stream()
                .filter(m -> m.getUser().getId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Member not found in team"));

        // Prevent changing the owner's role if they are the only admin or owner logic
        if (memberToUpdate.getUser().getId().equals(team.getUser().getId()) && newRole != TeamStatus.ADMIN) {
            throw new RuntimeException("Cannot demote the team owner");
        }

        memberToUpdate.setRole(newRole);
        teamRepository.save(team);

        // Send role update email

            log.info("Sending role update email to: {}", memberToUpdate.getUser().getEmail());
            emailService.roleUpdated(memberToUpdate.getUser().getEmail(), team.getName(), newRole);
            log.info("Role update email sent successfully to: {}", memberToUpdate.getUser().getEmail());

        log.info("User {} role updated to {} in team {} by {}", memberId, newRole, teamId, userId);

        return mapToResponse(team);
    }

    // Map entity to response
    private TeamResponse mapToResponse(Team team) {
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .ownerId(team.getUser().getId())
                .memberUsernames(team.getMembers().stream()
                        .map(m -> m.getUser().getUsername())
                        .collect(Collectors.toSet()))
                .createdAt(team.getCreatedAt())
                .build();
    }

    // to view all teams joined by thier user name and description and number of members avaliable as well as ur teamstatus
    @Cacheable(key = "'user:' + #userId + '_teams'")
    public List<TeamSummaryResponse> viewJoinedTeams(UUID userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        List<TeamMember> memberships = teamMemberRepository.findByUser(user);

        log.info("Fetching summary for {} teams joined by user {}", memberships.size(), userId);

        return memberships.stream()
                .map(membership -> {
                    Team team = membership.getTeam();
                    return TeamSummaryResponse.builder()
                            .teamId(team.getId())
                            .teamName(team.getName())
                            .description(team.getDescription())
                            .memberCount(team.getMembers().size())
                            .myRole(membership.getRole())
                            .build();
                })
                .collect(Collectors.toList());
    }


    @Cacheable(key = "'team:' + #teamId + '_membersWithRole'")
    public List<TeamMemberResponse> viewTeamMembersWithRole(UUID teamId, UUID userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found"));

        // Check if user is a member
        boolean isMember = team.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(userId));

        if (!isMember) {
            log.warn("User with id {} is not a member of team with id {}", userId, teamId);
            throw new RuntimeException("User is not a member of this team");
        }

        return team.getMembers().stream()
                .map(member -> TeamMemberResponse.builder()
                        .userId(member.getUser().getId())
                        .username(member.getUser().getUsername())
                        .firstName(member.getUser().getFirstName())
                        .lastName(member.getUser().getLastName())
                        .email(member.getUser().getEmail())
                        .role(member.getRole())
                        .build())
                .collect(Collectors.toList());
    }
}