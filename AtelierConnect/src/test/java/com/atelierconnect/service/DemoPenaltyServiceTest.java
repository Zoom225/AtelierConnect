package com.atelierconnect.service;

import com.atelierconnect.entity.*;
import com.atelierconnect.entity.enums.TypeParticipant;
import com.atelierconnect.repository.*;
import com.atelierconnect.service.impl.DemoPenaltyService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemoPenaltyServiceTest {
    @Mock ParticipantRepository participants;
    @Mock PenaliteRepository penalites;
    @Mock MouvementDetteRepository mouvements;
    @Mock EntityManager entityManager;
    @InjectMocks DemoPenaltyService service;
    private Participant demo;

    @BeforeEach
    void setUp() {
        demo = Participant.builder().matricule("L10002").email("demo.penalise@gmail.com")
                .typeParticipant(TypeParticipant.VISITEUR).solde(0.0).build();
        demo.setId(42L);
    }

    private void arrangeAccount() {
        when(participants.findById(42L)).thenReturn(Optional.of(demo));
    }

    @Test
    void restoresPaidDebtAndCreatesActiveSuspension() {
        arrangeAccount();
        assertThat(service.prepare(demo)).isSameAs(demo);
        assertThat(demo.getSolde()).isEqualTo(90.0);
        ArgumentCaptor<MouvementDette> movement = ArgumentCaptor.forClass(MouvementDette.class);
        verify(mouvements).save(movement.capture());
        assertThat(movement.getValue().getMontant()).isEqualByComparingTo("90.00");
        assertThat(movement.getValue().getNature()).isEqualTo(MouvementDette.Nature.SOLDE_INITIAL);
        ArgumentCaptor<Penalite> penalty = ArgumentCaptor.forClass(Penalite.class);
        verify(penalites).save(penalty.capture());
        assertThat(penalty.getValue().getDateFin()).isEqualTo(LocalDate.now().plusDays(7));
        InOrder order = inOrder(participants, entityManager, mouvements);
        order.verify(participants).lockParticipantById(42L);
        order.verify(participants).findById(42L);
        order.verify(entityManager).refresh(demo);
        order.verify(mouvements).save(any());
    }

    @Test
    void repeatedLoginDoesNotAccumulateDebtOrSuspensions() {
        arrangeAccount();
        Penalite existing = Penalite.builder().participant(demo).dateFin(LocalDate.now().minusDays(2)).build();
        when(penalites.findFirstByParticipantIdAndMotif(eq(42L), anyString())).thenReturn(Optional.of(existing));
        service.prepare(demo);
        service.prepare(demo);
        assertThat(demo.getSolde()).isEqualTo(90.0);
        verify(mouvements, times(1)).save(any());
        verify(penalites, times(2)).save(existing);
        assertThat(existing.getDateFin()).isEqualTo(LocalDate.now().plusDays(7));
    }

    @Test
    void onlyAddsMissingAmountAndPreservesHistory() {
        arrangeAccount();
        demo.setSolde(30.0);
        service.prepare(demo);
        ArgumentCaptor<MouvementDette> movement = ArgumentCaptor.forClass(MouvementDette.class);
        verify(mouvements).save(movement.capture());
        assertThat(movement.getValue().getMontant()).isEqualByComparingTo("60");
        verify(mouvements, never()).deleteAll();
    }

    @Test
    void preservesLargerExistingDebt() {
        arrangeAccount();
        demo.setSolde(120.0);
        service.prepare(demo);
        assertThat(demo.getSolde()).isEqualTo(120.0);
        verifyNoInteractions(mouvements);
        verify(penalites).save(any());
    }

    @Test
    void canDisableAutomaticScenario() {
        ReflectionTestUtils.setField(service, "enabled", false);
        assertThat(service.prepare(demo)).isSameAs(demo);
        verifyNoInteractions(participants, penalites, mouvements, entityManager);
    }

    @Test
    void leavesOtherAccountsUntouched() {
        demo.setMatricule("L10001");
        service.prepare(demo);
        verifyNoInteractions(participants, penalites, mouvements, entityManager);
    }

    @Test
    void requiresDemoEmailAndVisitorType() {
        demo.setEmail("other@example.com");
        service.prepare(demo);
        demo.setEmail("demo.penalise@gmail.com");
        demo.setTypeParticipant(TypeParticipant.PARTENAIRE);
        service.prepare(demo);
        verifyNoInteractions(participants, penalites, mouvements, entityManager);
    }
}
