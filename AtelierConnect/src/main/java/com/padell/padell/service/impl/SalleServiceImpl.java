package com.padell.padell.service.impl;

import com.padell.padell.entity.Centre;
import com.padell.padell.entity.Salle;
import com.padell.padell.exception.ResourceNotFoundException;
import com.padell.padell.repository.SalleRepository;
import com.padell.padell.service.CentreService;
import com.padell.padell.service.SalleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalleServiceImpl implements SalleService {

    private final SalleRepository terrainRepository;
    private final CentreService siteService;

    @Override
    public Salle create(Salle terrain, Long siteId) {
        Centre site = siteService.getById(siteId);
        terrain.setSite(site);
        log.info("Terrain créé pour le site {}", siteId);
        return terrainRepository.save(terrain);
    }

    @Override
    public Salle getById(Long id) {
        return terrainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Terrain introuvable avec l'ID : " + id));
    }

    @Override
    public List<Salle> getAll() {
        return terrainRepository.findAll();
    }

    @Override
    public List<Salle> getBySiteId(Long siteId) {
        siteService.getById(siteId);
        return terrainRepository.findByCentreId(siteId);
    }

    @Override
    public Salle update(Long id, Salle terrain) {
        Salle existing = getById(id);
        existing.setNom(terrain.getNom());
        return terrainRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        Salle existing = getById(id);
        terrainRepository.delete(existing);
    }
}
