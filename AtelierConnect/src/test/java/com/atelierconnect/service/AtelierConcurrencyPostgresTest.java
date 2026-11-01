package com.atelierconnect.service;

import com.atelierconnect.dto.request.*;
import com.atelierconnect.entity.*;
import com.atelierconnect.entity.enums.*;
import com.atelierconnect.exception.BusinessException;
import com.atelierconnect.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_CONCURRENCY_TEST", matches = "true")
class AtelierConcurrencyPostgresTest {
    @Autowired AtelierService service;
    @Autowired CentreRepository centres;
    @Autowired SalleRepository salles;
    @Autowired ParticipantRepository participants;
    @Autowired AtelierRepository ateliers;
    Centre centre;
    Participant organisateur;
    Salle salle;
    LocalDate day;

    @BeforeEach void fixture() {
        centre = centres.save(Centre.builder().nom("Recette " + UUID.randomUUID()).adresse("Test")
                .heureOuverture(LocalTime.of(7, 0)).heureFermeture(LocalTime.of(23, 0))
                .dureeAtelierMinutes(120).dureeEntreAtelierMinutes(20)
                .anneeCivile(LocalDate.now().getYear()).ouvertVisiteurs(true).build());
        salle = newSalle();
        organisateur = participants.save(Participant.builder().matricule("REC-" + UUID.randomUUID())
                .nom("Test").prenom("Recette").typeParticipant(TypeParticipant.PARTENAIRE).solde(0.0).build());
        day = LocalDate.now().plusDays(2);
    }

    Salle newSalle() { return salles.save(Salle.builder().nom("Salle recette").centre(centre).build()); }

    Long create(Salle target, LocalTime time) {
        return service.createAtelier(new CreateAtelierRequest(target.getId(), day.atTime(time), "PUBLIC"),
                organisateur.getMatricule()).id();
    }

    List<Boolean> race(List<Supplier<?>> operations) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(operations.size());
        CountDownLatch ready = new CountDownLatch(operations.size()), start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (Supplier<?> operation : operations) futures.add(pool.submit(() -> {
                ready.countDown(); start.await();
                try { operation.get(); return true; } catch (BusinessException rejected) { return false; }
            }));
            assertThat(ready.await(15, TimeUnit.SECONDS)).isTrue(); start.countDown();
            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> future : futures) results.add(future.get(30, TimeUnit.SECONDS));
            return results;
        } finally { start.countDown(); pool.shutdownNow(); }
    }

    @Test void eightSimultaneousCreationsOnlyAllocateOneRoomSlot() throws Exception {
        List<Supplier<?>> operations = new ArrayList<>();
        for (int i = 0; i < 8; i++) operations.add(() -> create(salle, LocalTime.of(10, 0)));
        assertThat(race(operations)).containsExactlyInAnyOrder(true, false, false, false, false, false, false, false);
        assertThat(ateliers.findBySalleCentreId(centre.getId())).hasSize(1);
    }

    @Test void roomBreakAcceptsTwentyMinutesAndRejectsNineteenOnBothSides() {
        create(salle, LocalTime.of(10, 0));
        assertThatThrownBy(() -> create(salle, LocalTime.of(12, 19))).isInstanceOf(BusinessException.class);
        assertThatCode(() -> create(salle, LocalTime.of(12, 20))).doesNotThrowAnyException();
        assertThatThrownBy(() -> create(salle, LocalTime.of(7, 41))).isInstanceOf(BusinessException.class);
        assertThatCode(() -> create(salle, LocalTime.of(7, 40))).doesNotThrowAnyException();
    }

    @Test void twoMovesToSameRoomSlotOnlyOneSucceeds() throws Exception {
        Long first = create(salle, LocalTime.of(10, 0));
        Long second = create(newSalle(), LocalTime.of(10, 0));
        Salle target = newSalle();
        AtelierRequest request = AtelierRequest.builder().salleId(target.getId()).organisateurId(organisateur.getId())
                .date(day).heureDebut(LocalTime.of(15, 0)).typeAtelier(TypeAtelier.PUBLIC).build();
        assertThat(race(List.of(() -> service.updateAtelier(first, request), () -> service.updateAtelier(second, request))))
                .containsExactlyInAnyOrder(true, false);
        assertThat(ateliers.findBySalleCentreId(centre.getId()).stream()
                .filter(a -> a.getSalle().getId().equals(target.getId()))).hasSize(1);
    }

    @AfterEach void cleanupOnlyOwnFixtures() {
        if (centre != null) {
            // JPA cascades remove only this test's workshops/reservations/payments.
            for (Salle fixture : salles.findByCentreId(centre.getId())) salles.deleteById(fixture.getId());
        }
        if (organisateur != null) participants.deleteById(organisateur.getId());
        if (centre != null) centres.deleteById(centre.getId());
    }
}
