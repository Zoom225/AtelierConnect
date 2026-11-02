package com.atelierconnect.entity;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhysicalSchemaNamingTest {
    @Test
    void entityMappingsUseAtelierConnectPhysicalNames() throws Exception {
        assertThat(Atelier.class.getAnnotation(Table.class).name()).isEqualTo("ateliers");
        assertThat(Participant.class.getAnnotation(Table.class).name()).isEqualTo("participants");
        assertThat(Salle.class.getAnnotation(Table.class).name()).isEqualTo("salles");
        assertThat(Centre.class.getAnnotation(Table.class).name()).isEqualTo("centres");
        assertColumn(Atelier.class, "typeAtelier", "type_atelier");
        assertColumn(Atelier.class, "nbParticipantsActuels", "nb_participants_actuels");
        assertColumn(Atelier.class, "prixParParticipant", "prix_par_participant");
        assertColumn(Participant.class, "typeParticipant", "type_participant");
        assertColumn(Centre.class, "dureeAtelierMinutes", "duree_atelier_minutes");
        assertColumn(Centre.class, "dureeEntreAtelierMinutes", "duree_entre_atelier_minutes");
        assertJoin(Atelier.class, "salle", "salle_id");
        assertJoin(Reservation.class, "atelier", "atelier_id");
        assertJoin(Reservation.class, "participant", "participant_id");
        for (Class<?> entity : new Class<?>[]{Participant.class, Salle.class, Administrateur.class, JourFermeture.class}) {
            assertJoin(entity, "centre", "centre_id");
        }
        assertJoin(Penalite.class, "participant", "participant_id");
    }

    private void assertColumn(Class<?> entity, String property, String column) throws Exception {
        assertThat(entity.getDeclaredField(property).getAnnotation(Column.class).name()).isEqualTo(column);
    }

    private void assertJoin(Class<?> entity, String property, String column) throws Exception {
        assertThat(entity.getDeclaredField(property).getAnnotation(JoinColumn.class).name()).isEqualTo(column);
    }
}
