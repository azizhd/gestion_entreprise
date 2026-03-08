package com.example.backend.repository;

import com.example.backend.entitie.Facture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

public interface FactureRepository extends JpaRepository<Facture, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Facture> findTopByNumeroFactureStartingWithAndDevis_Client_Entreprise_IdOrderByNumeroFactureDesc(String prefix, Integer entrepriseId);

	Page<Facture> findByDevis_Client_Entreprise_Id(Integer entrepriseId, Pageable pageable);

	Optional<Facture> findByIdAndDevis_Client_Entreprise_Id(Long id, Integer entrepriseId);

    @Query("select f from Facture f where f.dueDate < :today and f.statututFacture not in (com.example.backend.entitie.enumuration.StatutFacture.PAYEE, com.example.backend.entitie.enumuration.StatutFacture.ANNULEE)")
    List<Facture> findOverdue(@Param("today") LocalDate today);

	@Query("select coalesce(sum(f.totalTtc),0) from Facture f where f.devis.client.entreprise.id = :entrepriseId")
	double sumTotalTtcByEntreprise(@Param("entrepriseId") Integer entrepriseId);

	@Query("select coalesce(sum(f.totalTtc),0) from Facture f where f.devis.client.entreprise.id = :entrepriseId and f.statututFacture = :status")
	double sumTotalTtcByEntrepriseAndStatus(@Param("entrepriseId") Integer entrepriseId, @Param("status") com.example.backend.entitie.enumuration.StatutFacture status);

	@Query("select coalesce(sum(f.totalTtc),0) from Facture f where f.devis.client.entreprise.id = :entrepriseId and (:start is null or f.date >= :start) and (:end is null or f.date <= :end)")
	double sumTotalTtcByEntrepriseAndDate(@Param("entrepriseId") Integer entrepriseId,
	                                     @Param("start") LocalDateTime start,
	                                     @Param("end") LocalDateTime end);

	@Query("select coalesce(sum(f.totalTtc),0) from Facture f where f.devis.client.entreprise.id = :entrepriseId and f.statututFacture = :status and (:start is null or f.date >= :start) and (:end is null or f.date <= :end)")
	double sumTotalTtcByEntrepriseStatusAndDate(@Param("entrepriseId") Integer entrepriseId,
	                                           @Param("status") com.example.backend.entitie.enumuration.StatutFacture status,
	                                           @Param("start") LocalDateTime start,
	                                           @Param("end") LocalDateTime end);

	@Query("select count(f) from Facture f where f.devis.client.entreprise.id = :entrepriseId and f.statututFacture = :status and (:start is null or f.date >= :start) and (:end is null or f.date <= :end)")
	long countByEntrepriseStatusAndDate(@Param("entrepriseId") Integer entrepriseId,
	                                    @Param("status") com.example.backend.entitie.enumuration.StatutFacture status,
	                                    @Param("start") LocalDateTime start,
	                                    @Param("end") LocalDateTime end);

	@Query("select f.devis.client.id, coalesce(sum(f.totalTtc),0) from Facture f where f.devis.client.entreprise.id = :entrepriseId group by f.devis.client.id")
	List<Object[]> revenueByClient(@Param("entrepriseId") Integer entrepriseId);

	@Query("select f.devis.client.nom, coalesce(sum(f.totalTtc),0) from Facture f where f.devis.client.entreprise.id = :entrepriseId and (:start is null or f.date >= :start) and (:end is null or f.date <= :end) group by f.devis.client.nom order by sum(f.totalTtc) desc")
	List<Object[]> revenueByClientAndDate(@Param("entrepriseId") Integer entrepriseId,
	                                     @Param("start") LocalDateTime start,
	                                     @Param("end") LocalDateTime end);

	@Query("select function('DATE_FORMAT', f.date, '%Y-%m') as ym, coalesce(sum(f.totalTtc),0) from Facture f where f.devis.client.entreprise.id = :entrepriseId group by function('DATE_FORMAT', f.date, '%Y-%m') order by ym asc")
	List<Object[]> monthlyTotals(@Param("entrepriseId") Integer entrepriseId);

	@Query("select function('DATE_FORMAT', f.date, '%Y-%m') as ym, coalesce(sum(f.totalTtc),0) from Facture f where f.devis.client.entreprise.id = :entrepriseId and (:start is null or f.date >= :start) and (:end is null or f.date <= :end) group by function('DATE_FORMAT', f.date, '%Y-%m') order by ym asc")
	List<Object[]> monthlyTotalsByDate(@Param("entrepriseId") Integer entrepriseId,
	                                  @Param("start") LocalDateTime start,
	                                  @Param("end") LocalDateTime end);

	@Query("select function('YEAR', f.date) as yr, coalesce(sum(f.totalTtc),0) from Facture f where f.devis.client.entreprise.id = :entrepriseId group by function('YEAR', f.date) order by yr asc")
	List<Object[]> yearlyTotals(@Param("entrepriseId") Integer entrepriseId);

	@Query("select f from Facture f where f.devis.client.entreprise.id = :entrepriseId and f.dueDate < :today and f.statututFacture not in (com.example.backend.entitie.enumuration.StatutFacture.PAYEE, com.example.backend.entitie.enumuration.StatutFacture.ANNULEE) and (:start is null or f.date >= :start) and (:end is null or f.date <= :end)")
	List<Facture> findOverdueByEntrepriseAndDate(@Param("entrepriseId") Integer entrepriseId,
	                                           @Param("today") LocalDate today,
	                                           @Param("start") LocalDateTime start,
	                                           @Param("end") LocalDateTime end);
}
