package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FournisseurLedgerItemDto {
    public enum EntryType { DEPENSE, PAIEMENT }

    private LocalDate date;
    private EntryType type;
    private Double amount;
    private Double runningBalance;
    private String reference;
    private String description;
}
