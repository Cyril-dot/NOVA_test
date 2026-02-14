package com.novaTech.Nova.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinMeetingDTO {
    private String meetingCode;
    private String password; // If meeting requires password
    
    // For guests
    private String guestName;
    private String guestEmail;
}