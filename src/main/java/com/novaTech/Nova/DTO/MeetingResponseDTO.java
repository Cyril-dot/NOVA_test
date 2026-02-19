package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.MeetingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO returned by all meeting endpoints.
 * Includes the two new Daily.co fields so the frontend can use
 * them directly without having to recompute the room URL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingResponseDTO {

    private UUID            id;
    private String          meetingCode;
    private String          title;
    private String          description;
    private String          hostName;
    private UUID            hostId;

    private LocalDateTime   scheduledStartTime;
    private LocalDateTime   actualStartTime;
    private LocalDateTime   endTime;
    private LocalDateTime   createdAt;

    private MeetingStatus   status;
    private Integer         maxParticipants;
    private Integer         currentParticipants;

    private Boolean         isPublic;
    private Boolean         requiresPassword;
    private Boolean         allowGuests;
    private Boolean         videoEnabled;
    private Boolean         audioEnabled;
    private Boolean         screenShareEnabled;
    private Boolean         chatEnabled;

    // ── Daily.co ─────────────────────────────────────────────────────────────

    /**
     * Full Daily.co room URL, e.g. https://noav.daily.co/abc-def-ghi
     * Frontend passes this to callFrame.join({ url: dailyRoomUrl, token }).
     */
    private String dailyRoomUrl;

    /**
     * Daily.co room name (slug), e.g. abc-def-ghi
     * Useful if the frontend needs to construct its own token request URL.
     */
    private String dailyRoomName;

    // ── Participants ──────────────────────────────────────────────────────────

    private List<ParticipantDTO> participants;
}