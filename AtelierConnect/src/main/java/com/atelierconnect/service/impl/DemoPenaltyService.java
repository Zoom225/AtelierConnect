package com.atelierconnect.service.impl;

import com.atelierconnect.entity.MouvementDette;
import com.atelierconnect.entity.Participant;
import com.atelierconnect.entity.Penalite;
import com.atelierconnect.entity.enums.TypeParticipant;
import com.atelierconnect.repository.MouvementDetteRepository;
import com.atelierconnect.repository.ParticipantRepository;
import com.atelierconnect.repository.PenaliteRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Prépare uniquement le scénario du compte de démonstration après authentification. */
@Service
@RequiredArgsConstructor
public class DemoPenaltyService {
    private static final String MOTIF = "Suspension de démonstration renouvelée à la connexion";
    private final ParticipantRepository participants;
    private final PenaliteRepository penalites;
    private final MouvementDetteRepository mouvements;
    private final EntityManager entityManager;

    @Value("${app.demo.reset-penalty-on-login:true}")
    private boolean enabled = true;

    @Transactional
    public Participant prepare(Participant authenticated) {
        if (!enabled || !"L10002".equalsIgnoreCase(authenticated.getMatricule())
                || !"demo.penalise@gmail.com".equalsIgnoreCase(authenticated.getEmail())
                || authenticated.getTypeParticipant() != TypeParticipant.VISITEUR) {
            return authenticated;
        }

        // Même verrou que les règlements : les connexions concurrentes ne cumulent pas la dette.
        participants.lockParticipantById(authenticated.getId());
        Participant participant = participants.findById(authenticated.getId()).orElseThrow();
        entityManager.refresh(participant);
        if (participant.getSolde() < 90.0) {
            BigDecimal added = BigDecimal.valueOf(90).subtract(BigDecimal.valueOf(participant.getSolde()));
            mouvements.save(MouvementDette.builder()
                    .participantId(participant.getId()).nature(MouvementDette.Nature.SOLDE_INITIAL)
                    .montant(added).referenceSource("demo-login:" + UUID.randomUUID())
                    .description("Réactivation de la dette de démonstration à la connexion")
                    .dateOperation(LocalDateTime.now()).build());
            participant.setSolde(90.0);
            participants.save(participant);
        }

        Penalite penalty = penalites.findFirstByParticipantIdAndMotif(participant.getId(), MOTIF)
                .orElseGet(() -> Penalite.builder().participant(participant).motif(MOTIF).build());
        penalty.setDateFin(LocalDate.now().plusDays(7));
        penalites.save(penalty);
        return participant;
    }
}
