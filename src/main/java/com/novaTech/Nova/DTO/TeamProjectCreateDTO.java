package com.novaTech.Nova.DTO;

import com.novaTech.Nova.Entities.Enums.ProjectStatus;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public record TeamProjectCreateDTO(String title,
                                   String description,
                                   LocalDate startDate,
                                   LocalDate endDate,
                                   ProjectStatus status,

                                   List<MultipartFile> documents,
                                   String documentDescription) {
}
