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
    private AtelierRepository matchRepository;

    @Autowired
    private CentreRepository siteRepository;

    @Autowired
    private SalleRepository terrainRepository;

    @Autowired
    private ParticipantRepository membreRepository;

    @Test
    @DisplayName("findOverlappingMatches retourne les matchs planifiés qui chevauchent le créneau")
    void shouldFindOverlappingMatches() {
        Salle terrain = terrainRepository.save(Salle.builder()
                .nom("Court A")
                .site(siteRepository.save(site()))
                .prix(60.0)
                .build());
        Participant organisateur = membreRepository.save(membre("G1001"));

        LocalDateTime start = LocalDateTime.of(2026, 7, 10, 10, 0);
        matchRepository.save(match(terrain, organisateur, start, StatutAtelier.PLANIFIE));

        assertThat(matchRepository.findOverlappingAteliers(
                terrain.getId(),
                start.plusMinutes(30),
                start.plusMinutes(120),
                StatutAtelier.ANNULE
        )).hasSize(1);
    }

    @Test
    @DisplayName("findOverlappingMatches ignore les matchs annulés")
    void shouldIgnoreCancelledMatches() {
        Salle terrain = terrainRepository.save(Salle.builder()
                .nom("Court A")
                .site(siteRepository.save(site()))
                .prix(60.0)
                .build());
        Participant organisateur = membreRepository.save(membre("G1001"));

        LocalDateTime start = LocalDateTime.of(2026, 7, 10, 10, 0);
        matchRepository.save(match(terrain, organisateur, start, StatutAtelier.ANNULE));

        assertThat(matchRepository.findOverlappingAteliers(
                terrain.getId(),
                start.plusMinutes(30),
                start.plusMinutes(120),
                StatutAtelier.ANNULE
        )).isEmpty();
    }

    private Centre site() {
        return Centre.builder()
                .nom("Padel Club Lyon")
                .adresse("12 rue de la République")
                .heureOuverture(LocalTime.of(8, 0))
                .heureFermeture(LocalTime.of(22, 0))
                .dureeMatchMinutes(90)
                .dureeEntreMatchMinutes(15)
                .anneeCivile(2026)
                .build();
    }

    private Participant membre(String matricule) {
        return Participant.builder()
                .matricule(matricule)
                .nom("Martin")
                .prenom("Lucas")
                .email(matricule.toLowerCase() + "@example.com")
                .typeMembre(TypeParticipant.GLOBAL)
                .solde(0.0)
                .build();
    }

    private Atelier match(Salle terrain, Participant organisateur, LocalDateTime start, StatutAtelier statut) {
        return Atelier.builder()
                .terrain(terrain)
                .organisateur(organisateur)
                .dateDebut(start)
                .dateFin(start.plusMinutes(90))
                .typeMatch(TypeAtelier.PUBLIC)
                .statut(statut)
                .nbJoueursActuels(1)
                .prixTotal(60.0)
                .prixParJoueur(15.0)
                .build();
    }
}
