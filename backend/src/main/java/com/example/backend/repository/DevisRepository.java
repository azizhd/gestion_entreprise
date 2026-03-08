package com.example.backend.repository;

import com.example.backend.entitie.Devis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DevisRepository extends JpaRepository<Devis, Long> {

	Page<Devis> findByClient_Entreprise_Id(Integer entrepriseId, Pageable pageable);

	Optional<Devis> findByIdAndClient_Entreprise_Id(Long id, Integer entrepriseId);

	Optional<Devis> findTopByNumeroDevisStartingWithAndClient_Entreprise_IdOrderByNumeroDevisDesc(String prefix, Integer entrepriseId);
}
