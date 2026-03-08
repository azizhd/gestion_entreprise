package com.example.backend.repository;

import com.example.backend.entitie.Utilisateur;
import com.example.backend.entitie.enumuration.TypeRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);
    List<Utilisateur> findByEntreprise_Id(Integer entrepriseId);
    long count();
    
    List<Utilisateur> findByEntreprise_IdAndRole(Integer entrepriseId, TypeRole role);
}