package com.atelierconnect.service;

import com.atelierconnect.config.JwtConfig;
import com.atelierconnect.entity.*;
import com.atelierconnect.entity.enums.*;
import com.atelierconnect.repository.*;
import com.atelierconnect.scheduler.AtelierScheduler;
import com.atelierconnect.support.IsolatedPostgres;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class IncompletePrivatePenaltyIntegrationTest {
    @DynamicPropertySource
    static void requireIsolatedPostgres(DynamicPropertyRegistry properties) {
        IsolatedPostgres.configure(properties);
    }
    @Autowired AtelierScheduler scheduler;
    @Autowired ParticipantService participantService;
    @Autowired CentreRepository centres;
    @Autowired SalleRepository salles;
    @Autowired AtelierRepository ateliers;
    @Autowired ParticipantRepository participants;
    @Autowired PenaliteRepository penalites;
    @Autowired ReservationRepository reservations;
    @Autowired PaiementRepository paiements;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JwtConfig jwt;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    Centre centre;
    Salle salle;
    Participant organisateur;
    Atelier atelier;
    final List<Long> paidGuests = new ArrayList<>();

    @BeforeEach void fixture() {
        centre = centres.save(Centre.builder().nom("Test sanction " + UUID.randomUUID()).adresse("Test isolé")
                .heureOuverture(LocalTime.of(8, 0)).heureFermeture(LocalTime.of(22, 0))
                .dureeAtelierMinutes(120).dureeEntreAtelierMinutes(20).anneeCivile(LocalDate.now().getYear())
                .ouvertVisiteurs(true).build());
        salle = salles.save(Salle.builder().nom("Salle test sanction").centre(centre).build());
        organisateur = participants.save(Participant.builder().matricule("SAN-" + UUID.randomUUID())
                .nom("Organisateur").prenom("Test").typeParticipant(TypeParticipant.PARTENAIRE).solde(0.0).build());
        LocalDateTime start = LocalDate.now().plusDays(1).atTime(10, 0);
        atelier = ateliers.save(Atelier.builder().salle(salle).organisateur(organisateur)
                .dateDebut(start).dateFin(start.plusMinutes(120)).typeAtelier(TypeAtelier.PRIVE)
                .statut(StatutAtelier.PLANIFIE).nbParticipantsActuels(5)
                .prixTotal(90.0).prixParParticipant(15.0).build());
        replacePaidSeats(atelier, 5);
    }

    void replacePaidSeats(Atelier target, int count) {
        reservations.findByAtelierId(target.getId()).forEach(r -> reservations.deleteById(r.getId()));
        for (int i = 0; i < count; i++) {
            Participant guest = participants.save(Participant.builder().matricule("PAY-" + UUID.randomUUID())
                    .nom("Invité").prenom("Test").typeParticipant(TypeParticipant.PARTENAIRE).solde(0.0).build());
            paidGuests.add(guest.getId());
            Reservation reservation = reservations.save(Reservation.builder().atelier(target).participant(guest)
                    .statut(StatutReservation.CONFIRMEE).build());
            paiements.save(Paiement.builder().reservation(reservation).montant(15.0).statut(StatutPaiement.PAYE)
                    .datePaiement(LocalDateTime.now()).build());
        }
    }

    List<Penalite> ownPenalties() {
        return penalites.findAll().stream()
                .filter(p -> p.getParticipant().getId().equals(organisateur.getId())).toList();
    }

    @ParameterizedTest(name = "{0} {1}, J+{2}, {3}/6 confirmés : sanction={4}")
    @CsvSource({"PRIVE,PLANIFIE,1,0,true", "PRIVE,PLANIFIE,1,5,true", "PRIVE,PLANIFIE,1,6,false",
            "PUBLIC,PLANIFIE,1,5,false", "PRIVE,ANNULE,1,5,false", "PRIVE,COMPLET,1,6,false",
            "PRIVE,PLANIFIE,2,5,false", "PRIVE,PLANIFIE,0,5,false", "PRIVE,PLANIFIE,-1,5,false"})
    void schedulerAppliesOnlyAtJMinusOneToIncompletePlannedPrivateWorkshops(
            TypeAtelier type, StatutAtelier statut, int dayOffset, int confirmed, boolean expected) {
        atelier.setTypeAtelier(type);
        atelier.setStatut(statut);
        atelier.setDateDebut(LocalDate.now().plusDays(dayOffset).atTime(10, 0));
        atelier.setDateFin(atelier.getDateDebut().plusMinutes(120));
        atelier.setNbParticipantsActuels(confirmed);
        ateliers.save(atelier);
        replacePaidSeats(atelier, confirmed);

        scheduler.processIncompletePrivateAteliers();

        assertThat(ownPenalties()).hasSize(expected ? 1 : 0);
        assertThat(participantService.hasActivePenalty(organisateur.getId())).isEqualTo(expected);
        if (expected) {
            Penalite penalty = ownPenalties().getFirst();
            assertThat(penalty.getDateFin()).isEqualTo(LocalDate.now().plusWeeks(1));
            assertThat(penalty.getMotif()).contains("Atelier privé incomplet", "[atelier #" + atelier.getId() + "]");
        }
        Atelier unchanged = ateliers.findById(atelier.getId()).orElseThrow();
        assertThat(unchanged.getTypeAtelier()).isEqualTo(type);
        assertThat(unchanged.getStatut()).isEqualTo(statut);
        assertThat(unchanged.getNbParticipantsActuels()).isEqualTo(confirmed);
        assertThat(participants.findById(organisateur.getId()).orElseThrow().getSolde())
                .isEqualTo(expected ? (6 - confirmed) * 15.0 : 0.0);
    }

    @Test void repeatedChecksDoNotExtendAnExistingPenaltyEvenAfterItsExpiry() {
        scheduler.processIncompletePrivateAteliers();
        Penalite penalty = ownPenalties().getFirst();
        Long penaltyId = penalty.getId();
        LocalDate originalEnd = penalty.getDateFin();
        scheduler.processIncompletePrivateAteliers();
        assertThat(ownPenalties()).hasSize(1);
        assertThat(ownPenalties().getFirst().getDateFin()).isEqualTo(originalEnd);

        penalty.setDateFin(LocalDate.now().minusDays(1));
        penalites.save(penalty);
        scheduler.processIncompletePrivateAteliers();
        assertThat(ownPenalties()).hasSize(1);
        assertThat(ownPenalties().getFirst().getId()).isEqualTo(penaltyId);
        assertThat(ownPenalties().getFirst().getDateFin()).isEqualTo(penalty.getDateFin());
    }

    @Test void newAutomaticPenaltyIsReportedAndBlocksBothCreationModesThroughRealJwt() throws Exception {
        scheduler.processIncompletePrivateAteliers();
        String token = jwt.generateToken(organisateur.getMatricule(), "PARTENAIRE", "MEMBER");
        mvc.perform(get("/api/participants/{id}/penalty", organisateur.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(content().string("true"));
        for (String type : List.of("PUBLIC", "PRIVE")) {
            mvc.perform(post("/api/ateliers").header("Authorization", "Bearer " + token)
                            .contentType("application/json").content(json.writeValueAsString(Map.of(
                                    "salleId", salle.getId(), "atelierDate", LocalDate.now().plusDays(3).atTime(10, 0).toString(),
                                    "atelierType", type))))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message")
                            .value("Vous avez un solde impayé et ne pouvez pas créer d'atelier."));
        }
        assertThat(ateliers.findByOrganisateurId(organisateur.getId())).hasSize(1);
    }

    @Test void eachIncompleteWorkshopChargesItsMissingSeatsOnlyOnce() {
        Atelier second = ateliers.save(Atelier.builder().salle(salle).organisateur(organisateur)
                .dateDebut(atelier.getDateDebut().plusHours(3)).dateFin(atelier.getDateFin().plusHours(3))
                .typeAtelier(TypeAtelier.PRIVE).statut(StatutAtelier.PLANIFIE).nbParticipantsActuels(1)
                .prixTotal(90.0).prixParParticipant(15.0).build());
        replacePaidSeats(second, 1);
        scheduler.processIncompletePrivateAteliers();
        scheduler.processIncompletePrivateAteliers();
        assertThat(ownPenalties()).hasSize(2);
        assertThat(ownPenalties().stream().map(Penalite::getMotif))
                .anyMatch(m -> m.contains("[atelier #" + atelier.getId() + "]"))
                .anyMatch(m -> m.contains("[atelier #" + second.getId() + "]"));
        assertThat(participants.findById(organisateur.getId()).orElseThrow().getSolde()).isEqualTo(90.0);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_CONCURRENCY_TEST", matches = "true")
    void concurrentChecksCreateOnlyOnePenalty() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try {
            List<Future<?>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) results.add(pool.submit(() -> {
                ready.countDown();
                if (!start.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("Start timeout");
                participantService.penalizeIncompletePrivateAtelier(atelier.getId());
                return null;
            }));
            assertThat(ready.await(15, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> result : results) result.get(30, TimeUnit.SECONDS);
            assertThat(ownPenalties()).hasSize(1);
        } finally { start.countDown(); pool.shutdownNow(); }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_CONCURRENCY_TEST", matches = "true")
    void rechecksTheFreshStateWhenLastPlaceWasConfirmedAfterAnEarlierRead() {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        TransactionTemplate tx = new TransactionTemplate(transactions);
        try {
            tx.executeWithoutResult(status -> {
                Atelier stale = ateliers.findById(atelier.getId()).orElseThrow();
                assertThat(stale.getNbParticipantsActuels()).isEqualTo(5);
                Future<?> lastPlace = pool.submit(() -> tx.executeWithoutResult(otherStatus -> {
                    Atelier completed = ateliers.findById(atelier.getId()).orElseThrow();
                    completed.setNbParticipantsActuels(6);
                    completed.setStatut(StatutAtelier.COMPLET);
                    ateliers.saveAndFlush(completed);
                }));
                try { lastPlace.get(30, TimeUnit.SECONDS); }
                catch (Exception failure) { throw new IllegalStateException(failure); }
                participantService.penalizeIncompletePrivateAtelier(atelier.getId());
                assertThat(stale.getNbParticipantsActuels()).isEqualTo(6);
            });
            assertThat(ownPenalties()).isEmpty();
        } finally { pool.shutdownNow(); }
    }

    @AfterEach void cleanupOnlyOwnFixtures() {
        if (salle != null) salles.deleteById(salle.getId());
        if (organisateur != null) participants.deleteById(organisateur.getId());
        paidGuests.forEach(participants::deleteById);
        if (centre != null) centres.deleteById(centre.getId());
    }
}
