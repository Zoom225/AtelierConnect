package com.atelierconnect.repository;

import com.atelierconnect.entity.Atelier;
import com.atelierconnect.entity.Participant;
import com.atelierconnect.entity.Centre;
import com.atelierconnect.entity.Salle;
import com.atelierconnect.entity.enums.StatutAtelier;
import com.atelierconnect.entity.enums.TypeAtelier;
import com.atelierconnect.entity.enums.TypeParticipant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Tests de MatchRepository")
class AtelierRepositoryTest {

    @Autowired
    private AtelierRepository atelierRepository;

    @Autowired
    private CentreRepository centreRepository;

    @Autowired
    private SalleRepository salleRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Test
    @DisplayName("findOverlappingMatches retourne les atelier planifiés qui chevauchent le créneau")
    void shouldFindOverlappingAteliers() {
        Salle salle = salleRepository.save(Salle.builder()
                .nom("Court A")
                .centre(centreRepository.save(centre()))
                .prix(60.0)
                .build());
        Participant organisateur = participantRepository.save(participant("G1001"));

        LocalDateTime start = LocalDateTime.of(2026, 7, 10, 10, 0);
        atelierRepository.save(atelier(salle, organisateur, start, StatutAtelier.PLANIFIE));

        assertThat(atelierRepository.findOverlappingAteliers(
                salle.getId(),
                start.plusMinutes(30),
                start.plusMinutes(120),
                StatutAtelier.ANNULE
        )).hasSize(1);
    }

    @Test
    @DisplayName("findOverlappingMatches ignore les atelier annulés")
    void shouldIgnoreCancelledAteliers() {
        Salle salle = salleRepository.save(Salle.builder()
                .nom("Court A")
                .centre(centreRepository.save(centre()))
                .prix(60.0)
                .build());
        Participant organisateur = participantRepository.save(participant("G1001"));

        LocalDateTime start = LocalDateTime.of(2026, 7, 10, 10, 0);
        atelierRepository.save(atelier(salle, organisateur, start, StatutAtelier.ANNULE));

        assertThat(atelierRepository.findOverlappingAteliers(
                salle.getId(),
                start.plusMinutes(30),
                start.plusMinutes(120),
                StatutAtelier.ANNULE
        )).isEmpty();
    }

    private Centre centre() {
        return Centre.builder()
                .nom("Centre collaboratif Lyon")
                .adresse("12 rue de la République")
                .heureOuverture(LocalTime.of(8, 0))
                .heureFermeture(LocalTime.of(22, 0))
                .dureeAtelierMinutes(90)
                .dureeEntreAtelierMinutes(15)
                .anneeCivile(2026)
                .build();
    }

    private Participant participant(String matricule) {
        return Participant.builder()
                .matricule(matricule)
                .nom("Martin")
                .prenom("Lucas")
                .email(matricule.toLowerCase() + "@example.com")
                .typeParticipant(TypeParticipant.PARTENAIRE)
                .solde(0.0)
                .build();
    }

    private Atelier atelier(Salle salle, Participant organisateur, LocalDateTime start, StatutAtelier statut) {
        return Atelier.builder()
                .salle(salle)
                .organisateur(organisateur)
                .dateDebut(start)
                .dateFin(start.plusMinutes(90))
                .typeAtelier(TypeAtelier.PUBLIC)
                .statut(statut)
                .nbParticipantsActuels(1)
                .prixTotal(60.0)
                .prixParParticipant(15.0)
                .build();
    }
}
