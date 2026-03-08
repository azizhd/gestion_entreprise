package com.example.backend.repository;

import com.example.backend.entitie.Tache;
import com.example.backend.entitie.enumuration.StatusTache;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.time.LocalDate;

public interface TacheRepository extends JpaRepository<Tache, Long> {
    Page<Tache> findByEntreprise_Id(Integer entrepriseId, Pageable pageable);

    List<Tache> findByEntreprise_Id(Integer entrepriseId);

    Page<Tache> findByUtilisateurs_IdAndEntreprise_Id(Long utilisateurId, Integer entrepriseId, Pageable pageable);

    boolean existsByIdAndEntreprise_Id(Long id, Integer entrepriseId);

    @Query("select count(t) from Tache t join t.utilisateurs u where u.id = :userId and t.entreprise.id = :entrepriseId")
    long countByAssignee(@Param("userId") Long userId, @Param("entrepriseId") Integer entrepriseId);

    @Query("select count(t) from Tache t join t.utilisateurs u where u.id = :userId and t.entreprise.id = :entrepriseId and t.status = :status")
    long countByAssigneeAndStatus(@Param("userId") Long userId, @Param("entrepriseId") Integer entrepriseId, @Param("status") com.example.backend.entitie.enumuration.StatusTache status);

    @Query("select count(t) from Tache t where t.entreprise.id = :entrepriseId and t.status = com.example.backend.entitie.enumuration.StatusTache.EN_RETARD")
    long countOverdue(@Param("entrepriseId") Integer entrepriseId);

    @Query("select count(t) from Tache t where t.entreprise.id = :entrepriseId and t.status = :status")
    long countByEntrepriseAndStatus(@Param("entrepriseId") Integer entrepriseId, @Param("status") com.example.backend.entitie.enumuration.StatusTache status);

    List<Tache> findTop10ByEntreprise_IdAndStatusOrderByDateFinDesc(Integer entrepriseId, com.example.backend.entitie.enumuration.StatusTache status);

    List<Tache> findByEntreprise_IdAndDateFinBetween(Integer entrepriseId, LocalDate start, LocalDate end);

    @Query("select t from Tache t where t.dateFin < :today and t.status not in (com.example.backend.entitie.enumuration.StatusTache.TERMINE, com.example.backend.entitie.enumuration.StatusTache.EN_RETARD)")
    List<Tache> findOverdueCandidates(@Param("today") LocalDate today);
}
