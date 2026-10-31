package com.padell.padell.repository;

import com.padell.padell.entity.*;
import com.padell.padell.entity.enums.StatutAtelier;
import com.padell.padell.entity.enums.StatutReservation;
import com.padell.padell.entity.enums.TypeAtelier;
import com.padell.padell.entity.enums.TypeParticipant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Tests de ReservationRepository")
class ReservationRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private AtelierRepository matchRepository;

    @Autowired
    private ParticipantRepository membreRepository;

    @Autowired
    private CentreRepository siteRepository;

    @Autowired
    private SalleRepository terrainRepository;

    @Test
    @DisplayName("existsByMatchIdAndMembreId détecte un membre déjà inscrit")
    void shouldFindExistingReservationForMemberAndMatch() {
        Centre site = siteRepository.save(site());
        Salle terrain = terrainRepository.save(Salle.builder().nom("Court A").site(site).prix(60.0).build());
        Participant organisateur = membreRepository.save(membre("G1001"));
        Participant joueur = membreRepository.save(membre("G1002"));
        Atelier match = matchRepository.save(match(terrain, organisateur));

        reservationRepository.save(Reservation.builder()
                .match(match)
                .membre(joueur)
                .statut(StatutReservation.EN_ATTENTE)
                .build());

        assertThat(reservationRepository.existsByMatchIdAndMembreId(match.getId(), joueur.getId())).isTrue();
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

    private Atelier match(Salle terrain, Participant organisateur) {
        LocalDateTime start = LocalDateTime.of(2026, 7, 10, 10, 0);
        return Atelier.builder()
                .terrain(terrain)
                .organisateur(organisateur)
                .dateDebut(start)
                .dateFin(start.plusMinutes(90))
                .typeMatch(TypeAtelier.PUBLIC)
                .statut(StatutAtelier.PLANIFIE)
                .nbJoueursActuels(1)
                .prixTotal(60.0)
                .prixParJoueur(15.0)
                .build();
    }
}
