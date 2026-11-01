package com.atelierconnect.mapper;

import com.atelierconnect.entity.Atelier;
import com.atelierconnect.entity.Participant;
import com.atelierconnect.entity.Centre;
import com.atelierconnect.entity.Salle;
import com.atelierconnect.entity.enums.StatutAtelier;
import com.atelierconnect.entity.enums.TypeAtelier;
import com.atelierconnect.dto.response.AtelierDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests de MatchMapper")
class AtelierMapperTest {

    private AtelierMapper atelierMapper;

    private Atelier atelier;
    private Participant organisateur;
    private Salle salle;
    private Centre centre;

    private static final LocalDateTime FIXED_DATE = LocalDateTime.of(2024, 5, 20, 14, 0);

    @BeforeEach
    void setUp() {
        atelierMapper = new AtelierMapper();

        centre = new Centre();
        centre.setId(1L);
        centre.setNom("Test Centre");
        centre.setDureeAtelierMinutes(90);
        centre.setHeureOuverture(LocalTime.of(9, 0));
        centre.setHeureFermeture(LocalTime.of(22, 0));

        organisateur = new Participant();
        organisateur.setId(1L);
        organisateur.setPrenom("John");
        organisateur.setNom("Doe");

        salle = new Salle();
        salle.setId(1L);
        salle.setNom("Court Central");
        salle.setPrix(20.0);
        salle.setCentre(centre);

        atelier = new Atelier();
        atelier.setId(1L);
        atelier.setSalle(salle);
        atelier.setOrganisateur(organisateur);
        atelier.setDateDebut(FIXED_DATE);
        atelier.setDateFin(FIXED_DATE.plusMinutes(centre.getDureeAtelierMinutes()));
        atelier.setTypeAtelier(TypeAtelier.PUBLIC);
        atelier.setStatut(StatutAtelier.PLANIFIE);
        atelier.setNbParticipantsActuels(1);
        atelier.setPrixParParticipant(5.0);
    }

    @Test
    @DisplayName("toMatchDto - Doit mapper correctement Atelier vers MatchDto")
    void toAtelierDto_shouldMapAtelierToAtelierDto() {
        AtelierDto resultDto = atelierMapper.toAtelierDto(atelier);

        assertNotNull(resultDto);
        assertAll("Vérification des propriétés de MatchDto",
                () -> assertEquals(atelier.getId(), resultDto.id()),
                () -> assertEquals(atelier.getSalle().getId(), resultDto.salleId()),
                () -> assertEquals(atelier.getSalle().getNom(), resultDto.salleNom()),
                () -> assertEquals(centre.getNom(), resultDto.centreNom()),
                () -> assertEquals(atelier.getOrganisateur().getId(), resultDto.organisateurId()),
                () -> assertEquals("John Doe", resultDto.organisateurNom(), "Le nom de l'organisateur doit être concaténé"),
                () -> assertEquals(FIXED_DATE.toLocalDate(), resultDto.date()),
                () -> assertEquals(FIXED_DATE.toLocalTime(), resultDto.heureDebut()),
                () -> assertEquals(FIXED_DATE.plusMinutes(90).toLocalTime(), resultDto.heureFin()),
                () -> assertEquals(atelier.getTypeAtelier(), resultDto.typeAtelier()),
                () -> assertEquals(atelier.getStatut(), resultDto.statut()),
                () -> assertEquals(1, resultDto.nbParticipantsActuels(), "Le nombre de participants doit être 1"),
                () -> assertEquals(atelier.getPrixParParticipant(), resultDto.prixParParticipant()),
                () -> assertNull(resultDto.dateConversionPublic())
        );
    }

    @Test
    @DisplayName("Enregistre et expose le consentement daté à l'ouverture publique")
    void mapsConsentAndTimestamp() {
        LocalDateTime moment = LocalDateTime.of(2026, 10, 31, 12, 0);
        atelier.setAccordOuverturePublique(true);
        atelier.setDateAccordOuverturePublique(moment);

        AtelierDto dto = atelierMapper.toAtelierDto(atelier);

        assertTrue(dto.accordOuverturePublique());
        assertEquals(moment, dto.dateAccordOuverturePublique());
    }

    @Test
    @DisplayName("toMatchDto - Doit retourner null si le Atelier est null")
    void toAtelierDto_shouldReturnNull_whenAtelierIsNull() {
        AtelierDto resultDto = atelierMapper.toAtelierDto(null);

        assertNull(resultDto, "Le DTO résultant doit être null quand l'entité source est null");
    }

    @Test
    @DisplayName("toMatchDto - Doit gérer les propriétés nulles sans erreur")
    void toAtelierDto_shouldHandleNullProperties() {
        Atelier atelierWithNulls = new Atelier();
        atelierWithNulls.setId(2L);
        atelierWithNulls.setSalle(null);
        atelierWithNulls.setOrganisateur(null);
        atelierWithNulls.setDateDebut(FIXED_DATE);
        atelierWithNulls.setDateFin(FIXED_DATE.plusMinutes(90));
        atelierWithNulls.setNbParticipantsActuels(0);

        AtelierDto resultDto = atelierMapper.toAtelierDto(atelierWithNulls);

        assertNotNull(resultDto);
        assertAll("Vérification du mapping avec des propriétés nulles",
                () -> assertEquals(2L, resultDto.id()),
                () -> assertNull(resultDto.salleId(), "L'ID de la salle doit être null"),
                () -> assertNull(resultDto.salleNom(), "Le nom de la salle doit être null"),
                () -> assertNull(resultDto.centreNom(), "Le nom du centre doit être null"),
                () -> assertNull(resultDto.organisateurId(), "L'ID de l'organisateur doit être null"),
                () -> assertNull(resultDto.organisateurNom(), "Le nom de l'organisateur doit être null"),
                () -> assertEquals(0, resultDto.nbParticipantsActuels(), "Le nombre de participants doit être 0"),
                () -> assertEquals(FIXED_DATE.toLocalDate(), resultDto.date()),
                () -> assertEquals(FIXED_DATE.toLocalTime(), resultDto.heureDebut())
        );
    }
}
