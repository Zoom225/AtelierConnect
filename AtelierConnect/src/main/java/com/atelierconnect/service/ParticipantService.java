package com.atelierconnect.service;

import com.atelierconnect.entity.Participant;

import java.util.List;

public interface ParticipantService {
    Participant create(Participant participant);
    Participant create(Participant participant, Long centreId, String rawPassword);
    Participant prepareForCreation(Participant participant, Long centreId, String rawPassword);
    Participant getById(Long id);
    Participant getByMatricule(String matricule);
    List<Participant> getAll();
    Participant update(Long id, Participant participant);
    void delete(Long id);
    boolean hasActivePenalty(Long participantId);
    boolean hasOutstandingBalance(Long participantId);
    void addPenalty(Long participantId);
    Participant authenticate(String matricule, String password);
}
