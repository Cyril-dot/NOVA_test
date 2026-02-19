package com.novaTech.Nova.Entities.repo;

import com.novaTech.Nova.Entities.meeting.MeetingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link MeetingParticipant} — table: nova_meeting_participants
 *
 * JPQL queries reference the entity class name (MeetingParticipant), not the
 * physical table name, so renaming the table only requires the @Table annotation
 * change on the entity itself.
 */
@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, UUID> {

    // ── Basic lookups ─────────────────────────────────────────────────────────

    List<MeetingParticipant> findByMeetingId(UUID meetingId);

    Optional<MeetingParticipant> findBySessionId(String sessionId);

    Optional<MeetingParticipant> findByPeerId(String peerId);

    // ── Active / online participants ──────────────────────────────────────────

    /**
     * All participants who have NOT left yet (leftAt IS NULL).
     */
    @Query("""
           SELECT p FROM MeetingParticipant p
           WHERE p.meeting.id = :meetingId
             AND p.leftAt IS NULL
           """)
    List<MeetingParticipant> findActiveParticipants(@Param("meetingId") UUID meetingId);

    /**
     * A specific registered user's active participant record in a meeting.
     * Used to detect re-joins and to authorise moderation actions.
     */
    @Query("""
           SELECT p FROM MeetingParticipant p
           WHERE p.meeting.id = :meetingId
             AND p.user.id    = :userId
             AND p.leftAt IS NULL
           """)
    Optional<MeetingParticipant> findActiveUserParticipant(@Param("meetingId") UUID meetingId,
                                                            @Param("userId")    UUID userId);

    /**
     * All participants currently flagged as online (isOnline = true) for a
     * given meeting code.  Used by the presence system / dashboard.
     */
    @Query("""
           SELECT p FROM MeetingParticipant p
           WHERE p.meeting.meetingCode = :meetingCode
             AND p.isOnline = true
           """)
    List<MeetingParticipant> findOnlineParticipantsByMeetingCode(
            @Param("meetingCode") String meetingCode);

    // ── Daily.co helpers ──────────────────────────────────────────────────────

    /**
     * Find a participant by their Daily.co participant ID.
     * Useful if a Daily webhook sends a participant-left event containing
     * the Daily participant ID and you need to map it back to your DB record.
     */
    Optional<MeetingParticipant> findByPeerIdAndMeetingId(String peerId, UUID meetingId);

    /**
     * Count how many participants are still active in a meeting.
     * Lightweight alternative to loading the full collection.
     */
    @Query("""
           SELECT COUNT(p) FROM MeetingParticipant p
           WHERE p.meeting.id = :meetingId
             AND p.leftAt IS NULL
           """)
    Integer countActiveParticipants(@Param("meetingId") UUID meetingId);

    /**
     * All guest participants in a meeting who have not yet left.
     */
    @Query("""
           SELECT p FROM MeetingParticipant p
           WHERE p.meeting.id = :meetingId
             AND p.isGuest    = true
             AND p.leftAt IS NULL
           """)
    List<MeetingParticipant> findActiveGuests(@Param("meetingId") UUID meetingId);
}