package com.atelierconnect.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntitySerializationTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void credentialsAreNeverExposedByEntities() throws Exception {
        String marker = "serialization-test-marker";
        Participant participant = Participant.builder().passwordHash(marker).build();
        Administrateur administrateur = Administrateur.builder().passwordHash(marker).build();

        for (Object entity : new Object[]{participant, administrateur}) {
            String json = mapper.writeValueAsString(entity);
            assertThat(json).doesNotContain("passwordHash", marker);
        }
    }

    @Test
    void renamedReferencesRoundTripWithoutCyclesAndRestoreTheirOwners() throws Exception {
        Centre centre = Centre.builder().nom("Centre de test").build();
        Salle salle = Salle.builder().nom("Salle de test").centre(centre).build();
        Atelier atelier = Atelier.builder().salle(salle).build();
        Reservation reservation = Reservation.builder().atelier(atelier).build();
        Paiement paiement = Paiement.builder().reservation(reservation).build();
        JourFermeture fermeture = JourFermeture.builder().centre(centre).build();
        centre.getSalles().add(salle);
        centre.getJoursFermeture().add(fermeture);
        salle.getAteliers().add(atelier);
        atelier.getReservations().add(reservation);
        reservation.setPaiement(paiement);

        Centre restored = mapper.readValue(mapper.writeValueAsString(centre), Centre.class);
        Salle restoredSalle = restored.getSalles().iterator().next();
        Atelier restoredAtelier = restoredSalle.getAteliers().getFirst();
        Reservation restoredReservation = restoredAtelier.getReservations().getFirst();

        assertThat(restoredSalle.getCentre()).isSameAs(restored);
        assertThat(restored.getJoursFermeture().iterator().next().getCentre()).isSameAs(restored);
        assertThat(restoredAtelier.getSalle()).isSameAs(restoredSalle);
        assertThat(restoredReservation.getAtelier()).isSameAs(restoredAtelier);
        assertThat(restoredReservation.getPaiement().getReservation()).isSameAs(restoredReservation);
    }

    @Test
    void newCollectionsAreMutableAndBelongToEachInstance() {
        Atelier first = Atelier.builder().build();
        Atelier second = new Atelier();
        Salle firstSalle = Salle.builder().build();
        Salle secondSalle = new Salle();

        first.getReservations().add(new Reservation());
        firstSalle.getAteliers().add(first);

        assertThat(second.getReservations()).isEmpty();
        assertThat(secondSalle.getAteliers()).isEmpty();
    }
}
