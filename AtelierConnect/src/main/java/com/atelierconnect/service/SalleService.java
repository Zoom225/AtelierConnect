package com.atelierconnect.service;

import com.atelierconnect.entity.Salle;

import java.util.List;

public interface SalleService {
    Salle create(Salle salle, Long centreId);
    Salle getById(Long id);
    List<Salle> getAll();
    List<Salle> getByCentreId(Long centreId);
    Salle update(Long id, Salle salle);
    void delete(Long id);
}
