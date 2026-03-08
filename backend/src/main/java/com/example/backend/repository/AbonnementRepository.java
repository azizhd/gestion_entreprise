package com.example.backend.repository;

import com.example.backend.entitie.Abonnement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AbonnementRepository extends JpaRepository<Abonnement, Long> {
}
