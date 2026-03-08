package com.example.backend.dto;

import com.example.backend.entitie.enumuration.StatusTache;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TacheDto {
    private Long id;
    private String description;
    private LocalDateTime datedebut;
    private LocalDateTime datefin;
    private StatusTache status;
    private List<Long> utilisateurIds;
    private List<Long> depenseIds;
}
