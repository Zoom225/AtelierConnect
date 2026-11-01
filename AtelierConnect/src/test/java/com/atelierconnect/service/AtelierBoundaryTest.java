package com.atelierconnect.service;

import com.atelierconnect.dto.request.CreateAtelierRequest;
import com.atelierconnect.entity.*;
import com.atelierconnect.entity.enums.*;
import com.atelierconnect.exception.BusinessException;
import com.atelierconnect.mapper.AtelierMapper;
import com.atelierconnect.repository.*;
import com.atelierconnect.service.impl.AtelierServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtelierBoundaryTest {
    @Mock AtelierRepository atelierRepository;
    @Mock ParticipantRepository participantRepository;
    @Mock ReservationRepository reservationRepository;
    @Mock PaiementRepository paiementRepository;
    @Mock JourFermetureRepository jourFermetureRepository;
    @Mock SalleService salleService;
    @Mock ParticipantService participantService;
    @Mock AtelierMapper atelierMapper;
    @Mock EntityManager entityManager;
    @InjectMocks AtelierServiceImpl service;
    Centre centre;
    Salle salle;
    Participant participant;

    @BeforeEach void fixture() {
        centre = Centre.builder().heureOuverture(LocalTime.of(7, 0))
                .heureFermeture(LocalTime.of(23, 0)).ouvertVisiteurs(true).build();
        centre.setId(1L);
        salle = Salle.builder().centre(centre).build(); salle.setId(1L);
        participant = Participant.builder().matricule("G1234").centre(centre)
                .typeParticipant(TypeParticipant.PARTENAIRE).solde(0.0).build(); participant.setId(1L);
    }

    void creationFixture() {
        when(participantRepository.findByMatriculeIgnoreCase("G1234")).thenReturn(Optional.of(participant));
        when(salleService.getByIdForUpdate(1L)).thenReturn(salle);
    }

    void saves() {
        when(atelierRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(reservationRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(paiementRepository.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    void create(int days) {
        service.createAtelier(new CreateAtelierRequest(1L, LocalDate.now().plusDays(days).atTime(10, 0), "PUBLIC"), "G1234");
    }

    @ParameterizedTest @EnumSource(TypeParticipant.class)
    void acceptsLastAllowedDayWithCanonicalPricesAndPendingPlace(TypeParticipant category) {
        participant.setTypeParticipant(category); creationFixture(); saves();
        create(AtelierRules.delaiReservation(category));
        ArgumentCaptor<Atelier> saved = ArgumentCaptor.forClass(Atelier.class);
        verify(atelierRepository).save(saved.capture());
        assertThat(Duration.between(saved.getValue().getDateDebut(), saved.getValue().getDateFin()).toMinutes()).isEqualTo(120);
        assertThat(saved.getValue().getPrixTotal()).isEqualTo(90);
        assertThat(saved.getValue().getPrixParParticipant()).isEqualTo(15);
        assertThat(saved.getValue().getNbParticipantsActuels()).isZero();
        verify(reservationRepository).save(argThat(r -> r.getStatut() == StatutReservation.EN_ATTENTE));
        verify(paiementRepository).save(argThat(p -> p.getStatut() == StatutPaiement.EN_ATTENTE && p.getMontant() == 15));
    }

    @ParameterizedTest @EnumSource(TypeParticipant.class)
    void refusesDayAfterCategoryLimit(TypeParticipant category) {
        participant.setTypeParticipant(category); creationFixture();
        assertThatThrownBy(() -> create(AtelierRules.delaiReservation(category) + 1)).isInstanceOf(BusinessException.class);
        verify(atelierRepository, never()).save(any());
    }

    @Test void refusesPastDate() {
        creationFixture();
        assertThatThrownBy(() -> create(-1)).isInstanceOf(BusinessException.class);
    }

    @Test void partenaireCanOrganizeOutsideItsCentre() {
        creationFixture(); saves();
        Centre other = new Centre(); other.setId(2L); participant.setCentre(other);
        assertThatCode(() -> create(30)).doesNotThrowAnyException();
    }

    @Test void abonneCannotOrganizeOutsideItsCentre() {
        creationFixture(); participant.setTypeParticipant(TypeParticipant.ABONNE);
        Centre other = new Centre(); other.setId(2L); participant.setCentre(other);
        assertThatThrownBy(() -> create(15)).isInstanceOf(BusinessException.class).hasMessageContaining("rattachement");
    }

    @Test void visiteurCannotUseCentreClosedToVisitors() {
        creationFixture(); participant.setTypeParticipant(TypeParticipant.VISITEUR); centre.setOuvertVisiteurs(false);
        assertThatThrownBy(() -> create(7)).isInstanceOf(BusinessException.class).hasMessageContaining("visiteurs");
    }

    @Test void refusesWorkshopEndingAfterMidnightInDaytimeCentre() {
        creationFixture();
        assertThatThrownBy(() -> service.createAtelier(new CreateAtelierRequest(1L,
                LocalDate.now().plusDays(2).atTime(22, 30), "PUBLIC"), "G1234"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("horaires");
    }

    @Test void booleanConsentWithoutTimestampDoesNotAuthorizeOpening() {
        Atelier atelier = Atelier.builder().typeAtelier(TypeAtelier.PRIVE).statut(StatutAtelier.PLANIFIE)
                .dateDebut(LocalDateTime.now().plusDays(2)).accordOuverturePublique(true).nbParticipantsActuels(1).build();
        when(atelierRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(atelier));
        assertThatThrownBy(() -> service.convertToPublic(9L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("accord explicite");
        verify(atelierRepository, never()).save(any());
    }
}
