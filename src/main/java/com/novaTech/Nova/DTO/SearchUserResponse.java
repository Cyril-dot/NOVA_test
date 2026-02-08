package com.novaTech.Nova.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchUserResponse {

    private String userName;
    private String userFirstName;
    private String userLastName;
    private String userEmail;
    private String userProfilePic;

}
