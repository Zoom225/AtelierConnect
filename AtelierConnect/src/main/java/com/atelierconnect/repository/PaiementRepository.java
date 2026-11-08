package com.atelierconnect.repository;

import com.atelierconnect.entity.Paiement;
import com.atelierconnect.entity.enums.StatutPaiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {
    @Query("SELECT p.id FROM Paiement p JOIN p.reservation r JOIN r.atelier a " +
            "WHERE p.statut = com.atelierconnect.entity.enums.StatutPaiement.EN_ATTENTE " +
            "AND a.dateDebut >= :start AND a.dateDebut < :end ORDER BY p.id")
    List<Long> findUnpaidIdsForDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    Optional<Paiement> findByReservationId(Long reservationId);
    List<Paiement> findByStatut(StatutPaiement statut);
    List<Paiement> findByReservationParticipantId(Long participantId);
}
