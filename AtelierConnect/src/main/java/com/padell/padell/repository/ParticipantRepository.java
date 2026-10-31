package com.padell.padell.repository;

import com.padell.padell.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    @EntityGraph(attributePaths = "site")
    List<Participant> findAll();

    @EntityGraph(attributePaths = "site")
    Optional<Participant> findById(Long id);

    @EntityGraph(attributePaths = "site")
    Optional<Participant> findByMatricule(String matricule);

    @EntityGraph(attributePaths = "site")
    Optional<Participant> findByMatriculeIgnoreCase(String matricule);

    boolean existsByMatricule(String matricule);

    boolean existsByEmail(String email);
}
