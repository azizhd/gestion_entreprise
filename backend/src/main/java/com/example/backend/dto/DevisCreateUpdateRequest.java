package com.example.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DevisCreateUpdateRequest {

    @NotNull
    private Long clientId;

    private LocalDateTime devisDate;

    @PositiveOrZero
    private Double tvaRate;

    @NotEmpty
    @Valid
    private List<LigneDevisRequest> lignes;
}
