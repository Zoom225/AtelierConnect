package com.atelierconnect.service;

import com.atelierconnect.entity.Reservation;

import java.util.List;

public interface ReservationService {
    Reservation create(Long atelierId, Long participantId, Long requesterId);
    List<Reservation> createForInvites(Long atelierId, Long requesterId, List<String> inviteeMatricules);
    Reservation getById(Long id);
    List<Reservation> getByAtelierId(Long atelierId);
    List<Reservation> getByParticipantId(Long participantId);
    void checkReservationOwner(Long reservationId, Long participantId);
    void cancel(Long reservationId);
    void confirm(Long reservationId);
}
