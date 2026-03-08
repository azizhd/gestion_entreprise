package com.example.backend.entitie;

import com.example.backend.entitie.enumuration.StatusDepense;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "depense")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Depense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;

    private Double montant;

    private LocalDate date;

    @Enumerated(EnumType.STRING)
    private StatusDepense statusDepense;

    private LocalDateTime approvalDate;

    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Utilisateur approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Utilisateur createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_id")
    private Fournisseur fournisseur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tache_id")
    @JsonIgnoreProperties({"depenses", "utilisateurs"})
    private Tache tache;
}
