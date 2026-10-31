package com.atelierconnect.repository;

import com.atelierconnect.entity.Reservation;
import com.atelierconnect.entity.enums.StatutReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
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
    @Override
    @EntityGraph(attributePaths = {"match", "membre", "paiement"})
    Optional<Reservation> findById(Long id);

    @EntityGraph(attributePaths = {"match", "membre", "paiement"})
    List<Reservation> findByMatchId(Long matchId);

    @EntityGraph(attributePaths = {"match", "membre", "paiement"})
    List<Reservation> findByMembreId(Long membreId);
    boolean existsByMatchIdAndMembreId(Long matchId, Long membreId);
    boolean existsByMatchIdAndMembreIdAndStatutNot(
            Long matchId, Long membreId, StatutReservation statut);
}
