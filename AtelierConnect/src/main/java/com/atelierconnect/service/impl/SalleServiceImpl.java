package com.atelierconnect.service.impl;

import com.atelierconnect.entity.Centre;
import com.atelierconnect.entity.Salle;
import com.atelierconnect.exception.ResourceNotFoundException;
import com.atelierconnect.repository.SalleRepository;
import com.atelierconnect.service.CentreService;
import com.atelierconnect.service.SalleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalleServiceImpl implements SalleService {

    private final SalleRepository salleRepository;
    private final CentreService centreService;

    @Override
    public Salle create(Salle salle, Long centreId) {
        Centre centre = centreService.getById(centreId);
        salle.setCentre(centre);
        log.info("Salle créé pour le centre {}", centreId);
        return salleRepository.save(salle);
    }

    @Override
    public Salle getById(Long id) {
        return salleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable avec l'ID : " + id));
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Salle getByIdForUpdate(Long id) {
        return salleRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable avec l'ID : " + id));
    }

    @Override
    public List<Salle> getAll() {
        return salleRepository.findAll();
    }

    @Override
    public List<Salle> getByCentreId(Long centreId) {
        centreService.getById(centreId);
        return salleRepository.findByCentreId(centreId);
    }

    @Override
    @Transactional
    public Salle update(Long id, Salle salle) {
        Salle existing = getById(id);
        existing.setNom(salle.getNom());
        return salleRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        Salle existing = getById(id);
        salleRepository.delete(existing);
    }
}
