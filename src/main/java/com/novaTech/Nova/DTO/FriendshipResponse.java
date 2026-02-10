package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.FriendshipStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendshipResponse {
    private Long id;
    private Long userId;
    private String userName;
    private FriendshipStatus status;
    private LocalDateTime createdAt;
    private Boolean isRequester;
}
