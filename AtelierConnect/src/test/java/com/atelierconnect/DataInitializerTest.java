package com.atelierconnect;

import com.atelierconnect.entity.Participant;
import com.atelierconnect.entity.Centre;
import com.atelierconnect.entity.Administrateur;
import com.atelierconnect.entity.enums.TypeParticipant;
import com.atelierconnect.entity.enums.TypeAdministrateur;
import com.atelierconnect.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock private CentreRepository siteRepository;
    @Mock private SalleRepository terrainRepository;
    @Mock private ParticipantRepository membreRepository;
    @Mock private AdministrateurRepository administrateurRepository;
    @Mock private JourFermetureRepository jourFermetureRepository;
    @Mock private AtelierRepository matchRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private PaiementRepository paiementRepository;
    @Mock private PenaliteRepository penaliteRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dataInitializer, "demoMemberPassword", "Membre1234!");
        ReflectionTestUtils.setField(dataInitializer, "demoAdminPassword", "Admin1234!");
    }

    @Test
    void run_shouldBackfillLegacyMemberPasswordHashes() {
        Participant legacyMember = Participant.builder()
                .matricule("g1001")
                .nom("Martin")
                .prenom("Lucas")
                .email("lucas@email.com")
                .typeMembre(TypeParticipant.GLOBAL)
                .solde(0.0)
                .passwordHash(null)
                .build();

        Participant penalizedMember = Participant.builder()
                .matricule("L10002")
                .nom("Penalise")
                .prenom("Demo")
                .email("demo@email.com")
                .typeMembre(TypeParticipant.LIBRE)
                .solde(0.0)
                .passwordHash("existing-hash")
                .build();
        penalizedMember.setId(2L);

        when(siteRepository.count()).thenReturn(1L);
        when(matchRepository.count()).thenReturn(1L);
        when(membreRepository.findAll()).thenReturn(List.of(legacyMember, penalizedMember));
        when(passwordEncoder.encode("Membre1234!")).thenReturn("hashed-default-password");
        when(membreRepository.findByMatriculeIgnoreCase("L10002")).thenReturn(Optional.of(penalizedMember));
        when(penaliteRepository.existsByMembreIdAndDateFinAfter(eq(2L), any(LocalDate.class))).thenReturn(true);

        dataInitializer.run();

        ArgumentCaptor<List<Participant>> captor = ArgumentCaptor.forClass(List.class);
        verify(membreRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(Participant::getPasswordHash)
                .contains("hashed-default-password");
        assertThat(legacyMember.getPasswordHash()).isEqualTo("hashed-default-password");
    }

    @Test
    void run_shouldBackfillDemoAdminPasswordHashes() {
        Administrateur adminLyon = Administrateur.builder()
                .email("admin.lyon@padel.com")
                .nom("Admin")
                .prenom("Lyon")
                .typeAdministrateur(TypeAdministrateur.SITE)
                .passwordHash("old-hash")
                .build();
        adminLyon.setId(10L);

        Administrateur adminParis = Administrateur.builder()
                .email("admin.paris@padel.com")
                .nom("Admin")
                .prenom("Paris")
                .typeAdministrateur(TypeAdministrateur.SITE)
                .passwordHash("old-hash-2")
                .build();
        adminParis.setId(11L);

        when(siteRepository.count()).thenReturn(1L);
        when(matchRepository.count()).thenReturn(1L);
        when(membreRepository.findAll()).thenReturn(List.of());
        when(administrateurRepository.findByEmail("admin@padel.com")).thenReturn(Optional.empty());
        when(administrateurRepository.findByEmail("admin.lyon@padel.com")).thenReturn(Optional.of(adminLyon));
        when(administrateurRepository.findByEmail("admin.paris@padel.com")).thenReturn(Optional.of(adminParis));
        when(membreRepository.save(any(Participant.class))).thenAnswer(invocation -> {
            Participant saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });
        when(penaliteRepository.existsByMembreIdAndDateFinAfter(any(), any(LocalDate.class))).thenReturn(true);
        when(passwordEncoder.encode("Membre1234!")).thenReturn("hashed-member-password");
        when(passwordEncoder.encode("Admin1234!")).thenReturn("hashed-admin-password");

        dataInitializer.run();

        verify(administrateurRepository).save(adminLyon);
        verify(administrateurRepository).save(adminParis);
        assertThat(adminLyon.getPasswordHash()).isEqualTo("hashed-admin-password");
        assertThat(adminParis.getPasswordHash()).isEqualTo("hashed-admin-password");
    }
}
