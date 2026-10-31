package com.atelierconnect.repository;

import com.atelierconnect.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    List<Reservation> findByMatchId(Long matchId);
    List<Reservation> findByMembreId(Long membreId);
    boolean existsByMatchIdAndMembreId(Long matchId, Long membreId);
}
