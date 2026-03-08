package com.example.backend.entitie;

import com.example.backend.entitie.enumuration.TypeRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "utilisateur")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String prenom;
    private String email;
    private String password;

    @Enumerated(EnumType.STRING)
    private TypeRole role;

    private String photo;
    private String telephone;

    // ManyToOne side of OneToMany
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id") // column in utilisateur table
    private Entreprise entreprise;

        @ManyToMany(mappedBy = "utilisateurs")
        private List<Tache> taches;


    @OneToMany(mappedBy = "utilisateur")
    private List<Notification> notification;

    @OneToMany(mappedBy = "utilisateur")
    private List<AuditLog> auditLog ;

}
