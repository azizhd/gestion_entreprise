package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsLetterDto {
    private Long id;
    private String titre;
    private String contenu;
    private LocalDate dateEnvoi;
}
