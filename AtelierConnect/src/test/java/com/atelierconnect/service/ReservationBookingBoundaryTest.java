package com.atelierconnect.service;

import com.atelierconnect.entity.*;
import com.atelierconnect.entity.enums.*;
import com.atelierconnect.exception.BusinessException;
import com.atelierconnect.repository.*;
import com.atelierconnect.service.impl.ReservationServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationBookingBoundaryTest {
    @Mock ReservationRepository reservationRepository;
    @Mock PaiementRepository paiementRepository;
    @Mock AtelierService atelierService;
    @Mock ParticipantService participantService;
    @InjectMocks ReservationServiceImpl service;
    Participant participant;
    Atelier atelier;
    Centre centre;

    @BeforeEach void fixture() {
        centre = Centre.builder().ouvertVisiteurs(true).build(); centre.setId(1L);
        participant = Participant.builder().typeParticipant(TypeParticipant.PARTENAIRE).centre(centre).solde(0.0).build();
        participant.setId(1L);
        atelier = Atelier.builder().typeAtelier(TypeAtelier.PUBLIC).statut(StatutAtelier.PLANIFIE)
                .salle(Salle.builder().centre(centre).build()).prixParParticipant(15.0).nbParticipantsActuels(0)
                .dateDebut(LocalDate.now().plusDays(2).atTime(10, 0)).build(); atelier.setId(1L);
        when(atelierService.getAtelierEntityByIdForUpdate(1L)).thenReturn(atelier);
        when(participantService.getById(1L)).thenReturn(participant);
    }

    @ParameterizedTest @EnumSource(TypeParticipant.class)
    void lastAllowedBookingDayRemainsPending(TypeParticipant category) {
        participant.setTypeParticipant(category);
        atelier.setDateDebut(LocalDate.now().plusDays(AtelierRules.delaiReservation(category)).atTime(10, 0));
        when(reservationRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(paiementRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        Reservation result = service.create(1L, 1L, 1L);
        assertThat(result.getStatut()).isEqualTo(StatutReservation.EN_ATTENTE);
        assertThat(result.getPaiement().getMontant()).isEqualTo(15);
        assertThat(atelier.getNbParticipantsActuels()).isZero();
    }

    @ParameterizedTest @EnumSource(TypeParticipant.class)
    void dayAfterBookingLimitIsRejected(TypeParticipant category) {
        participant.setTypeParticipant(category);
        atelier.setDateDebut(LocalDate.now().plusDays(AtelierRules.delaiReservation(category) + 1).atTime(10, 0));
        assertThatThrownBy(() -> service.create(1L, 1L, 1L)).isInstanceOf(BusinessException.class);
        verify(reservationRepository, never()).save(any());
    }

    @Test void abonneWithoutAttachedCentreIsRejectedWithoutNullPointer() {
        participant.setTypeParticipant(TypeParticipant.ABONNE); participant.setCentre(null);
        assertThatThrownBy(() -> service.create(1L, 1L, 1L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("rattachement");
    }
}
