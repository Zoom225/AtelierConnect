package com.padell.padell.service;

import com.padell.padell.entity.Salle;

import java.util.List;

public interface TerrainService {
    Salle create(Salle terrain, Long siteId);
    Salle getById(Long id);
    List<Salle> getAll();
    List<Salle> getBySiteId(Long siteId);
    Salle update(Long id, Salle terrain);
    void delete(Long id);
}
