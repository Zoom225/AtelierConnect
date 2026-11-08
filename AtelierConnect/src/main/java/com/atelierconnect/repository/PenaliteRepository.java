package com.atelierconnect.repository;


import com.atelierconnect.entity.Penalite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface PenaliteRepository extends JpaRepository<Penalite, Long> {
    boolean existsByParticipantId(Long participantId);
    // La suspension reste active pendant toute sa date de fin.
    boolean existsByParticipantIdAndDateFinGreaterThanEqual(Long participantId, LocalDate date);

    boolean existsByParticipantIdAndMotif(Long participantId, String motif);
}
