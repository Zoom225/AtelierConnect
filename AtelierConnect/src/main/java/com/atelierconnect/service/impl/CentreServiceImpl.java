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

    private final CentreRepository centreRepository;

    @Override
    public Centre create(Centre centre) {
        return centreRepository.save(centre);
    }

    @Override
    public Centre getById(Long id) {
        return centreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Centre introuvable avec l'ID : " + id));
    }

    @Override
    public List<Centre> getAll() {
        // C'est maintenant la bonne approche car les collections sont en LAZY
        // et ne seront pas chargées, évitant ainsi l'erreur 500.
        return centreRepository.findAll();
    }

    @Override
    public Centre update(Long id, Centre centre) {
        Centre existing = getById(id);
        existing.setNom(centre.getNom());
        existing.setAdresse(centre.getAdresse());
        existing.setHeureOuverture(centre.getHeureOuverture());
        existing.setHeureFermeture(centre.getHeureFermeture());
        existing.setDureeAtelierMinutes(centre.getDureeAtelierMinutes());
        existing.setDureeEntreAtelierMinutes(centre.getDureeEntreAtelierMinutes());
        existing.setAnneeCivile(centre.getAnneeCivile());
        existing.setOuvertVisiteurs(centre.getOuvertVisiteurs());
        return centreRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        Centre existing = getById(id);
        centreRepository.delete(existing);
    }
}
