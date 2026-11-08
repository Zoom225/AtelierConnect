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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @Mock private MouvementDetteRepository mouvementDetteRepository;

    @InjectMocks
    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dataInitializer, "demoParticipantPassword", "Membre1234!");
    }

    @Test
    void run_shouldCreateOnlyThePenalizedDemoParticipantWithSixUnpaidShares() {
        when(passwordEncoder.encode(anyString())).thenReturn("demo-hash");
        when(centreRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(salleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(atelierRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        dataInitializer.run();

        ArgumentCaptor<Participant> saved = ArgumentCaptor.forClass(Participant.class);
        verify(participantRepository, times(6)).save(saved.capture());
        assertThat(saved.getAllValues()).filteredOn(p -> "L10002".equals(p.getMatricule()))
                .singleElement().extracting(Participant::getSolde).isEqualTo(90.0);
        assertThat(saved.getAllValues()).filteredOn(p -> !"L10002".equals(p.getMatricule()))
                .extracting(Participant::getSolde).containsOnly(0.0);
        ArgumentCaptor<Administrateur> admins = ArgumentCaptor.forClass(Administrateur.class);
        verify(administrateurRepository, times(3)).save(admins.capture());
        assertThat(admins.getAllValues()).extracting(Administrateur::getMatricule, Administrateur::getEmail)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("ADMIN001", "admin@atelierconnect.example"),
                        org.assertj.core.groups.Tuple.tuple("ADMIN002", "admin.bruxelles@atelierconnect.example"),
                        org.assertj.core.groups.Tuple.tuple("ADMIN003", "admin.namur@atelierconnect.example"));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 15.0, 42.0, 90.0})
    void run_shouldNeverRecreateOrOverwriteAnExistingDemoDebt(double existingBalance) {
        Participant participant = Participant.builder().matricule("L10002")
                .email("demo.penalise@gmail.com").passwordHash("existing-hash")
                .typeParticipant(TypeParticipant.VISITEUR).solde(existingBalance).build();
        participant.setId(23L);
        when(centreRepository.count()).thenReturn(1L);
        when(atelierRepository.count()).thenReturn(1L);
        when(participantRepository.findAll()).thenReturn(List.of(participant));
        when(participantRepository.findByMatriculeIgnoreCase("L10002")).thenReturn(Optional.of(participant));
        when(penaliteRepository.existsByParticipantId(eq(23L))).thenReturn(true);

        dataInitializer.run();
        dataInitializer.run();

        assertThat(participant.getSolde()).isEqualTo(existingBalance);
        verify(participantRepository, never()).save(any());
    }

    @Test
    void run_shouldNotRenewAnExpiredDemonstrationSuspension() {
        Participant participant = Participant.builder().matricule("L10002").solde(0.0)
                .email("demo.penalise@gmail.com").passwordHash("existing-hash")
                .typeParticipant(TypeParticipant.VISITEUR).build();
        participant.setId(99L);
        when(centreRepository.count()).thenReturn(1L);
        when(atelierRepository.count()).thenReturn(1L);
        when(participantRepository.findAll()).thenReturn(List.of(participant));
        when(participantRepository.findByMatriculeIgnoreCase("L10002")).thenReturn(Optional.of(participant));
        // L'historique existe, mais la recherche de pénalité active retournerait false.
        when(penaliteRepository.existsByParticipantId(99L)).thenReturn(true);
        dataInitializer.run();
        dataInitializer.run();
        verify(penaliteRepository, never()).save(any());
        verify(participantRepository, never()).save(any());
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
        when(penaliteRepository.existsByParticipantId(any())).thenReturn(true);
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
        when(penaliteRepository.existsByParticipantId(eq(2L))).thenReturn(true);

        dataInitializer.run();

        ArgumentCaptor<List<Participant>> captor = ArgumentCaptor.forClass(List.class);
        verify(participantRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(Participant::getPasswordHash)
                .contains("hashed-default-password");
        assertThat(legacyParticipant.getPasswordHash()).isEqualTo("hashed-default-password");
    }

    @Test
    void run_shouldUpdateLegacyDemoAdminEmailsAndPreservePasswordHashes() {
        Administrateur adminLyon = Administrateur.builder()
                .matricule("ADMIN002")
                .email("admin.lyon@padel.com")
                .nom("Admin")
                .prenom("Lyon")
                .typeAdministrateur(TypeAdministrateur.SITE)
                .passwordHash("old-hash")
                .build();
        adminLyon.setId(10L);

        Administrateur adminParis = Administrateur.builder()
                .matricule("ADMIN003")
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
        when(administrateurRepository.findByMatricule(anyString())).thenAnswer(invocation ->
                List.of(adminLyon, adminParis).stream()
                        .filter(admin -> admin.getMatricule().equals(invocation.getArgument(0))).findFirst());
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> {
            Participant saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });
        when(penaliteRepository.existsByParticipantId(any())).thenReturn(true);
        when(passwordEncoder.encode("Membre1234!")).thenReturn("hashed-member-password");


        dataInitializer.run();

        verify(administrateurRepository).save(adminLyon);
        verify(administrateurRepository).save(adminParis);
        assertThat(adminLyon.getEmail()).isEqualTo("admin.bruxelles@atelierconnect.example");
        assertThat(adminParis.getEmail()).isEqualTo("admin.namur@atelierconnect.example");
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
        when(penaliteRepository.existsByParticipantId(eq(50L))).thenReturn(true);
        when(administrateurRepository.findByMatricule(anyString())).thenAnswer(invocation ->
                admin.getMatricule().equals(invocation.getArgument(0)) ? Optional.of(admin) : Optional.empty());
        when(passwordEncoder.encode("ADMIN002")).thenReturn("encoded-admin-matricule");

        dataInitializer.run();

        assertThat(admin.getPasswordHash()).isEqualTo("encoded-admin-matricule");
        verify(administrateurRepository).save(admin);
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin@atelierconnect.example", "direction@centre.example"})
    void run_shouldPreserveCurrentOrCustomizedAdminCredentials(String email) {
        Administrateur admin = Administrateur.builder().matricule("ADMIN001").email(email)
                .passwordHash("custom-hash").typeAdministrateur(TypeAdministrateur.GLOBAL).build();
        admin.setId(1L);
        existingDatabaseWithAdmin(admin);

        dataInitializer.run();

        assertThat(admin.getEmail()).isEqualTo(email);
        assertThat(admin.getPasswordHash()).isEqualTo("custom-hash");
        verify(administrateurRepository, never()).save(admin);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void run_shouldMigrateGlobalAdminEmailOnlyOnce() {
        Administrateur admin = Administrateur.builder().matricule("ADMIN001").email("admin@padel.com")
                .passwordHash("existing-hash").typeAdministrateur(TypeAdministrateur.GLOBAL).build();
        admin.setId(1L);
        existingDatabaseWithAdmin(admin);

        dataInitializer.run();
        dataInitializer.run();

        assertThat(admin.getEmail()).isEqualTo("admin@atelierconnect.example");
        assertThat(admin.getPasswordHash()).isEqualTo("existing-hash");
        verify(administrateurRepository, times(1)).save(admin);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void run_shouldRefuseAnEmailAlreadyUsedByAnotherAdmin() {
        Administrateur admin = Administrateur.builder().matricule("ADMIN001").email("admin@padel.com")
                .passwordHash("existing-hash").typeAdministrateur(TypeAdministrateur.GLOBAL).build();
        admin.setId(1L);
        Administrateur other = Administrateur.builder().email("admin@atelierconnect.example").build();
        other.setId(2L);
        existingDatabaseWithAdmin(admin);
        when(administrateurRepository.findByEmail("admin@atelierconnect.example")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> dataInitializer.run()).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("déjà utilisée");
        assertThat(admin.getEmail()).isEqualTo("admin@padel.com");
        assertThat(admin.getPasswordHash()).isEqualTo("existing-hash");
        verify(administrateurRepository, never()).save(any());
    }

    private void existingDatabaseWithAdmin(Administrateur admin) {
        Participant participant = Participant.builder().matricule("L10002").passwordHash("existing-hash").build();
        participant.setId(50L);
        when(centreRepository.count()).thenReturn(1L);
        when(atelierRepository.count()).thenReturn(1L);
        when(participantRepository.findAll()).thenReturn(List.of(participant));
        when(participantRepository.findByMatriculeIgnoreCase("L10002")).thenReturn(Optional.of(participant));
        when(penaliteRepository.existsByParticipantId(50L)).thenReturn(true);
        when(administrateurRepository.findByMatricule(anyString())).thenAnswer(invocation ->
                admin.getMatricule().equals(invocation.getArgument(0)) ? Optional.of(admin) : Optional.empty());
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
        when(penaliteRepository.existsByParticipantId(eq(50L)))
                .thenReturn(true);

        dataInitializer.run();

        assertThat(existing.getEmail()).isEqualTo("adresse.personnalisee@example.com");
        assertThat(existing.getPasswordHash()).isEqualTo("mon-hachage-bcrypt-existant");
        verify(participantRepository, never()).save(existing);
    }
}
