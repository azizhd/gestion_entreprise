package com.example.backend.entitie;

import com.example.backend.entitie.enumuration.AbonnementType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "abonnement")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Abonnement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private AbonnementType abonnementType;

    @OneToOne(mappedBy = "abonnement")
    private  Entreprise entreprise;

    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private Boolean status;
}
