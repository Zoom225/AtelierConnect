package com.atelierconnect.mapper;

import com.atelierconnect.dto.request.ParticipantRequest;
import com.atelierconnect.dto.response.ParticipantResponse;
import com.atelierconnect.entity.Participant;
import com.atelierconnect.entity.Centre;
import com.atelierconnect.entity.enums.TypeParticipant;
import com.atelierconnect.entity.enums.TypeParticipantLegacy;
import com.atelierconnect.entity.enums.TypeParticipant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests de MembreMapper")
class ParticipantMapperTest {

    private ParticipantMapper participantMapper;

    @BeforeEach
    void setUp() {
        participantMapper = new ParticipantMapper();
    }

    @Test
    @DisplayName("Doit mapper MembreRequest vers Participant Entity")
    void shouldMapParticipantRequestToParticipantEntity() {
        // Arrange
        ParticipantRequest request = ParticipantRequest.builder()
                .matricule("L12345")
                .nom("Doe")
                .prenom("John")
                .email("john.doe@example.com")
                .typeParticipant(TypeParticipantLegacy.LIBRE)
                .build();

        // Act
        Participant participant = participantMapper.toEntity(request);

        // Assert
        assertNotNull(participant);
        assertAll("Vérification du mapping de Request vers Entity",
                () -> assertEquals("L12345", participant.getMatricule()),
                () -> assertEquals("Doe", participant.getNom()),
                () -> assertEquals("John", participant.getPrenom()),
                () -> assertEquals("john.doe@example.com", participant.getEmail()),
                () -> assertEquals(TypeParticipant.VISITEUR, participant.getTypeParticipant()),
                () -> assertNull(participant.getCentre(), "Le centre ne doit pas être mappé par le mapper")
        );
    }

    @Test
    @DisplayName("Doit mapper Participant Entity vers MembreResponse")
    void shouldMapParticipantEntityToParticipantResponse() {
        // Arrange
        Centre centre = new Centre();
        centre.setId(1L);
        centre.setNom("Centre collaboratif Central");

        // Correction : Construire l'objet, puis setter l'ID
        Participant participant = Participant.builder()
                .matricule("S54321")
                .nom("Smith")
                .prenom("Jane")
                .email("jane.smith@example.com")
                .typeParticipant(TypeParticipant.ABONNE)
                .centre(centre)
                .solde(25.5)
                .build();
        participant.setId(1L); // Simule l'ID généré par la base de données

        // Act
        ParticipantResponse response = participantMapper.toResponse(participant);

        // Assert
        assertNotNull(response);
        assertAll("Vérification du mapping de Entity vers Response",
                () -> assertEquals(1L, response.getId()),
                () -> assertEquals("S54321", response.getMatricule()),
                () -> assertEquals("Smith", response.getNom()),
                () -> assertEquals("Jane", response.getPrenom()),
                () -> assertEquals(TypeParticipantLegacy.SITE, response.getTypeParticipant()),
                () -> assertEquals(TypeParticipant.ABONNE, response.getCategorie()),
                () -> assertEquals(1L, response.getCentreId()),
                () -> assertEquals("Centre collaboratif Central", response.getCentreNom()),
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
        assertEquals(TypeParticipant.VISITEUR, participantMapper.toEntity(request).getTypeParticipant());
        request.setTypeParticipant(TypeParticipantLegacy.SITE);
        assertFalse(request.isCategorieValide());
    }

    @Test
    @DisplayName("Doit gérer un Centre null lors du mapping vers MembreResponse")
    void shouldHandleNullCentreWhenMappingToParticipantResponse() {
        // Arrange
        // Correction : Construire l'objet, puis setter l'ID
        Participant participant = Participant.builder()
                .matricule("G9876")
                .typeParticipant(TypeParticipant.PARTENAIRE) // Un participant GLOBAL n'a pas de centre
                .centre(null)
                .solde(0.0) // Initialiser tous les champs non-null
                .build();
        participant.setId(2L);

        // Act
        ParticipantResponse response = participantMapper.toResponse(participant);

        // Assert
        assertNotNull(response);
        assertAll("Vérification du mapping avec un centre null",
                () -> assertEquals(2L, response.getId()),
                () -> assertNull(response.getCentreId()),
                () -> assertNull(response.getCentreNom())
        );
    }
}
