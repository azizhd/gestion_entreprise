package com.example.backend.repository;

import com.example.backend.entitie.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    Page<Client> findByEntreprise_Id(Integer entrepriseId, Pageable pageable);
    Optional<Client> findByIdAndEntreprise_Id(Long id, Integer entrepriseId);
}
