package com.example.backend.repository;

import com.example.backend.entitie.Entreprise;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntrepriseRepository extends JpaRepository<Entreprise, Integer> {
	java.util.Optional<Entreprise> findByManager_Id(Long managerId);
}
