package com.atelierconnect.service;

import com.atelierconnect.entity.*;
import com.atelierconnect.entity.enums.*;
import com.atelierconnect.exception.BusinessException;
import com.atelierconnect.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test réel des verrous PostgreSQL, sans H2 ni base réinitialisée.
 * Activer avec RUN_POSTGRES_CONCURRENCY_TEST=true et une base de TEST distincte.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_CONCURRENCY_TEST", matches = "true")
class PaiementConcurrencyPostgresTest {

    @Autowired private PaiementService paiementService;
    @Autowired private ReservationService reservationService;
    @Autowired private CentreRepository centreRepository;
    @Autowired private SalleRepository salleRepository;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private AtelierRepository atelierRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private PaiementRepository paiementRepository;

    private final List<Long> paiementIds = new ArrayList<>();
    private final List<Long> reservationIds = new ArrayList<>();
    private final List<Long> participantIds = new ArrayList<>();
    private Long atelierId;
    private Long salleId;
    private Long centreId;

    @Test
    void deuxInvitationsPriveesPourDernierInviteUneSeuleReussit() throws Exception {
        JeuEssai jeu = creerJeuEssai();
        Atelier atelier = jeu.atelier();
        atelier.setTypeAtelier(TypeAtelier.PRIVE);
        atelier.setNbParticipantsActuels(0);
        atelierRepository.save(atelier);
        creerReservation(atelier, jeu.participantA());
        for (int i = 0; i < 4; i++) {
            creerReservation(atelier, creerParticipant("Invite-" + UUID.randomUUID()));
        }
        Participant dernierInvite = creerParticipant("Concurrent-" + UUID.randomUUID());
        List<Boolean> resultats = executerSimultanement(
                () -> inviterOuRefuser(atelier.getId(), jeu.participantB().getId(), jeu.participantA().getId()),
                () -> inviterOuRefuser(atelier.getId(), dernierInvite.getId(), jeu.participantA().getId()));

        assertThat(resultats).containsExactlyInAnyOrder(true, false);
        assertThat(reservationRepository.countByAtelierIdAndStatutNot(atelierId, StatutReservation.ANNULEE))
                .isEqualTo(6);
        assertThat(atelierRepository.findById(atelierId).orElseThrow().getNbParticipantsActuels()).isZero();
        // Inclure les lignes de l'invitation gagnante dans le nettoyage ciblé.
        for (Reservation reservation : reservationRepository.findByAtelierId(atelierId)) {
            if (!reservationIds.contains(reservation.getId())) reservationIds.add(reservation.getId());
            if (!paiementIds.contains(reservation.getPaiement().getId())) paiementIds.add(reservation.getPaiement().getId());
        }
    }

    private boolean inviterOuRefuser(Long atelierId, Long participantId, Long organisateurId) {
        try {
            reservationService.create(atelierId, participantId, organisateurId);
            return true;
        } catch (BusinessException refused) {
            return false;
        }
    }

    @Test
    void deuxParticipantsPourDernierePlaceUnSeulPaiementReussit() throws Exception {
        JeuEssai jeu = creerJeuEssai();
        long reservationA = creerReservation(jeu.atelier(), jeu.participantA());
        long reservationB = creerReservation(jeu.atelier(), jeu.participantB());

        List<Boolean> resultats = executerSimultanement(
                () -> payerOuRefuser(reservationA, jeu.participantA().getId()),
                () -> payerOuRefuser(reservationB, jeu.participantB().getId())
        );

        assertThat(resultats).containsExactlyInAnyOrder(true, false);
        assertThat(atelierRepository.findById(atelierId).orElseThrow().getNbParticipantsActuels())
                .isEqualTo(6);
        assertThat(paiementRepository.findByReservationId(reservationA).orElseThrow().getStatut())
                .isIn(StatutPaiement.PAYE, StatutPaiement.EN_ATTENTE);
        long paiementsConfirmes = List.of(reservationA, reservationB).stream()
                .filter(id -> paiementRepository.findByReservationId(id).orElseThrow()
                        .getStatut() == StatutPaiement.PAYE)
                .count();
        assertThat(paiementsConfirmes).isEqualTo(1);
    }

    @Test
    void doubleClicSurMemeReservationNePaieQuUneFois() throws Exception {
        JeuEssai jeu = creerJeuEssai();
        long reservationId = creerReservation(jeu.atelier(), jeu.participantA());

        List<Boolean> resultats = executerSimultanement(
                () -> payerOuRefuser(reservationId, jeu.participantA().getId()),
                () -> payerOuRefuser(reservationId, jeu.participantA().getId())
        );

        assertThat(resultats).containsExactlyInAnyOrder(true, false);
        assertThat(atelierRepository.findById(atelierId).orElseThrow().getNbParticipantsActuels())
                .isEqualTo(6);
        assertThat(reservationRepository.findById(reservationId).orElseThrow().getStatut())
                .isEqualTo(StatutReservation.CONFIRMEE);
        assertThat(paiementRepository.findByReservationId(reservationId).orElseThrow().getStatut())
                .isEqualTo(StatutPaiement.PAYE);
    }

