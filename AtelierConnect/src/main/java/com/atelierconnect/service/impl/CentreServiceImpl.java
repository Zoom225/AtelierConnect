package com.atelierconnect.service.impl;

import com.atelierconnect.entity.Centre;
import com.atelierconnect.exception.ResourceNotFoundException;
import com.atelierconnect.repository.CentreRepository;
import com.atelierconnect.service.CentreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CentreServiceImpl implements CentreService {

    private final CentreRepository siteRepository;

    @Override
    public Centre create(Centre site) {
        return siteRepository.save(site);
    }

    @Override
    public Centre getById(Long id) {
        return siteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Site introuvable avec l'ID : " + id));
    }

    @Override
    public List<Centre> getAll() {
        // C'est maintenant la bonne approche car les collections sont en LAZY
        // et ne seront pas chargées, évitant ainsi l'erreur 500.
        return siteRepository.findAll();
    }

    @Override
    public Centre update(Long id, Centre site) {
        Centre existing = getById(id);
        existing.setNom(site.getNom());
        existing.setAdresse(site.getAdresse());
        existing.setHeureOuverture(site.getHeureOuverture());
        existing.setHeureFermeture(site.getHeureFermeture());
        existing.setDureeMatchMinutes(site.getDureeMatchMinutes());
        existing.setDureeEntreMatchMinutes(site.getDureeEntreMatchMinutes());
        existing.setAnneeCivile(site.getAnneeCivile());
        existing.setOuvertVisiteurs(site.getOuvertVisiteurs());
        return siteRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        Centre existing = getById(id);
        siteRepository.delete(existing);
    }
}
