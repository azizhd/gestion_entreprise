package com.example.backend.repository;

import com.example.backend.entitie.Document;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, Long> {

        @EntityGraph(attributePaths = {"uploadedBy"})
        @Query("select d from Document d where d.entreprise.id = :entrepriseId "
            + "and (:uploadedById is null or d.uploadedBy.id = :uploadedById) "
            + "and (:type is null or d.type = :type) "
            + "and (:category is null or d.category = :category) "
            + "and (:fromDate is null or d.uploadDate >= :fromDate) "
            + "and (:toDate is null or d.uploadDate <= :toDate) "
            + "and (:search is null or lower(d.title) like lower(concat('%', :search, '%')) "
            + "or lower(d.description) like lower(concat('%', :search, '%')))" )
    Page<Document> search(@Param("entrepriseId") Integer entrepriseId,
                          @Param("uploadedById") Long uploadedById,
                          @Param("type") String type,
                          @Param("category") String category,
                          @Param("fromDate") LocalDateTime fromDate,
                          @Param("toDate") LocalDateTime toDate,
                          @Param("search") String search,
                          Pageable pageable);
}
