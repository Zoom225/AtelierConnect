package com.atelierconnect.entity;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LegacySchemaCompatibilityTest {
    @Test
    void renamedPropertiesKeepTheirExistingPhysicalColumns() throws Exception {
        assertThat(Atelier.class.getAnnotation(Table.class).name()).isEqualTo("matches");
        assertThat(Participant.class.getAnnotation(Table.class).name()).isEqualTo("membres");
        assertThat(Salle.class.getAnnotation(Table.class).name()).isEqualTo("terrains");
        assertThat(Centre.class.getAnnotation(Table.class).name()).isEqualTo("sites");
        assertColumn(Atelier.class, "typeAtelier", "type_match");
        assertColumn(Atelier.class, "nbParticipantsActuels", "nb_joueurs_actuels");
        assertColumn(Atelier.class, "prixParParticipant", "prix_par_joueur");
        assertColumn(Participant.class, "typeParticipant", "type_membre");
        assertColumn(Centre.class, "dureeAtelierMinutes", "duree_match_minutes");
        assertColumn(Centre.class, "dureeEntreAtelierMinutes", "duree_entre_match_minutes");
        assertJoin(Atelier.class, "salle", "terrain_id");
        assertJoin(Reservation.class, "atelier", "match_id");
        assertJoin(Reservation.class, "participant", "membre_id");
        for (Class<?> entity : new Class<?>[]{Participant.class, Salle.class, Administrateur.class, JourFermeture.class}) {
            assertJoin(entity, "centre", "site_id");
        }
        assertJoin(Penalite.class, "participant", "membre_id");
    }

    private void assertColumn(Class<?> entity, String property, String column) throws Exception {
        assertThat(entity.getDeclaredField(property).getAnnotation(Column.class).name()).isEqualTo(column);
    }

    private void assertJoin(Class<?> entity, String property, String column) throws Exception {
        assertThat(entity.getDeclaredField(property).getAnnotation(JoinColumn.class).name()).isEqualTo(column);
    }
}
