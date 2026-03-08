package com.example.backend.repository;

import com.example.backend.entitie.RendezVous;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RendezVousRepository extends JpaRepository<RendezVous, Long> {
    Page<RendezVous> findByEntreprise_Id(Integer entrepriseId, Pageable pageable);
}
