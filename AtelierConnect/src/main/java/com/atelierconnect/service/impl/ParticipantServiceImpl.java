package com.atelierconnect.service.impl;

import com.atelierconnect.entity.Participant;
import com.atelierconnect.entity.Penalite;
import com.atelierconnect.entity.Centre;
import com.atelierconnect.entity.enums.TypeParticipant;
import com.atelierconnect.exception.BusinessException;
import com.atelierconnect.exception.ResourceNotFoundException;
import com.atelierconnect.repository.ParticipantRepository;
import com.atelierconnect.repository.PenaliteRepository;
import com.atelierconnect.service.ParticipantService;
import com.atelierconnect.service.CentreService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ParticipantServiceImpl implements ParticipantService {

    @Value("${APP_DEMO_MEMBER_PASSWORD}")
    private String defaultMemberPassword;

    private final ParticipantRepository membreRepository;
    private final PenaliteRepository penaliteRepository;
    private final CentreService siteService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Participant create(Participant membre, Long siteId, String rawPassword) {
        prepareForCreation(membre, siteId, rawPassword);
        return create(membre);
    }

    @Override
    public Participant prepareForCreation(Participant membre, Long siteId, String rawPassword) {
        if (siteId != null) {
            Centre site = siteService.getById(siteId);
            membre.setSite(site);
        }

        String password = StringUtils.hasText(rawPassword) ? rawPassword : defaultMemberPassword;
        membre.setPasswordHash(passwordEncoder.encode(password));
        return membre;
    }

    @Override
    public Participant create(Participant membre) {
        membre.setMatricule(normalizeMatricule(membre.getMatricule()));
        validateMatricule(membre.getMatricule(), membre.getTypeMembre());

        // Regle metier : le matricule doit etre unique.
        if (membreRepository.existsByMatricule(membre.getMatricule())) {
            throw new BusinessException("Le matricule existe déjà : " + membre.getMatricule());
        }
        // Regle metier : l'email doit etre unique lorsqu'il est renseigne.
        if (membre.getEmail() != null && membreRepository.existsByEmail(membre.getEmail())) {
            throw new BusinessException("L'email existe déjà : " + membre.getEmail());
        }
        // Regle metier : un membre SITE doit etre rattache a un site.
        if (membre.getTypeMembre() == TypeParticipant.SITE && membre.getSite() == null) {
            throw new BusinessException("Un membre SITE doit être lié à un site.");
        }

        membre.setSolde(0.0);
        return membreRepository.save(membre);
    }

    @Override
    public Participant getById(Long id) {
        return membreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Membre introuvable avec l'ID : " + id));
    }

    @Override
    public Participant getByMatricule(String matricule) {
        String normalizedMatricule = normalizeMatricule(matricule);
        return membreRepository.findByMatriculeIgnoreCase(normalizedMatricule)
                .orElseThrow(() -> new ResourceNotFoundException("Membre introuvable avec le matricule : " + normalizedMatricule));
    }

    @Override
    public List<Participant> getAll() {
        return membreRepository.findAll();
    }

    @Override
    public Participant update(Long id, Participant membre) {
        Participant existing = getById(id);
        existing.setNom(membre.getNom());
        existing.setPrenom(membre.getPrenom());
        existing.setEmail(membre.getEmail());
        return membreRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        Participant existing = getById(id);
        membreRepository.delete(existing);
    }

    @Override
    public boolean hasActivePenalty(Long membreId) {
        return penaliteRepository.existsByMembreIdAndDateFinAfter(membreId, LocalDate.now());
    }

    @Override
    public boolean hasOutstandingBalance(Long membreId) {
        Participant membre = getById(membreId);
        return membre.getSolde() > 0.0;
    }
    @Override
    public void addPenalty(Long membreId) {
        Participant membre = getById(membreId);
        Penalite penalite = Penalite.builder()
                .membre(membre)
                .dateFin(LocalDate.now().plusWeeks(1))
                .motif("Match privé incomplet avant l'échéance")
                .build();
        penaliteRepository.save(penalite);
    }

    @Override
    public Participant authenticate(String matricule, String password) {
        String normalizedMatricule = normalizeMatricule(matricule);
        Participant membre = membreRepository.findByMatriculeIgnoreCase(normalizedMatricule)
                .orElseThrow(() -> new BusinessException("Identifiants invalides."));

        if (!StringUtils.hasText(password)
                || !StringUtils.hasText(membre.getPasswordHash())
                || !passwordEncoder.matches(password, membre.getPasswordHash())) {
            throw new BusinessException("Identifiants invalides.");
        }

        return membre;
    }

    private String normalizeMatricule(String matricule) {
        return matricule == null ? "" : matricule.trim().toUpperCase(Locale.ROOT);
    }

    private void validateMatricule(String matricule, TypeParticipant type) {
        boolean valid = switch (type) {
            case PARTENAIRE -> matricule.matches("^G\\d{4}$");
            case ABONNE -> matricule.matches("^S\\d{5}$");
            case VISITEUR -> matricule.matches("^L\\d{5}$");
        };
        // Regle metier : un matricule doit respecter le format du type de membre.
        if (!valid) {
            throw new BusinessException("Format de matricule invalide pour le type " + type + " : " + matricule);
        }
    }
}
