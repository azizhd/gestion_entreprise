package com.example.backend.entitie;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.example.backend.entitie.enumuration.StatutFacture;

@Entity
@Table(name = "facture")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Facture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numeroFacture;
    private LocalDate dueDate;
    private Double totalHt;
    private Double totalTva;
    private Double totalTtc;
    private Double tvaRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "statutut_facture")
    private StatutFacture statututFacture;


    private String reference;
    private LocalDateTime date;
    private Double montant;
    private Boolean payee;

    @OneToOne
    private Devis devis;

    @OneToMany(mappedBy = "facture")
    private List<Paiement> paiements;
}
