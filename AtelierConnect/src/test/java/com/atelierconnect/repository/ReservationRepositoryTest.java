package com.atelierconnect.repository;

import com.atelierconnect.entity.*;
import com.atelierconnect.entity.enums.StatutAtelier;
import com.atelierconnect.entity.enums.StatutReservation;
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
@DisplayName("Tests de ReservationRepository")
class ReservationRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private AtelierRepository atelierRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private CentreRepository centreRepository;

    @Autowired
    private SalleRepository salleRepository;

    @Test
    @DisplayName("existsByMatchIdAndMembreId détecte un participant déjà inscrit")
    void shouldFindExistingReservationForParticipantAndAtelier() {
        Centre centre = centreRepository.save(centre());
        Salle salle = salleRepository.save(Salle.builder().nom("Court A").centre(centre).prix(60.0).build());
        Participant organisateur = participantRepository.save(participant("G1001"));
        Participant participant = participantRepository.save(participant("G1002"));
        Atelier atelier = atelierRepository.save(atelier(salle, organisateur));

        reservationRepository.save(Reservation.builder()
                .atelier(atelier)
                .participant(participant)
                .statut(StatutReservation.EN_ATTENTE)
                .build());

        assertThat(reservationRepository.existsByAtelierIdAndParticipantId(atelier.getId(), participant.getId())).isTrue();
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

    private Atelier atelier(Salle salle, Participant organisateur) {
        LocalDateTime start = LocalDateTime.of(2026, 7, 10, 10, 0);
        return Atelier.builder()
                .salle(salle)
                .organisateur(organisateur)
                .dateDebut(start)
                .dateFin(start.plusMinutes(90))
                .typeAtelier(TypeAtelier.PUBLIC)
                .statut(StatutAtelier.PLANIFIE)
                .nbParticipantsActuels(1)
                .prixTotal(60.0)
                .prixParParticipant(15.0)
                .build();
    }
}
