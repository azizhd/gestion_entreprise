package com.example.backend.repository;

import com.example.backend.entitie.Depense;
import com.example.backend.entitie.enumuration.StatusDepense;
import java.util.List;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepenseRepository extends JpaRepository<Depense, Long> {

    Page<Depense> findByTache_IdAndTache_Entreprise_Id(Long tacheId, Integer entrepriseId, Pageable pageable);

    Page<Depense> findByTache_IdAndTache_Entreprise_IdAndTache_Utilisateurs_Id(Long tacheId, Integer entrepriseId, Long utilisateurId, Pageable pageable);

    boolean existsByTache_IdAndStatusDepense(Long tacheId, StatusDepense status);

    @Query("select coalesce(sum(d.montant), 0) from Depense d where d.tache.id = :taskId and d.tache.entreprise.id = :entrepriseId and d.statusDepense = :status")
    double sumByTaskAndStatus(@Param("taskId") Long taskId,
                              @Param("entrepriseId") Integer entrepriseId,
                              @Param("status") StatusDepense status);

    @Query("select count(d) from Depense d where d.tache.id = :taskId and d.statusDepense = :status")
    long countByTaskAndStatus(@Param("taskId") Long taskId, @Param("status") StatusDepense status);

    @Query("select coalesce(sum(d.montant),0) from Depense d where d.fournisseur.id = :fournisseurId and d.fournisseur.entreprise.id = :entrepriseId and d.statusDepense = 'APPROVED'")
    double sumApprovedByFournisseur(@Param("fournisseurId") Long fournisseurId, @Param("entrepriseId") Integer entrepriseId);

    @Query("select d from Depense d where d.fournisseur.id = :fournisseurId and d.fournisseur.entreprise.id = :entrepriseId and d.statusDepense in ('APPROVED','PENDING') order by d.date desc")
    List<Depense> findUnpaidByFournisseur(@Param("fournisseurId") Long fournisseurId, @Param("entrepriseId") Integer entrepriseId);

    List<Depense> findByFournisseur_IdAndFournisseur_Entreprise_IdOrderByDateAsc(Long fournisseurId, Integer entrepriseId);

    @Query("select d.fournisseur.id, coalesce(sum(d.montant),0) from Depense d where d.fournisseur.entreprise.id = :entrepriseId and d.statusDepense = 'APPROVED' group by d.fournisseur.id order by sum(d.montant) desc")
    List<Object[]> topFournisseursByDepense(@Param("entrepriseId") Integer entrepriseId);

    @Query("select function('DATE_FORMAT', d.date, '%Y-%m') as ym, coalesce(sum(d.montant),0) from Depense d where d.fournisseur.id = :fournisseurId and d.fournisseur.entreprise.id = :entrepriseId and d.statusDepense = 'APPROVED' group by function('DATE_FORMAT', d.date, '%Y-%m') order by ym asc")
    List<Object[]> monthlyTotalsByFournisseur(@Param("fournisseurId") Long fournisseurId, @Param("entrepriseId") Integer entrepriseId);

    @Query("select coalesce(sum(d.montant),0) from Depense d where d.tache.entreprise.id = :entrepriseId")
    double sumByEntreprise(@Param("entrepriseId") Integer entrepriseId);

    @Query("select coalesce(sum(d.montant),0) from Depense d where d.tache.entreprise.id = :entrepriseId and d.statusDepense = :status")
    double sumByEntrepriseAndStatus(@Param("entrepriseId") Integer entrepriseId, @Param("status") StatusDepense status);

    @Query("select coalesce(sum(d.montant),0) from Depense d where d.tache.entreprise.id = :entrepriseId and (:start is null or d.date >= :start) and (:end is null or d.date <= :end)")
    double sumByEntrepriseAndDate(@Param("entrepriseId") Integer entrepriseId,
                                  @Param("start") LocalDate start,
                                  @Param("end") LocalDate end);

    @Query("select coalesce(sum(d.montant),0) from Depense d where d.tache.entreprise.id = :entrepriseId and d.statusDepense = :status and (:start is null or d.date >= :start) and (:end is null or d.date <= :end)")
    double sumByEntrepriseStatusAndDate(@Param("entrepriseId") Integer entrepriseId,
                                        @Param("status") StatusDepense status,
                                        @Param("start") LocalDate start,
                                        @Param("end") LocalDate end);

    @Query("select function('DATE_FORMAT', d.date, '%Y-%m') as ym, coalesce(sum(d.montant),0) from Depense d where d.tache.entreprise.id = :entrepriseId group by function('DATE_FORMAT', d.date, '%Y-%m') order by ym asc")
    List<Object[]> monthlyTotalsByEntreprise(@Param("entrepriseId") Integer entrepriseId);

    @Query("select function('DATE_FORMAT', d.date, '%Y-%m') as ym, coalesce(sum(d.montant),0) from Depense d where d.tache.entreprise.id = :entrepriseId and (:start is null or d.date >= :start) and (:end is null or d.date <= :end) group by function('DATE_FORMAT', d.date, '%Y-%m') order by ym asc")
    List<Object[]> monthlyTotalsByEntrepriseAndDate(@Param("entrepriseId") Integer entrepriseId,
                                                   @Param("start") LocalDate start,
                                                   @Param("end") LocalDate end);
}
