package com.example.backend.entitie;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "client")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String adresse;
    private String telephone;
    private String email;
    private String nom;

    // Payment terms in days; used to derive invoice due dates
    private Integer paymentTermsDays;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    @OneToMany(mappedBy = "client")
    private List<Devis> devis ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_letter_id")
    private NewsLetter newsLetter;
}
