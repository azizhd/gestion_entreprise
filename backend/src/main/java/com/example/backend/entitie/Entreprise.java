package com.example.backend.entitie;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Set;
import java.time.LocalDateTime;

@Entity
@Table(name = "entreprise")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Entreprise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String nom;
    private String logo;
    private String email;
    private String telephone;
    private String location;

    private Boolean deleted = Boolean.FALSE;
    private LocalDateTime deletionRequestedAt;
    private LocalDateTime purgeAt;

    // OneToOne abonnement  
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "abonnement_id")
    private Abonnement abonnement;

    // OneToOne manager (special user)
    @OneToOne
    @JoinColumn(name = "manager_id")
    private Utilisateur manager;

    // OneToMany employees
    @OneToMany(mappedBy = "entreprise", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Utilisateur> utilisateurs;

    @OneToMany(mappedBy = "entreprise", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Fournisseur> fournisseurs;
}
