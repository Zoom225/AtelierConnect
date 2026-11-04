package com.atelierconnect;

import com.atelierconnect.entity.Participant;
import com.atelierconnect.entity.Centre;
import com.atelierconnect.entity.Administrateur;
import com.atelierconnect.entity.Salle;
import com.atelierconnect.entity.Atelier;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock private CentreRepository centreRepository;
    @Mock private SalleRepository salleRepository;
    @Mock private ParticipantRepository participantRepository;
    @Mock private AdministrateurRepository administrateurRepository;
    @Mock private JourFermetureRepository jourFermetureRepository;
    @Mock private AtelierRepository atelierRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private PaiementRepository paiementRepository;
    @Mock private PenaliteRepository penaliteRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dataInitializer, "demoParticipantPassword", "Membre1234!");
    }

    @ParameterizedTest
    @ValueSource(strings = {"canonical", "descriptive", "legacy"})
    void run_shouldSeedExistingRoomsAcrossNamingVersions(String naming) {
        List<String> names = switch (naming) {
            case "canonical" -> List.of("Salle A", "Salle B", "Salle C", "Salle 1");
            case "descriptive" -> List.of("Salle Création A", "Salle Formation B", "Salle Collaborative C", "Salle Numérique 1");
            default -> List.of("Court A", "Court B", "Court C", "Court 1");
        };
        List<Salle> rooms = names.stream().map(name -> Salle.builder().nom(name).build()).toList();
        List<Participant> participants = List.of("G1001", "G1002", "S10001", "S10002", "L10001", "L10002")
                .stream().map(matricule -> Participant.builder().matricule(matricule)
                        .email(matricule + "@atelierconnect.example").passwordHash("existing-hash").build()).toList();
        for (int i = 0; i < participants.size(); i++) {
            participants.get(i).setId((long) i + 1);
        }
        when(centreRepository.count()).thenReturn(1L);
        when(atelierRepository.count()).thenReturn(0L);
        when(salleRepository.findAll()).thenReturn(rooms);
        when(participantRepository.findAll()).thenReturn(participants);
        when(participantRepository.findByMatriculeIgnoreCase(anyString())).thenAnswer(invocation ->
                participants.stream().filter(p -> p.getMatricule().equals(invocation.getArgument(0))).findFirst());
        when(penaliteRepository.existsByParticipantIdAndDateFinAfter(any(), any(LocalDate.class))).thenReturn(true);
        when(atelierRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        dataInitializer.run();

        ArgumentCaptor<Atelier> ateliers = ArgumentCaptor.forClass(Atelier.class);
        verify(atelierRepository, times(4)).save(ateliers.capture());
        assertThat(ateliers.getAllValues()).extracting(Atelier::getSalle)
                .containsExactly(rooms.get(0), rooms.get(1), rooms.get(3), rooms.get(2));
        verify(salleRepository, never()).save(any());
        verify(centreRepository, never()).save(any());
    }

    @Test
    void run_shouldBackfillLegacyParticipantPasswordHashes() {
        Participant legacyParticipant = Participant.builder()
                .matricule("g1001")
                .nom("Martin")
                .prenom("Lucas")
                .email("lucas@email.com")
                .typeParticipant(TypeParticipant.PARTENAIRE)
                .solde(0.0)
                .passwordHash(null)
                .build();

        Participant penalizedParticipant = Participant.builder()
                .matricule("L10002")
                .nom("Penalise")
                .prenom("Demo")
                .email("demo@email.com")
                .typeParticipant(TypeParticipant.VISITEUR)
                .solde(0.0)
                .passwordHash("existing-hash")
                .build();
        penalizedParticipant.setId(2L);

        when(centreRepository.count()).thenReturn(1L);
        when(atelierRepository.count()).thenReturn(1L);
        when(participantRepository.findAll()).thenReturn(List.of(legacyParticipant, penalizedParticipant));
        when(passwordEncoder.encode("Membre1234!")).thenReturn("hashed-default-password");
        when(participantRepository.findByMatriculeIgnoreCase("L10002")).thenReturn(Optional.of(penalizedParticipant));
        when(penaliteRepository.existsByParticipantIdAndDateFinAfter(eq(2L), any(LocalDate.class))).thenReturn(true);

        dataInitializer.run();

        ArgumentCaptor<List<Participant>> captor = ArgumentCaptor.forClass(List.class);
        verify(participantRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(Participant::getPasswordHash)
                .contains("hashed-default-password");
        assertThat(legacyParticipant.getPasswordHash()).isEqualTo("hashed-default-password");
    }

    @Test
    void run_shouldPreserveExistingDemoAdminPasswordHashes() {
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

        when(centreRepository.count()).thenReturn(1L);
        when(atelierRepository.count()).thenReturn(1L);
        when(participantRepository.findAll()).thenReturn(List.of());
        when(administrateurRepository.findByEmail("admin@padel.com")).thenReturn(Optional.empty());
        when(administrateurRepository.findByEmail("admin.lyon@padel.com")).thenReturn(Optional.of(adminLyon));
        when(administrateurRepository.findByEmail("admin.paris@padel.com")).thenReturn(Optional.of(adminParis));
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> {
            Participant saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });
        when(penaliteRepository.existsByParticipantIdAndDateFinAfter(any(), any(LocalDate.class))).thenReturn(true);
        when(passwordEncoder.encode("Membre1234!")).thenReturn("hashed-member-password");


        dataInitializer.run();

        verify(administrateurRepository, never()).save(adminLyon);
        verify(administrateurRepository, never()).save(adminParis);
        assertThat(adminLyon.getPasswordHash()).isEqualTo("old-hash");
        assertThat(adminParis.getPasswordHash()).isEqualTo("old-hash-2");
    }

    @Test
    void run_shouldUseMatriculeWhenDemoAdministratorHasNoPassword() {
        Administrateur admin = Administrateur.builder()
                .matricule("ADMIN002")
                .email("admin.bruxelles@atelierconnect.example")
                .typeAdministrateur(TypeAdministrateur.SITE)
                .build();
        Participant participant = Participant.builder().matricule("L10002").passwordHash("existing-hash").build();
        participant.setId(50L);
        when(centreRepository.count()).thenReturn(1L);
        when(atelierRepository.count()).thenReturn(1L);
        when(participantRepository.findAll()).thenReturn(List.of(participant));
        when(participantRepository.findByMatriculeIgnoreCase("L10002")).thenReturn(Optional.of(participant));
        when(penaliteRepository.existsByParticipantIdAndDateFinAfter(eq(50L), any(LocalDate.class))).thenReturn(true);
        when(administrateurRepository.findByEmail(anyString())).thenAnswer(invocation ->
                admin.getEmail().equals(invocation.getArgument(0)) ? Optional.of(admin) : Optional.empty());
        when(passwordEncoder.encode("ADMIN002")).thenReturn("encoded-admin-matricule");

        dataInitializer.run();

        assertThat(admin.getPasswordHash()).isEqualTo("encoded-admin-matricule");
        verify(administrateurRepository).save(admin);
    }

    @Test
    void run_shouldPreserveExistingParticipantCredentials() {
        Participant existing = Participant.builder()
                .matricule("L10002")
                .nom("Test")
                .prenom("Demo")
                .email("adresse.personnalisee@example.com")
                .passwordHash("mon-hachage-bcrypt-existant")
                .typeParticipant(TypeParticipant.VISITEUR)
                .solde(0.0)
                .build();
        existing.setId(50L);

        when(centreRepository.count()).thenReturn(1L);
        when(atelierRepository.count()).thenReturn(1L);
        when(participantRepository.findAll()).thenReturn(List.of(existing));
        when(participantRepository.findByMatriculeIgnoreCase("L10002")).thenReturn(Optional.of(existing));
        when(penaliteRepository.existsByParticipantIdAndDateFinAfter(eq(50L), any(LocalDate.class)))
                .thenReturn(true);

        dataInitializer.run();

        assertThat(existing.getEmail()).isEqualTo("adresse.personnalisee@example.com");
        assertThat(existing.getPasswordHash()).isEqualTo("mon-hachage-bcrypt-existant");
        verify(participantRepository, never()).save(existing);
    }
}
