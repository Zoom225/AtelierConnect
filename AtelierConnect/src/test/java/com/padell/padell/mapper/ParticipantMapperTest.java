package com.padell.padell.mapper;

import com.padell.padell.dto.request.ParticipantRequest;
import com.padell.padell.dto.response.ParticipantResponse;
import com.padell.padell.entity.Participant;
import com.padell.padell.entity.Centre;
import com.padell.padell.entity.enums.TypeParticipant;
import com.padell.padell.entity.enums.TypeParticipantLegacy;
import com.padell.padell.entity.enums.TypeParticipant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests de MembreMapper")
class ParticipantMapperTest {

    private ParticipantMapper membreMapper;

    @BeforeEach
    void setUp() {
        membreMapper = new ParticipantMapper();
    }

    @Test
    @DisplayName("Doit mapper MembreRequest vers Membre Entity")
    void shouldMapMembreRequestToMembreEntity() {
        // Arrange
        ParticipantRequest request = ParticipantRequest.builder()
                .matricule("L12345")
                .nom("Doe")
                .prenom("John")
                .email("john.doe@example.com")
                .typeMembre(TypeParticipantLegacy.LIBRE)
                .build();

        // Act
        Participant membre = membreMapper.toEntity(request);

        // Assert
        assertNotNull(membre);
        assertAll("Vérification du mapping de Request vers Entity",
                () -> assertEquals("L12345", membre.getMatricule()),
                () -> assertEquals("Doe", membre.getNom()),
                () -> assertEquals("John", membre.getPrenom()),
                () -> assertEquals("john.doe@example.com", membre.getEmail()),
                () -> assertEquals(TypeParticipant.LIBRE, membre.getTypeMembre()),
                () -> assertNull(membre.getSite(), "Le site ne doit pas être mappé par le mapper")
        );
    }

    @Test
    @DisplayName("Doit mapper Membre Entity vers MembreResponse")
    void shouldMapMembreEntityToMembreResponse() {
        // Arrange
        Centre site = new Centre();
        site.setId(1L);
        site.setNom("Padel Club Central");

        // Correction : Construire l'objet, puis setter l'ID
        Participant membre = Participant.builder()
                .matricule("S54321")
                .nom("Smith")
                .prenom("Jane")
                .email("jane.smith@example.com")
                .typeMembre(TypeParticipant.SITE)
                .site(site)
                .solde(25.5)
                .build();
        membre.setId(1L); // Simule l'ID généré par la base de données

        // Act
        ParticipantResponse response = membreMapper.toResponse(membre);

        // Assert
        assertNotNull(response);
        assertAll("Vérification du mapping de Entity vers Response",
                () -> assertEquals(1L, response.getId()),
                () -> assertEquals("S54321", response.getMatricule()),
                () -> assertEquals("Smith", response.getNom()),
                () -> assertEquals("Jane", response.getPrenom()),
                () -> assertEquals(TypeParticipantLegacy.SITE, response.getTypeMembre()),
                () -> assertEquals(TypeParticipant.ABONNE, response.getCategorie()),
                () -> assertEquals(1L, response.getSiteId()),
                () -> assertEquals("Padel Club Central", response.getSiteNom()),
                () -> assertEquals(25.5, response.getSolde())
        );
    }

    @Test
    void categorieAtelierConnectAccepteUnParticipantSansTypeHistorique() {
        ParticipantRequest request = ParticipantRequest.builder()
                .matricule("L12345")
                .nom("Test")
                .prenom("Visiteur")
                .categorie(TypeParticipant.VISITEUR)
                .build();

        assertTrue(request.isCategorieValide());
        assertEquals(TypeParticipant.LIBRE, membreMapper.toEntity(request).getTypeMembre());
        request.setTypeMembre(TypeParticipantLegacy.SITE);
        assertFalse(request.isCategorieValide());
    }

    @Test
    @DisplayName("Doit gérer un Site null lors du mapping vers MembreResponse")
    void shouldHandleNullSiteWhenMappingToMembreResponse() {
        // Arrange
        // Correction : Construire l'objet, puis setter l'ID
        Participant membre = Participant.builder()
                .matricule("G9876")
                .typeMembre(TypeParticipant.GLOBAL) // Un membre GLOBAL n'a pas de site
                .site(null)
                .solde(0.0) // Initialiser tous les champs non-null
                .build();
        membre.setId(2L);

        // Act
        ParticipantResponse response = membreMapper.toResponse(membre);

        // Assert
        assertNotNull(response);
        assertAll("Vérification du mapping avec un site null",
                () -> assertEquals(2L, response.getId()),
                () -> assertNull(response.getSiteId()),
                () -> assertNull(response.getSiteNom())
        );
    }
}
