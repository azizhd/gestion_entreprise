package com.example.backend.repository;

import com.example.backend.entitie.Fournisseur;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FournisseurRepository extends JpaRepository<Fournisseur, Long> {

    Optional<Fournisseur> findByIdAndEntreprise_Id(Long id, Integer entrepriseId);

    Page<Fournisseur> findByEntreprise_Id(Integer entrepriseId, Pageable pageable);

    @Query("select f from Fournisseur f where f.entreprise.id = :entrepriseId and (lower(f.nom) like lower(concat('%', :keyword, '%')) or lower(f.prenom) like lower(concat('%', :keyword, '%')) or lower(f.email) like lower(concat('%', :keyword, '%')))")
    Page<Fournisseur> search(@Param("keyword") String keyword, @Param("entrepriseId") Integer entrepriseId, Pageable pageable);
}
