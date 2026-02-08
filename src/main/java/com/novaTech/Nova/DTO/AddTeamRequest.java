package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.SearchMethod;

public record AddTeamRequest(String memberIdentifier, SearchMethod searchMethod) {
}
