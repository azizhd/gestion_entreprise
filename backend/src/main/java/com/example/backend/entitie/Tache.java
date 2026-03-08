package com.example.backend.entitie;

import com.example.backend.entitie.enumuration.StatusTache;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tache")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Tache {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    private StatusTache status;

    private Integer progress;

    private Double budget;

    private Double totalApprovedExpenses;

    private Double totalPendingExpenses;

    private Double totalDeclinedExpenses;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    @OneToMany(mappedBy = "tache", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("tache")
    private List<Depense> depenses = new ArrayList<>();

        @ManyToMany
        @JoinTable(
            name = "utilisateur_tache",
            joinColumns = @JoinColumn(name = "tache_id"),
            inverseJoinColumns = @JoinColumn(name = "utilisateur_id")
        )
        @JsonIgnoreProperties({"taches", "entreprise"})
        private List<Utilisateur> utilisateurs = new ArrayList<>();
}
