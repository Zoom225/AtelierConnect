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
    private String defaultParticipantPassword;

    private final ParticipantRepository participantRepository;
    private final PenaliteRepository penaliteRepository;
    private final CentreService centreService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Participant create(Participant participant, Long centreId, String rawPassword) {
        prepareForCreation(participant, centreId, rawPassword);
        return create(participant);
    }

    @Override
    public Participant prepareForCreation(Participant participant, Long centreId, String rawPassword) {
        if (centreId != null) {
            Centre centre = centreService.getById(centreId);
            participant.setCentre(centre);
        }

        String password = StringUtils.hasText(rawPassword) ? rawPassword : defaultParticipantPassword;
        participant.setPasswordHash(passwordEncoder.encode(password));
        return participant;
    }

    @Override
    public Participant create(Participant participant) {
        participant.setMatricule(normalizeMatricule(participant.getMatricule()));
        validateMatricule(participant.getMatricule(), participant.getTypeParticipant());

        // Regle metier : le matricule doit etre unique.
        if (participantRepository.existsByMatricule(participant.getMatricule())) {
            throw new BusinessException("Le matricule existe déjà : " + participant.getMatricule());
        }
        // Regle metier : l'email doit etre unique lorsqu'il est renseigne.
        if (participant.getEmail() != null && participantRepository.existsByEmail(participant.getEmail())) {
            throw new BusinessException("L'email existe déjà : " + participant.getEmail());
        }
        // Regle metier : un participant SITE doit etre rattache a un centre.
        if (participant.getTypeParticipant() == TypeParticipant.ABONNE && participant.getCentre() == null) {
            throw new BusinessException("Un participant SITE doit être lié à un centre.");
        }

        participant.setSolde(0.0);
        return participantRepository.save(participant);
    }

    @Override
    public Participant getById(Long id) {
        return participantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Participant introuvable avec l'ID : " + id));
    }

    @Override
    public Participant getByMatricule(String matricule) {
        String normalizedMatricule = normalizeMatricule(matricule);
        return participantRepository.findByMatriculeIgnoreCase(normalizedMatricule)
                .orElseThrow(() -> new ResourceNotFoundException("Participant introuvable avec le matricule : " + normalizedMatricule));
    }

    @Override
    public List<Participant> getAll() {
        return participantRepository.findAll();
    }

    @Override
    public Participant update(Long id, Participant participant) {
        Participant existing = getById(id);
        existing.setNom(participant.getNom());
        existing.setPrenom(participant.getPrenom());
        existing.setEmail(participant.getEmail());
        return participantRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        Participant existing = getById(id);
        participantRepository.delete(existing);
    }

    @Override
    public boolean hasActivePenalty(Long participantId) {
        return penaliteRepository.existsByParticipantIdAndDateFinAfter(participantId, LocalDate.now());
    }

    @Override
    public boolean hasOutstandingBalance(Long participantId) {
        Participant participant = getById(participantId);
        return participant.getSolde() > 0.0;
    }
    @Override
    public void addPenalty(Long participantId) {
        Participant participant = getById(participantId);
        Penalite penalite = Penalite.builder()
                .participant(participant)
                .dateFin(LocalDate.now().plusWeeks(1))
                .motif("Atelier privé incomplet avant l'échéance")
                .build();
        penaliteRepository.save(penalite);
    }

    @Override
    public Participant authenticate(String matricule, String password) {
        String normalizedMatricule = normalizeMatricule(matricule);
        Participant participant = participantRepository.findByMatriculeIgnoreCase(normalizedMatricule)
                .orElseThrow(() -> new BusinessException("Identifiants invalides."));

        if (!StringUtils.hasText(password)
                || !StringUtils.hasText(participant.getPasswordHash())
                || !passwordEncoder.matches(password, participant.getPasswordHash())) {
            throw new BusinessException("Identifiants invalides.");
        }

        return participant;
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
        // Regle metier : un matricule doit respecter le format du type de participant.
        if (!valid) {
            throw new BusinessException("Format de matricule invalide pour le type " + type + " : " + matricule);
        }
    }
}
