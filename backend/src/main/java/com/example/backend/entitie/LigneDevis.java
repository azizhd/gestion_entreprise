package com.example.backend.entitie;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ligne_devis")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LigneDevis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "devis_id")
    private Devis devis;

    private String description;
    private Integer quantite;
    private Double prixUnitaire;
    private Double total;
}

