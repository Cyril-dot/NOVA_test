package com.novaTech.Nova.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned by POST /api/meetings/daily-token
 * and          POST /api/meetings/daily-token/guest
 *
 * Frontend usage (Meeting.vue):
 *
 *   const res = await fetch('/api/meetings/daily-token', { ... })
 *   const { token, roomUrl } = res.data.data
 *
 *   await callFrame.join({
 *     url:   roomUrl,   // full Daily room URL
 *     token: token,     // signed JWT — may be null if token creation failed
 *   })
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTokenResponseDTO {

    /**
     * Signed Daily meeting token (JWT).
     * Pass to callFrame.join({ token }) so Daily knows the participant's
     * display name and whether they are the room owner.
     * May be null if the Daily API call failed — join still works, just
     * without owner permissions or a pre-set display name.
     */
    private String token;

    /** Full Daily room URL, e.g. https://noav.daily.co/abc-def-ghi */
    private String roomUrl;

    /** Daily room name only, e.g. abc-def-ghi */
    private String roomName;

    /** The meeting code from your DB, e.g. ABC-DEF-GHI */
    private String meetingCode;

    /** True if the token holder is the meeting host (Daily room owner). */
    private boolean isOwner;
}