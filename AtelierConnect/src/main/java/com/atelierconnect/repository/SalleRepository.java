package com.atelierconnect.repository;

import com.atelierconnect.entity.Salle;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalleRepository extends JpaRepository<Salle, Long> {
    // Les DTO utilisent centre.nom alors que spring.jpa.open-in-view=false.
    // Le graphe charge le centre dans la transaction de lecture JPA.
    @Override
    @EntityGraph(attributePaths = "site")
    List<Salle> findAll();

    @Override
    @EntityGraph(attributePaths = "site")
    Optional<Salle> findById(Long id);

    @EntityGraph(attributePaths = "site")
    @Query("SELECT s FROM Salle s WHERE s.site.id = :centreId")
    List<Salle> findByCentreId(@Param("centreId") Long centreId);
}
