package com.novaTech.Nova.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the essential fields returned by the Daily.co
 * GET /rooms/{name} and POST /rooms endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyRoomResponseDTO {

    /** Daily room name, e.g. "abc-def-ghi" */
    private String name;

    /** Full Daily room URL, e.g. "https://noav.daily.co/abc-def-ghi" */
    private String url;
}