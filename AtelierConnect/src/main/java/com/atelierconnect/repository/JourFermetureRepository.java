package com.atelierconnect.repository;

import com.atelierconnect.entity.JourFermeture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface JourFermetureRepository extends JpaRepository<JourFermeture, Long> {

    boolean existsByDateAndGlobalTrue(LocalDate date);

    boolean existsByDateAndCentreId(LocalDate date, Long centreId);

    List<JourFermeture> findByGlobalTrue();

    List<JourFermeture> findByCentreId(Long centreId);
}
