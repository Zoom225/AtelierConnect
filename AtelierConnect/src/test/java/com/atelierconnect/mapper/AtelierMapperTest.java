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

    private AtelierMapper matchMapper;

    private Atelier match;
    private Participant organisateur;
    private Salle terrain;
    private Centre site;

    private static final LocalDateTime FIXED_DATE = LocalDateTime.of(2024, 5, 20, 14, 0);

    @BeforeEach
    void setUp() {
        matchMapper = new AtelierMapper();

        site = new Centre();
        site.setId(1L);
        site.setNom("Test Site");
        site.setDureeMatchMinutes(90);
        site.setHeureOuverture(LocalTime.of(9, 0));
        site.setHeureFermeture(LocalTime.of(22, 0));

        organisateur = new Participant();
        organisateur.setId(1L);
        organisateur.setPrenom("John");
        organisateur.setNom("Doe");

        terrain = new Salle();
        terrain.setId(1L);
        terrain.setNom("Court Central");
        terrain.setPrix(20.0);
        terrain.setSite(site);

        match = new Atelier();
        match.setId(1L);
        match.setTerrain(terrain);
        match.setOrganisateur(organisateur);
        match.setDateDebut(FIXED_DATE);
        match.setDateFin(FIXED_DATE.plusMinutes(site.getDureeMatchMinutes()));
        match.setTypeMatch(TypeAtelier.PUBLIC);
        match.setStatut(StatutAtelier.PLANIFIE);
        match.setNbJoueursActuels(1);
        match.setPrixParJoueur(5.0);
    }

    @Test
    @DisplayName("toMatchDto - Doit mapper correctement Match vers MatchDto")
    void toMatchDto_shouldMapMatchToMatchDto() {
        AtelierDto resultDto = matchMapper.toAtelierDto(match);

        assertNotNull(resultDto);
        assertAll("Vérification des propriétés de MatchDto",
                () -> assertEquals(match.getId(), resultDto.id()),
                () -> assertEquals(match.getTerrain().getId(), resultDto.terrainId()),
                () -> assertEquals(match.getTerrain().getNom(), resultDto.terrainNom()),
                () -> assertEquals(site.getNom(), resultDto.siteNom()),
                () -> assertEquals(match.getOrganisateur().getId(), resultDto.organisateurId()),
                () -> assertEquals("John Doe", resultDto.organisateurNom(), "Le nom de l'organisateur doit être concaténé"),
                () -> assertEquals(FIXED_DATE.toLocalDate(), resultDto.date()),
                () -> assertEquals(FIXED_DATE.toLocalTime(), resultDto.heureDebut()),
                () -> assertEquals(FIXED_DATE.plusMinutes(90).toLocalTime(), resultDto.heureFin()),
                () -> assertEquals(match.getTypeMatch(), resultDto.typeMatch()),
                () -> assertEquals(match.getStatut(), resultDto.statut()),
                () -> assertEquals(1, resultDto.nbJoueursActuels(), "Le nombre de joueurs doit être 1"),
                () -> assertEquals(match.getPrixParJoueur(), resultDto.prixParJoueur()),
                () -> assertNull(resultDto.dateConversionPublic())
        );
    }

    @Test
    @DisplayName("Enregistre et expose le consentement daté à l'ouverture publique")
    void mapsConsentAndTimestamp() {
        LocalDateTime moment = LocalDateTime.of(2026, 10, 31, 12, 0);
        match.setAccordOuverturePublique(true);
        match.setDateAccordOuverturePublique(moment);

        AtelierDto dto = matchMapper.toAtelierDto(match);

        assertTrue(dto.accordOuverturePublique());
        assertEquals(moment, dto.dateAccordOuverturePublique());
    }

    @Test
    @DisplayName("toMatchDto - Doit retourner null si le Match est null")
    void toMatchDto_shouldReturnNull_whenMatchIsNull() {
        AtelierDto resultDto = matchMapper.toAtelierDto(null);

        assertNull(resultDto, "Le DTO résultant doit être null quand l'entité source est null");
    }

    @Test
    @DisplayName("toMatchDto - Doit gérer les propriétés nulles sans erreur")
    void toMatchDto_shouldHandleNullProperties() {
        Atelier matchWithNulls = new Atelier();
        matchWithNulls.setId(2L);
        matchWithNulls.setTerrain(null);
        matchWithNulls.setOrganisateur(null);
        matchWithNulls.setDateDebut(FIXED_DATE);
        matchWithNulls.setDateFin(FIXED_DATE.plusMinutes(90));
        matchWithNulls.setNbJoueursActuels(0);

        AtelierDto resultDto = matchMapper.toAtelierDto(matchWithNulls);

        assertNotNull(resultDto);
        assertAll("Vérification du mapping avec des propriétés nulles",
                () -> assertEquals(2L, resultDto.id()),
                () -> assertNull(resultDto.terrainId(), "L'ID du terrain doit être null"),
                () -> assertNull(resultDto.terrainNom(), "Le nom du terrain doit être null"),
                () -> assertNull(resultDto.siteNom(), "Le nom du site doit être null"),
                () -> assertNull(resultDto.organisateurId(), "L'ID de l'organisateur doit être null"),
                () -> assertNull(resultDto.organisateurNom(), "Le nom de l'organisateur doit être null"),
                () -> assertEquals(0, resultDto.nbJoueursActuels(), "Le nombre de joueurs doit être 0"),
                () -> assertEquals(FIXED_DATE.toLocalDate(), resultDto.date()),
                () -> assertEquals(FIXED_DATE.toLocalTime(), resultDto.heureDebut())
        );
    }
}
