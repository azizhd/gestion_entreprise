package com.example.backend.entitie;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "news_letter")
@AllArgsConstructor
@NoArgsConstructor
@lombok.Data
public class NewsLetter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
    
        private String titre;
        private String contenu;
        private LocalDate dateEnvoi;

    @OneToMany(mappedBy = "newsLetter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Client> clients; ;
    
}
