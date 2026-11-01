package com.atelierconnect.repository;

import com.atelierconnect.entity.Reservation;
import com.atelierconnect.entity.enums.StatutReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    long countByAtelierIdAndStatutNot(Long atelierId, StatutReservation statut);
    /**
     * Verrouille la ligne avant la lecture de l'état du paiement.
     * Une requête native sur l'ID évite de verrouiller les jointures
     * externes générées par les relations JPA (PostgreSQL FOR UPDATE).
     * À appeler uniquement dans une transaction active.
     */
    @Query(value = "SELECT id FROM reservations WHERE id = :reservationId FOR UPDATE", nativeQuery = true)
    Long lockReservationById(@Param("reservationId") Long reservationId);

    // Le mapper construit des DTO après la transaction repository :
    // éviter LazyInitializationException avec open-in-view=false.
    @EntityGraph(attributePaths = {"atelier", "participant", "paiement"})
    List<Reservation> findByAtelierId(Long atelierId);

    @EntityGraph(attributePaths = {"atelier", "participant", "paiement"})
    List<Reservation> findByParticipantId(Long participantId);
    boolean existsByAtelierIdAndParticipantId(Long atelierId, Long participantId);
    boolean existsByAtelierIdAndParticipantIdAndStatutNot(
            Long atelierId, Long participantId, StatutReservation statut);
}
