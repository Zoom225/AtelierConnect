package com.atelierconnect.controller;

import com.atelierconnect.config.JwtConfig;
import com.atelierconnect.entity.*;
import com.atelierconnect.entity.enums.*;
import com.atelierconnect.repository.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class AuthorizationJwtIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JwtConfig jwt;
    @Autowired ObjectMapper json;
    @Autowired CentreRepository centres;
    @Autowired SalleRepository salles;
    @Autowired ParticipantRepository participants;
    @Autowired AdministrateurRepository admins;
    @Autowired AtelierRepository ateliers;
    @Autowired ReservationRepository reservations;
    @Autowired PaiementRepository paiements;
    @Autowired JourFermetureRepository fermetures;
    Centre a, b;
    Salle salleA, salleB;
    Participant participantA, participantB;
    Administrateur siteAdmin, globalAdmin;
    Atelier atelierA, atelierB;
    Reservation reservationA, reservationB;
    Paiement paiementB;
    String siteToken, globalToken, memberToken;
    List<Long> closureIds = new ArrayList<>();

    @BeforeEach void fixture() {
        String suffix = UUID.randomUUID().toString();
        a = newCentre("Centres homonymes " + suffix); b = newCentre(a.getNom());
        salleA = salles.save(Salle.builder().nom("Salle A").centre(a).build());
        salleB = salles.save(Salle.builder().nom("Salle B").centre(b).build());
        participantA = newParticipant("A-" + suffix, a); participantB = newParticipant("B-" + suffix, b);
        siteAdmin = admins.save(Administrateur.builder().matricule("SA-" + suffix).email("sa-" + suffix + "@example.test")
                .nom("Admin").prenom("Site").passwordHash("test-not-used-for-login")
                .typeAdministrateur(TypeAdministrateur.SITE).centre(a).build());
        globalAdmin = admins.save(Administrateur.builder().matricule("GA-" + suffix).email("ga-" + suffix + "@example.test")
                .nom("Admin").prenom("Global").passwordHash("test-not-used-for-login")
                .typeAdministrateur(TypeAdministrateur.GLOBAL).build());
        atelierA = newAtelier(salleA, participantA); atelierB = newAtelier(salleB, participantB);
        reservationA = reservations.save(Reservation.builder().atelier(atelierA).participant(participantA)
                .statut(StatutReservation.EN_ATTENTE).build());
        reservationB = reservations.save(Reservation.builder().atelier(atelierB).participant(participantB)
                .statut(StatutReservation.EN_ATTENTE).build());
        paiementB = paiements.save(Paiement.builder().reservation(reservationB).montant(15.0).statut(StatutPaiement.EN_ATTENTE).build());
        siteToken = jwt.generateToken(siteAdmin.getEmail(), "SITE", "ADMIN");
        globalToken = jwt.generateToken(globalAdmin.getEmail(), "GLOBAL", "ADMIN");
        memberToken = jwt.generateToken(participantA.getMatricule(), "PARTENAIRE", "MEMBER");
    }

    Centre newCentre(String name) { return centres.save(Centre.builder().nom(name).adresse("Recette")
            .heureOuverture(LocalTime.of(7, 0)).heureFermeture(LocalTime.of(22, 0))
            .dureeAtelierMinutes(120).dureeEntreAtelierMinutes(20).anneeCivile(LocalDate.now().getYear())
            .ouvertVisiteurs(true).build()); }

    Participant newParticipant(String key, Centre centre) { return participants.save(Participant.builder()
            .matricule(key).nom("Participant").prenom("Test").centre(centre)
            .typeParticipant(TypeParticipant.PARTENAIRE).solde(0.0).build()); }

    Atelier newAtelier(Salle salle, Participant owner) { LocalDateTime time = LocalDate.now().plusDays(3).atTime(10, 0);
        return ateliers.save(Atelier.builder().salle(salle).organisateur(owner).dateDebut(time).dateFin(time.plusMinutes(120))
                .typeAtelier(TypeAtelier.PUBLIC).statut(StatutAtelier.PLANIFIE).nbParticipantsActuels(0)
                .prixTotal(90.0).prixParParticipant(15.0).build()); }

    ResultActions call(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mvc.perform(request.header("Authorization", "Bearer " + token));
    }
    void refused(MockHttpServletRequestBuilder request, String token) throws Exception {
        call(request, token).andExpect(status().is4xxClientError());
    }

    @Test void siteListUsesIdentifiersEvenWhenCentresHaveIdenticalNames() throws Exception {
        JsonNode result = json.readTree(call(get("/api/ateliers"), siteToken).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).get("id").asLong()).isEqualTo(atelierA.getId());
        call(get("/api/participants"), siteToken).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(participantA.getId()));
    }

    @Test void siteCannotModifyForeignRoomParticipantOrCentre() throws Exception {
        refused(put("/api/salles/{id}", salleB.getId()).contentType("application/json")
                .content("{\"nom\":\"Intrusion\",\"centreId\":" + b.getId() + "}"), siteToken);
        refused(get("/api/participants/{id}", participantB.getId()), siteToken);
        refused(get("/api/ateliers/{id}", atelierB.getId()), siteToken);
        refused(get("/api/ateliers/centre/{id}", b.getId()), siteToken);
        refused(delete("/api/centres/{id}", a.getId()), siteToken);
        refused(post("/api/centres").contentType("application/json").content(json.writeValueAsString(Map.of(
                "nom", "Interdit", "adresse", "Test", "heureOuverture", "08:00", "heureFermeture", "22:00",
                "dureeAtelierMinutes", 120, "dureeEntreAtelierMinutes", 20, "anneeCivile", LocalDate.now().getYear()))), siteToken);
        assertThat(salles.findById(salleB.getId()).orElseThrow().getNom()).isEqualTo("Salle B");
        assertThat(centres.existsById(a.getId())).isTrue();
    }

    @Test void globalCanManageRoomsAcrossBothCentresAndReadParticipantsAndAteliers() throws Exception {
        for (Salle salle : List.of(salleA, salleB)) call(put("/api/salles/{id}", salle.getId())
                .contentType("application/json").content("{\"nom\":\"Renommee\",\"centreId\":" + salle.getCentre().getId() + "}"), globalToken)
                .andExpect(status().isOk()).andExpect(jsonPath("$.nom").value("Renommee"));
        call(get("/api/participants/{id}", participantB.getId()), globalToken).andExpect(status().isOk());
        call(get("/api/ateliers/{id}", atelierB.getId()), globalToken).andExpect(status().isOk());
    }

    @Test void closurePermissionsAreEnforcedThroughRealJwt() throws Exception {
        long own = createClosure(false, a.getId(), siteToken);
        long foreign = createClosure(false, b.getId(), globalToken);
        long global = createClosure(true, null, globalToken);
        JsonNode visible = json.readTree(call(get("/api/jours-fermeture"), siteToken).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        List<Long> visibleIds = new ArrayList<>(); visible.forEach(row -> visibleIds.add(row.get("id").asLong()));
        assertThat(visibleIds).contains(own, global).doesNotContain(foreign);
        call(get("/api/jours-fermeture/centre/{id}", a.getId()), siteToken).andExpect(status().isOk());
        refused(get("/api/jours-fermeture/centre/{id}", b.getId()), siteToken);
        call(get("/api/jours-fermeture"), globalToken).andExpect(status().isOk());
        refused(post("/api/jours-fermeture").contentType("application/json").content(closureBody(false, b.getId())), siteToken);
        refused(post("/api/jours-fermeture").contentType("application/json").content(closureBody(true, null)), siteToken);
        refused(delete("/api/jours-fermeture/{id}", foreign), siteToken);
        refused(delete("/api/jours-fermeture/{id}", global), siteToken);
        call(delete("/api/jours-fermeture/{id}", own), siteToken).andExpect(status().isNoContent()); closureIds.remove(own);
    }

    @Test void memberCannotReadPayOrCancelSomeoneElsesReservation() throws Exception {
        call(get("/api/reservations/{id}", reservationA.getId()), memberToken).andExpect(status().isOk());
        refused(get("/api/reservations/{id}", reservationB.getId()), memberToken);
        refused(get("/api/paiements/{id}", paiementB.getId()), memberToken);
        refused(post("/api/paiements/reservation/{id}/pay", reservationB.getId()), memberToken);
        refused(patch("/api/reservations/{id}/cancel", reservationB.getId()), memberToken);
        refused(post("/api/salles").contentType("application/json").content("{\"nom\":\"Interdit\",\"centreId\":" + a.getId() + "}"), memberToken);
        assertThat(paiements.findById(paiementB.getId()).orElseThrow().getStatut()).isEqualTo(StatutPaiement.EN_ATTENTE);
        assertThat(reservations.findById(reservationB.getId()).orElseThrow().getStatut()).isEqualTo(StatutReservation.EN_ATTENTE);
    }

    String closureBody(boolean global, Long centreId) throws Exception {
        Map<String,Object> body = new HashMap<>(); body.put("date", LocalDate.now().plusDays(40).toString());
        body.put("raison", "Recette"); body.put("global", global); body.put("centreId", centreId);
        return json.writeValueAsString(body);
    }
    long createClosure(boolean global, Long centreId, String token) throws Exception {
        long id = json.readTree(call(post("/api/jours-fermeture").contentType("application/json")
                .content(closureBody(global, centreId)), token).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong(); closureIds.add(id); return id;
    }

    @AfterEach void cleanupOwnFixtures() {
        closureIds.forEach(fermetures::deleteById);
        if (salleA != null) salles.deleteById(salleA.getId());
        if (salleB != null) salles.deleteById(salleB.getId());
        if (participantA != null) participants.deleteById(participantA.getId());
        if (participantB != null) participants.deleteById(participantB.getId());
        if (siteAdmin != null) admins.deleteById(siteAdmin.getId());
        if (globalAdmin != null) admins.deleteById(globalAdmin.getId());
        if (a != null) centres.deleteById(a.getId());
        if (b != null) centres.deleteById(b.getId());
    }
}
