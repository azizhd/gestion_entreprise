package com.example.backend.repository;

import com.example.backend.entitie.Paiement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    List<Paiement> findByFacture_IdAndEntreprise_Id(Long factureId, Integer entrepriseId);

    List<Paiement> findByFournisseur_IdAndEntreprise_Id(Long fournisseurId, Integer entrepriseId);

    @Query("select coalesce(sum(p.montant),0) from Paiement p where p.fournisseur.id = :fournisseurId and p.entreprise.id = :entrepriseId")
    double sumByFournisseur(@Param("fournisseurId") Long fournisseurId, @Param("entrepriseId") Integer entrepriseId);

    @Query("select coalesce(sum(p.montant),0) from Paiement p where p.facture.id = :factureId and p.entreprise.id = :entrepriseId")
    double sumByFacture(@Param("factureId") Long factureId, @Param("entrepriseId") Integer entrepriseId);

    Optional<Paiement> findByIdAndEntreprise_Id(Long id, Integer entrepriseId);
}
