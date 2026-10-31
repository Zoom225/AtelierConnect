package com.padell.padell.service;

import com.padell.padell.entity.Participant;

import java.util.List;

public interface ParticipantService {
    Participant create(Participant membre);
    Participant create(Participant membre, Long siteId, String rawPassword);
    Participant prepareForCreation(Participant membre, Long siteId, String rawPassword);
    Participant getById(Long id);
    Participant getByMatricule(String matricule);
    List<Participant> getAll();
    Participant update(Long id, Participant membre);
    void delete(Long id);
    boolean hasActivePenalty(Long membreId);
    boolean hasOutstandingBalance(Long membreId);
    void addPenalty(Long membreId);
    Participant authenticate(String matricule, String password);
}
