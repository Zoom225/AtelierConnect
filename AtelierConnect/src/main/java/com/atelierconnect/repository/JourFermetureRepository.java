package com.atelierconnect.repository;

import com.atelierconnect.entity.JourFermeture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JourFermetureRepository extends JpaRepository<JourFermeture, Long> {
    @Override
    @EntityGraph(attributePaths = "centre")
    List<JourFermeture> findAll();

    boolean existsByDateAndGlobalTrue(LocalDate date);

    boolean existsByDateAndCentreId(LocalDate date, Long centreId);

    @EntityGraph(attributePaths = "centre")
    List<JourFermeture> findByGlobalTrue();

    @EntityGraph(attributePaths = "centre")
    List<JourFermeture> findByCentreId(Long centreId);
}
