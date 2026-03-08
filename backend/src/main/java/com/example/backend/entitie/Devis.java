package com.example.backend.entitie;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

import com.example.backend.entitie.enumuration.StatutDevis;

@Entity
@Table(name = "devis")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Devis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reference;
    private String numeroDevis;
    private LocalDateTime devisDate;

    private Double totalHt;
    private Double totalTva;
    private Double totalTtc;
    private Double tvaRate;
    private Double montant;

    @Enumerated(EnumType.STRING)
    private StatutDevis statut;

    private Boolean transformeEnFacture = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @OneToMany(mappedBy = "devis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneDevis> lignesdevis;

    @OneToOne
    @JoinColumn(name = "facture_id")
    private Facture facture;

}