    @Test
    void annulationConcurrenteEtPaiementNeLaissentPasDePlaceOccupee() throws Exception {
        JeuEssai jeu = creerJeuEssai();
        long reservationId = creerReservation(jeu.atelier(), jeu.participantA());

        List<Boolean> resultats = executerSimultanement(
                () -> payerOuRefuser(reservationId, jeu.participantA().getId()),
                () -> {
                    reservationService.cancel(reservationId);
                    return true;
                });

        assertThat(resultats).contains(true);
        assertThat(reservationRepository.findById(reservationId).orElseThrow().getStatut())
                .isEqualTo(StatutReservation.ANNULEE);
        assertThat(atelierRepository.findById(atelierId).orElseThrow().getNbParticipantsActuels())
                .isEqualTo(5);
        assertThat(paiementRepository.findByReservationId(reservationId).orElseThrow().getStatut())
                .isIn(StatutPaiement.ANNULE, StatutPaiement.REMBOURSE);
    }

    private boolean payerOuRefuser(long reservationId, long participantId) {
        try {
            paiementService.pay(reservationId, participantId);
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    private List<Boolean> executerSimultanement(Callable<Boolean> a, Callable<Boolean> b)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch depart = new CountDownLatch(1);
        try {
            Future<Boolean> premier = pool.submit(() -> { depart.await(); return a.call(); });
            Future<Boolean> second = pool.submit(() -> { depart.await(); return b.call(); });
            depart.countDown();
            return List.of(premier.get(30, TimeUnit.SECONDS),
                    second.get(30, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    private JeuEssai creerJeuEssai() {
        String identifiant = UUID.randomUUID().toString().substring(0, 8);
        Centre centre = centreRepository.save(Centre.builder()
                .nom("Centre concurrence " + identifiant)
                .adresse("Adresse de test")
                .heureOuverture(LocalTime.of(8, 0))
                .heureFermeture(LocalTime.of(22, 0))
                .dureeAtelierMinutes(120)
                .dureeEntreAtelierMinutes(20)
                .anneeCivile(LocalDateTime.now().getYear())
                .ouvertVisiteurs(true).build());
        centreId = centre.getId();
        Salle salle = salleRepository.save(Salle.builder()
                .nom("Salle de test").centre(centre).prix(90.0).build());
        salleId = salle.getId();
        Participant a = creerParticipant("A-" + identifiant);
        Participant b = creerParticipant("B-" + identifiant);

        LocalDateTime debut = LocalDateTime.now().plusDays(3)
                .withHour(10).withMinute(0).withSecond(0).withNano(0);
        Atelier atelier = atelierRepository.save(Atelier.builder()
                .salle(salle).organisateur(a)
                .dateDebut(debut).dateFin(debut.plusMinutes(120))
                .typeAtelier(TypeAtelier.PUBLIC).statut(StatutAtelier.PLANIFIE)
                .nbParticipantsActuels(5).prixTotal(90.0).prixParParticipant(15.0)
                .build());
        atelierId = atelier.getId();
        return new JeuEssai(atelier, a, b);
    }

    private Participant creerParticipant(String matricule) {
        Participant p = participantRepository.save(Participant.builder()
                .matricule("IT-" + matricule)
                .nom("Participant").prenom("Test")
                .typeParticipant(TypeParticipant.PARTENAIRE)
                .solde(0.0).build());
        participantIds.add(p.getId());
        return p;
    }

    private long creerReservation(Atelier atelier, Participant participant) {
        Reservation reservation = reservationRepository.save(Reservation.builder()
                .atelier(atelier).participant(participant).statut(StatutReservation.EN_ATTENTE)
                .build());
        reservationIds.add(reservation.getId());
        Paiement paiement = paiementRepository.save(Paiement.builder()
                .reservation(reservation).montant(15.0)
                .statut(StatutPaiement.EN_ATTENTE).build());
        paiementIds.add(paiement.getId());
        return reservation.getId();
    }

    @AfterEach
    void nettoyerJeuEssai() {
        // Uniquement les lignes créées par ce test, jamais de DROP ni de TRUNCATE.
        paiementIds.forEach(paiementRepository::deleteById);
        reservationIds.forEach(reservationRepository::deleteById);
        if (atelierId != null) atelierRepository.deleteById(atelierId);
        participantIds.forEach(participantRepository::deleteById);
        if (salleId != null) salleRepository.deleteById(salleId);
        if (centreId != null) centreRepository.deleteById(centreId);
    }

    private record JeuEssai(Atelier atelier, Participant participantA, Participant participantB) {}
}
