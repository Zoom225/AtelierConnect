package com.atelierconnect.repository;

import com.atelierconnect.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    @EntityGraph(attributePaths = "centre")
    List<Participant> findAll();

    @EntityGraph(attributePaths = "centre")
    Optional<Participant> findById(Long id);

    @EntityGraph(attributePaths = "centre")
    Optional<Participant> findByMatricule(String matricule);

    @EntityGraph(attributePaths = "centre")
    Optional<Participant> findByMatriculeIgnoreCase(String matricule);

    boolean existsByMatricule(String matricule);

    boolean existsByEmail(String email);
}
