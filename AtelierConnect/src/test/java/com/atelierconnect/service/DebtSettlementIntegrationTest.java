package com.atelierconnect.service;

import com.atelierconnect.config.JwtConfig;
import com.atelierconnect.dto.request.ReglerDetteRequest;
import com.atelierconnect.entity.*;
import com.atelierconnect.entity.enums.*;
import com.atelierconnect.exception.BusinessException;
import com.atelierconnect.repository.*;
import com.atelierconnect.scheduler.AtelierScheduler;
import com.atelierconnect.service.impl.DetteService;
import com.atelierconnect.support.IsolatedPostgres;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class DebtSettlementIntegrationTest {
    /** Runs before Spring creates its datasource, schema or demonstration data. */
    @DynamicPropertySource
    static void requireIsolatedPostgres(DynamicPropertyRegistry properties) {
        IsolatedPostgres.configure(properties);
    }

    @Autowired DetteService dettes;
    @Autowired AtelierScheduler scheduler;
    @Autowired ParticipantService participantService;
    @Autowired PaiementService paiementService;
    @Autowired ReservationService reservationService;
    @Autowired AtelierService atelierService;
    @Autowired CentreRepository centres;
    @Autowired SalleRepository salles;
    @Autowired ParticipantRepository participants;
    @Autowired AtelierRepository ateliers;
    @Autowired ReservationRepository reservations;
    @Autowired PaiementRepository paiements;
    @Autowired PenaliteRepository penalites;
    @Autowired MouvementDetteRepository mouvements;
    @Autowired MockMvc mvc;
    @Autowired JwtConfig jwt;
    @Autowired ObjectMapper json;
    Centre centre;
    Salle salle;
    Participant owner;
    Atelier atelier;
    final List<Long> guests = new ArrayList<>();

    @BeforeEach void fixture() {
        centre = centres.save(Centre.builder().nom("Dette " + UUID.randomUUID()).adresse("Validation isolée")
                .heureOuverture(LocalTime.of(8, 0)).heureFermeture(LocalTime.of(22, 0))
                .dureeAtelierMinutes(120).dureeEntreAtelierMinutes(20).anneeCivile(LocalDate.now().getYear())
                .ouvertVisiteurs(true).build());
        salle = salles.save(Salle.builder().nom("Salle dette").centre(centre).build());
        owner = member(); guests.remove(owner.getId());
        atelier = workshop(TypeAtelier.PRIVE, LocalDate.now().plusDays(1), owner);
    }

    Participant member() {
        Participant p = participants.save(Participant.builder().matricule("DET-" + UUID.randomUUID())
                .nom("Test").prenom("Dette").typeParticipant(TypeParticipant.PARTENAIRE).solde(0.0).build());
        guests.add(p.getId()); return p;
    }
    Atelier workshop(TypeAtelier type, LocalDate day, Participant organisateur) {
        return ateliers.save(Atelier.builder().salle(salle).organisateur(organisateur).typeAtelier(type)
                .statut(StatutAtelier.PLANIFIE).dateDebut(day.atTime(10, 0)).dateFin(day.atTime(12, 0))
                .nbParticipantsActuels(0).prixTotal(90.0).prixParParticipant(15.0).build());
    }
    Reservation seat(boolean paid) {
        Participant p = member();
        Reservation r = reservations.save(Reservation.builder().atelier(atelier).participant(p)
                .statut(paid ? StatutReservation.CONFIRMEE : StatutReservation.EN_ATTENTE).build());
        paiements.save(Paiement.builder().reservation(r).montant(15.0)
                .statut(paid ? StatutPaiement.PAYE : StatutPaiement.EN_ATTENTE).build());
        if (paid) {
            atelier.setNbParticipantsActuels(atelier.getNbParticipantsActuels() + 1); ateliers.save(atelier);
        }
        return r;
    }
    void balance(double amount) { owner = participants.findById(owner.getId()).orElseThrow(); owner.setSolde(amount); participants.save(owner); }
    double balance() { return participants.findById(owner.getId()).orElseThrow().getSolde(); }
    long settlements() { return mouvements.findTop50ByParticipantIdOrderByIdDesc(owner.getId()).stream()
            .filter(m -> m.getNature() == MouvementDette.Nature.REGLEMENT_DETTE).count(); }
    String token(Participant p) { return jwt.generateToken(p.getMatricule(), "PARTENAIRE", "MEMBER"); }
    ReglerDetteRequest request(double amount, UUID key) { return new ReglerDetteRequest(BigDecimal.valueOf(amount), key); }

    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 4, 5, 6})
    void chargesOnlyTheRemainingPartOfTheNinetyEuroBudget(int paid) {
        for (int i = 0; i < paid; i++) seat(true);
        scheduler.processIncompletePrivateAteliers(); scheduler.processIncompletePrivateAteliers();
        assertThat(balance()).isEqualTo((6 - paid) * 15.0);
        assertThat(participantService.hasActivePenalty(owner.getId())).isEqualTo(paid < 6);
        assertThat(mouvements.findTop50ByParticipantIdOrderByIdDesc(owner.getId())).hasSize(paid < 6 ? 1 : 0);
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void unpaidJobAndPenaltyJobNeverChargePrivateInvitationsTwice(boolean penaltyFirst) {
        seat(true); seat(true);
        List<Reservation> pending = new ArrayList<>();
        for (int i = 0; i < 4; i++) pending.add(seat(false));
        if (penaltyFirst) scheduler.processIncompletePrivateAteliers();
        paiementService.checkUnpaidBeforeAtelier(); scheduler.processIncompletePrivateAteliers(); paiementService.checkUnpaidBeforeAtelier();
        assertThat(balance()).isEqualTo(60.0);
        assertThat(mouvements.findTop50ByParticipantIdOrderByIdDesc(owner.getId())).hasSize(1);
        for (Reservation r : pending) assertThat(reservations.findById(r.getId()).orElseThrow().getStatut()).isEqualTo(StatutReservation.ANNULEE);
    }

    @Test void lateIndividualPaymentAndRefundCannotBillTheSameSeatTwice() {
        Reservation paid = seat(true), pending = seat(false);
        scheduler.processIncompletePrivateAteliers();
        assertThat(balance()).isEqualTo(75.0);
        assertThatThrownBy(() -> paiementService.pay(pending.getId(), pending.getParticipant().getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("imputées");
        assertThatThrownBy(() -> reservationService.cancel(paid.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("échéance");
        assertThatThrownBy(() -> atelierService.convertToPublic(atelier.getId())).isInstanceOf(BusinessException.class);
        assertThat(paiements.findByReservationId(pending.getId()).orElseThrow().getStatut()).isEqualTo(StatutPaiement.EN_ATTENTE);
        assertThat(paiements.findByReservationId(paid.getId()).orElseThrow().getStatut()).isEqualTo(StatutPaiement.PAYE);
    }

    @Test void aDebtIncludedInAnEarlierPaymentIsNotCountedAsExtraPaidSeats() {
        atelier.setDateDebut(LocalDate.now().plusDays(2).atTime(10, 0)); atelier.setDateFin(atelier.getDateDebut().plusHours(2)); ateliers.save(atelier);
        Reservation pending = seat(false);
        Participant guest = participants.findById(pending.getParticipant().getId()).orElseThrow();
        guest.setSolde(30.0); participants.save(guest);
        Paiement paid = paiementService.pay(pending.getId(), guest.getId());
        assertThat(paid.getMontant()).isEqualTo(45.0);
        assertThat(participants.findById(guest.getId()).orElseThrow().getSolde()).isZero();
        atelier = ateliers.findById(atelier.getId()).orElseThrow();
        atelier.setDateDebut(LocalDate.now().plusDays(1).atTime(10, 0)); atelier.setDateFin(atelier.getDateDebut().plusHours(2)); ateliers.save(atelier);
        scheduler.processIncompletePrivateAteliers(); assertThat(balance()).isEqualTo(75.0);
    }

    @Test void publicUnpaidReservationKeepsTheExistingPerSeatCharge() {
        atelier.setTypeAtelier(TypeAtelier.PUBLIC); ateliers.save(atelier);
        Reservation pending = seat(false);
        paiementService.checkUnpaidBeforeAtelier(); paiementService.checkUnpaidBeforeAtelier();
        assertThat(balance()).isEqualTo(15.0);
        assertThat(participantService.hasActivePenalty(owner.getId())).isFalse();
        assertThat(paiements.findByReservationId(pending.getId()).orElseThrow().getStatut()).isEqualTo(StatutPaiement.ANNULE);
    }

    @Test void standaloneSettlementIsAuthenticatedIdempotentAndDoesNotEraseSuspension() throws Exception {
        balance(90);
        penalites.save(Penalite.builder().participant(owner).dateFin(LocalDate.now().plusDays(7)).motif("Test").build());
        UUID key = UUID.randomUUID(); String body = json.writeValueAsString(request(90, key));
        mvc.perform(post("/api/paiements/dette/pay").contentType("application/json").content(body)).andExpect(status().isForbidden());
        for (int i = 0; i < 2; i++) mvc.perform(post("/api/paiements/dette/pay")
                .header("Authorization", "Bearer " + token(owner)).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.solde").value(0)).andExpect(jsonPath("$.suspensionActive").value(true));
        assertThat(settlements()).isEqualTo(1); assertThat(participantService.hasActivePenalty(owner.getId())).isTrue();
        balance(15); dettes.pay(owner.getId(), request(90, key));
        assertThat(balance()).isEqualTo(15.0);
        assertThatThrownBy(() -> dettes.pay(owner.getId(), request(15, key))).isInstanceOf(BusinessException.class);
    }

    @Test void wrongAmountAndForeignIdentityNeverClearTheOwnersDebt() throws Exception {
        balance(90); Participant foreign = member();
        mvc.perform(post("/api/paiements/dette/pay").header("Authorization", "Bearer " + token(owner))
                .contentType("application/json").content(json.writeValueAsString(request(60, UUID.randomUUID()))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Votre solde a changé. Actualisez le montant avant de confirmer le règlement."));
        mvc.perform(post("/api/paiements/dette/pay").header("Authorization", "Bearer " + token(foreign))
                .contentType("application/json").content(json.writeValueAsString(Map.of("montantAttendu",90,
                        "cleRequete",UUID.randomUUID(),"participantId",owner.getId())))).andExpect(status().isBadRequest());
        assertThat(balance()).isEqualTo(90.0); assertThat(settlements()).isZero();
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void bothDebtAndSuspensionMustEndBeforeCreationAndParticipation(boolean active) throws Exception {
        balance(90);
        penalites.save(Penalite.builder().participant(owner).dateFin(LocalDate.now().plusDays(active ? 7 : -1)).motif("Test").build());
        Atelier publicAtelier = workshop(TypeAtelier.PUBLIC, LocalDate.now().plusDays(3), member());
        String auth = "Bearer " + token(owner);
        String creation = json.writeValueAsString(Map.of("salleId",salle.getId(),"atelierDate",LocalDate.now().plusDays(4).atTime(10,0).toString(),"atelierType","PUBLIC"));
        String participation = json.writeValueAsString(Map.of("atelierId",publicAtelier.getId(),"participantId",owner.getId(),"requesterId",owner.getId()));
        mvc.perform(post("/api/ateliers").header("Authorization",auth).contentType("application/json").content(creation)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/reservations").header("Authorization",auth).contentType("application/json").content(participation)).andExpect(status().isBadRequest());
        dettes.pay(owner.getId(), request(90,UUID.randomUUID()));
        mvc.perform(post("/api/ateliers").header("Authorization",auth).contentType("application/json").content(creation)).andExpect(status().is(active ? 400 : 201));
        mvc.perform(post("/api/reservations").header("Authorization",auth).contentType("application/json").content(participation)).andExpect(status().is(active ? 400 : 201));
    }

    @Test @EnabledIfEnvironmentVariable(named="RUN_POSTGRES_CONCURRENCY_TEST", matches="true")
    void concurrentSettlementAndChargeCannotLoseOrEraseAnAmount() throws Exception {
        balance(15); seat(true); seat(true);
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch ready=new CountDownLatch(2),start=new CountDownLatch(1);
        try {
            Future<Boolean> payment=pool.submit(() -> {ready.countDown(); start.await(); try {dettes.pay(owner.getId(),request(15,UUID.randomUUID()));return true;} catch(BusinessException changed){return false;}});
            Future<?> charge=pool.submit(() -> {ready.countDown(); start.await();participantService.penalizeIncompletePrivateAtelier(atelier.getId());return null;});
            assertThat(ready.await(15,TimeUnit.SECONDS)).isTrue();start.countDown();
            boolean paid=payment.get(45,TimeUnit.SECONDS);charge.get(45,TimeUnit.SECONDS);
            assertThat(balance()).isEqualTo(paid ? 60.0 : 75.0); assertThat(settlements()).isEqualTo(paid ? 1 : 0);
        } finally {start.countDown();pool.shutdownNow();}
    }

    @Test @EnabledIfEnvironmentVariable(named="RUN_POSTGRES_CONCURRENCY_TEST", matches="true")
    void doubleClickWithSameRequestKeyCreatesOnlyOneSettlement() throws Exception {
        balance(90); UUID key=UUID.randomUUID(); ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Future<?> first=pool.submit(() -> dettes.pay(owner.getId(),request(90,key)));
            Future<?> second=pool.submit(() -> dettes.pay(owner.getId(),request(90,key)));
            first.get(45,TimeUnit.SECONDS);second.get(45,TimeUnit.SECONDS);
            assertThat(balance()).isZero();assertThat(settlements()).isEqualTo(1);
        } finally {pool.shutdownNow();}
    }

    @Test void cancellingAPaidReservationKeepsItsCancelledState() {
        atelier.setDateDebut(LocalDate.now().plusDays(2).atTime(10, 0));
        atelier.setDateFin(atelier.getDateDebut().plusHours(2)); ateliers.save(atelier);
        Reservation pending = seat(false);
        paiementService.pay(pending.getId(), pending.getParticipant().getId());
        reservationService.cancel(pending.getId());
        assertThat(reservations.findById(pending.getId()).orElseThrow().getStatut()).isEqualTo(StatutReservation.ANNULEE);
        assertThat(paiements.findByReservationId(pending.getId()).orElseThrow().getStatut()).isEqualTo(StatutPaiement.REMBOURSE);
        assertThat(ateliers.findById(atelier.getId()).orElseThrow().getNbParticipantsActuels()).isZero();
    }

    @AfterEach void cleanupOnlyOwnFixtures() {
        if(salle!=null)salles.deleteById(salle.getId());
        if(owner!=null)participants.deleteById(owner.getId());
        guests.forEach(participants::deleteById);
        if(centre!=null)centres.deleteById(centre.getId());
    }
}
