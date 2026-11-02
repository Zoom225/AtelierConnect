package com.atelierconnect.repository;

import com.atelierconnect.entity.Salle;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalleRepository extends JpaRepository<Salle, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Salle s WHERE s.id = :id")
    Optional<Salle> findByIdForUpdate(@Param("id") Long id);
    // Les DTO utilisent centre.nom alors que spring.jpa.open-in-view=false.
    // Le graphe charge le centre dans la transaction de lecture JPA.
    @Override
    @EntityGraph(attributePaths = "centre")
    List<Salle> findAll();

    @Override
    @EntityGraph(attributePaths = "centre")
    Optional<Salle> findById(Long id);

    @EntityGraph(attributePaths = "centre")
    @Query("SELECT s FROM Salle s WHERE s.centre.id = :centreId")
    List<Salle> findByCentreId(@Param("centreId") Long centreId);
}
