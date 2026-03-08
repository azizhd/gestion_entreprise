package com.example.backend.entitie;

import com.example.backend.entitie.enumuration.RendezVousStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "rendez_vous")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RendezVous {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titre;

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    private String location;

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    private RendezVousStatus status;

    @Column(length = 500)
    private String statusNote;

    private String contactName;
    private String contactEmail;
    private String contactPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Utilisateur createdBy;

    @PrePersist
    public void onCreate() {
        if (status == null) {
            status = RendezVousStatus.PENDING;
        }
    }
}
