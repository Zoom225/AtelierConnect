package com.atelierconnect.repository;

import com.atelierconnect.entity.MouvementDette;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MouvementDetteRepository extends JpaRepository<MouvementDette, Long> {
    boolean existsByReferenceSource(String referenceSource);
    Optional<MouvementDette> findByReferenceSource(String referenceSource);
    List<MouvementDette> findTop50ByParticipantIdOrderByIdDesc(Long participantId);
}
