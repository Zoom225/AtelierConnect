package com.atelierconnect;

import com.atelierconnect.entity.Administrateur;
import com.atelierconnect.entity.JourFermeture;
import com.atelierconnect.entity.Atelier;
import com.atelierconnect.entity.Participant;
import com.atelierconnect.entity.Paiement;
import com.atelierconnect.entity.Penalite;
import com.atelierconnect.entity.Reservation;
import com.atelierconnect.entity.Centre;
import com.atelierconnect.entity.Salle;
import com.atelierconnect.entity.enums.StatutAtelier;
import com.atelierconnect.entity.enums.StatutPaiement;
import com.atelierconnect.entity.enums.StatutReservation;
import com.atelierconnect.entity.enums.TypeAdministrateur;
import com.atelierconnect.entity.enums.TypeAtelier;
import com.atelierconnect.entity.enums.TypeParticipant;
import com.atelierconnect.repository.AdministrateurRepository;
import com.atelierconnect.repository.JourFermetureRepository;
import com.atelierconnect.repository.AtelierRepository;
import com.atelierconnect.repository.ParticipantRepository;
import com.atelierconnect.repository.PaiementRepository;
import com.atelierconnect.repository.PenaliteRepository;
import com.atelierconnect.repository.ReservationRepository;
import com.atelierconnect.repository.CentreRepository;
import com.atelierconnect.repository.SalleRepository;
import com.atelierconnect.service.AtelierRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.data.initialize", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    @Value("${APP_DEMO_MEMBER_PASSWORD}")
    private String demoParticipantPassword;
    private final CentreRepository centreRepository;
    private final SalleRepository salleRepository;
    private final ParticipantRepository participantRepository;
    private final AdministrateurRepository administrateurRepository;
    private final JourFermetureRepository jourFermetureRepository;
    private final AtelierRepository atelierRepository;
    private final ReservationRepository reservationRepository;
    private final PaiementRepository paiementRepository;
    private final PenaliteRepository penaliteRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        migrateLegacyParticipantCredentials();

        if (centreRepository.count() > 0) {
            if (atelierRepository.count() == 0) {
                log.info("DataInitializer : base de donnees deja initialisee, ajout des ateliers d'exemple...");
                seedAteliersFromExistingData();
            }
            ensurePenalizedDemoParticipant();
            ensureDemoParticipantsCredentials();
            ensureDemoAdminCredentials();
            log.info("DataInitializer : base de donnees deja initialisee, initialisation ignoree...");
            return;
        }

        log.info("DataInitializer : initialisation de la base de donnees...");

        Centre centreBruxelles = centreRepository.save(Centre.builder()
                .nom("Centre Créatif Bruxelles")
                .adresse("Rue des Ateliers 12, 1000 Bruxelles")
                .heureOuverture(LocalTime.of(8, 0))
                .heureFermeture(LocalTime.of(22, 0))
                .dureeAtelierMinutes(AtelierRules.DUREE_MINUTES)
                .dureeEntreAtelierMinutes(AtelierRules.PAUSE_MINUTES)
                .anneeCivile(LocalDate.now().getYear())
                .ouvertVisiteurs(true)
                .build());

        Centre centreNamur = centreRepository.save(Centre.builder()
                .nom("Centre Collaboratif Namur")
                .adresse("Rue de la Créativité 45, 5000 Namur")
                .heureOuverture(LocalTime.of(7, 0))
                .heureFermeture(LocalTime.of(23, 0))
                .dureeAtelierMinutes(AtelierRules.DUREE_MINUTES)
                .dureeEntreAtelierMinutes(AtelierRules.PAUSE_MINUTES)
                .anneeCivile(LocalDate.now().getYear())
                .ouvertVisiteurs(true)
                .build());

        log.info("DataInitializer : {} centres créés", centreRepository.count());
//-----------------------Salles--------------------//
        Salle salleCreation = salleRepository.save(Salle.builder().nom("Salle Création A").centre(centreBruxelles).prix(AtelierRules.PRIX_TOTAL).build());
        Salle salleFormation = salleRepository.save(Salle.builder().nom("Salle Formation B").centre(centreBruxelles).prix(AtelierRules.PRIX_TOTAL).build());
        Salle salleCollaborative = salleRepository.save(Salle.builder().nom("Salle Collaborative C").centre(centreBruxelles).prix(AtelierRules.PRIX_TOTAL).build());
        Salle salleNumerique = salleRepository.save(Salle.builder().nom("Salle Numérique 1").centre(centreNamur).prix(AtelierRules.PRIX_TOTAL).build());
        salleRepository.save(Salle.builder().nom("Salle Numérique 2").centre(centreNamur).prix(AtelierRules.PRIX_TOTAL).build());

        log.info("DataInitializer : {} salles créées", salleRepository.count());
//-----------------------Jours de fermeture--------------------//
        jourFermetureRepository.save(JourFermeture.builder()
                .date(LocalDate.of(LocalDate.now().getYear(), 12, 25))
                .raison("Jour de Noel")
                .global(true)
                .centre(null)
                .build());

        jourFermetureRepository.save(JourFermeture.builder()
                .date(LocalDate.of(LocalDate.now().getYear() + 1, 1, 1))
                .raison("Jour de l'An")
                .global(true)
                .centre(null)
                .build());

        jourFermetureRepository.save(JourFermeture.builder()
                .date(LocalDate.now().plusDays(45))
                .raison("Maintenance planifiée du centre de Bruxelles")
                .global(false)
                .centre(centreBruxelles)
                .build());

        log.info("DataInitializer : jours de fermeture crees");
//-----------------------Participants--------------------//
        String defaultParticipantPasswordHash = passwordEncoder.encode(demoParticipantPassword);

        Participant lucas = participantRepository.save(Participant.builder()
                .matricule("G1001")
                .nom("Martin")
                .prenom("Lucas")
                .email("lucas.martin@gmail.com")
                .typeParticipant(TypeParticipant.PARTENAIRE)
                .solde(0.0)
                .centre(null)
                .passwordHash(defaultParticipantPasswordHash)
                .build());

        Participant emma = participantRepository.save(Participant.builder()
                .matricule("G1002")
                .nom("Dubois")
                .prenom("Emma")
                .email("emma.dubois@gmail.com")
                .typeParticipant(TypeParticipant.PARTENAIRE)
                .solde(0.0)
                .centre(null)
                .passwordHash(defaultParticipantPasswordHash)
                .build());

        Participant tom = participantRepository.save(Participant.builder()
                .matricule("S10001")
                .nom("Bernard")
                .prenom("Tom")
                .email("tom.bernard@gmail.com")
                .typeParticipant(TypeParticipant.ABONNE)
                .solde(0.0)
                .centre(centreBruxelles)
                .passwordHash(defaultParticipantPasswordHash)
                .build());

        Participant sarah = participantRepository.save(Participant.builder()
                .matricule("S10002")
                .nom("Leroy")
                .prenom("Sarah")
                .email("sarah.leroy@gmail.com")
                .typeParticipant(TypeParticipant.ABONNE)
                .solde(0.0)
                .centre(centreNamur)
                .passwordHash(defaultParticipantPasswordHash)
                .build());

        Participant alex = participantRepository.save(Participant.builder()
                .matricule("L10001")
                .nom("Petit")
                .prenom("Alex")
                .email("alex.petit@gmail.com")
                .typeParticipant(TypeParticipant.VISITEUR)
                .solde(0.0)
                .centre(null)
                .passwordHash(defaultParticipantPasswordHash)
                .build());

        Participant penalise = participantRepository.save(Participant.builder()
                .matricule("L10002")
                .nom("Penalise")
                .prenom("Demo")
                .email("demo.penalise@gmail.com")
                .typeParticipant(TypeParticipant.VISITEUR)
                .solde(0.0)
                .centre(null)
                .passwordHash(defaultParticipantPasswordHash)
                .build());

        log.info("DataInitializer : {} participants créés", participantRepository.count());

        seedActivePenalty(penalise);
        seedAteliers(salleCreation, salleFormation, salleCollaborative, salleNumerique, lucas, emma, tom, sarah, alex);
//-----------------------Administrateurs--------------------//
        saveDemoAdmin(Administrateur.builder()
                .matricule("ADMIN001")
                .nom("Admin")
                .prenom("Global")
                .email("admin@atelierconnect.example")
                .typeAdministrateur(TypeAdministrateur.GLOBAL)
                .centre(null)
                .build());

        saveDemoAdmin(Administrateur.builder()
                .matricule("ADMIN002")
                .nom("Admin")
                .prenom("Bruxelles")
                .email("admin.bruxelles@atelierconnect.example")
                .typeAdministrateur(TypeAdministrateur.SITE)
                .centre(centreBruxelles)
                .build());

        saveDemoAdmin(Administrateur.builder()
                .matricule("ADMIN003")
                .nom("Admin")
                .prenom("Namur")
                .email("admin.namur@atelierconnect.example")
                .typeAdministrateur(TypeAdministrateur.SITE)
                .centre(centreNamur)
                .build());

        log.info("DataInitializer : {} administrateurs crees", administrateurRepository.count());
        log.info("DataInitializer : initialisation terminee avec succes !");
    }

    private void migrateLegacyParticipantCredentials() {
        List<Participant> legacyParticipants = participantRepository.findAll().stream()
                .filter(member -> !StringUtils.hasText(member.getPasswordHash()))
                .toList();

        if (legacyParticipants.isEmpty()) {
            return;
        }

        legacyParticipants.forEach(member -> member.setPasswordHash(passwordEncoder.encode(demoParticipantPassword)));
        participantRepository.saveAll(legacyParticipants);

        log.warn("DataInitializer : {} compte(s) participant legacy ont recu le mot de passe par defaut", legacyParticipants.size());
    }

    private void seedAteliersFromExistingData() {
        Salle salleCreation = requireSeedSalle("Court A", "Salle Création A");
        Salle salleFormation = requireSeedSalle("Court B", "Salle Formation B");
        Salle salleCollaborative = requireSeedSalle("Court C", "Salle Collaborative C");
        Salle salleNumerique = requireSeedSalle("Court 1", "Salle Numérique 1");

        Participant lucas = requireSeedParticipant("G1001");
        Participant emma = requireSeedParticipant("G1002");
        Participant tom = requireSeedParticipant("S10001");
        Participant sarah = requireSeedParticipant("S10002");
        Participant alex = requireSeedParticipant("L10001");

        seedAteliers(salleCreation, salleFormation, salleCollaborative, salleNumerique, lucas, emma, tom, sarah, alex);
    }

    private Salle requireSeedSalle(String ancienNom, String nouveauNom) {
        return salleRepository.findAll().stream()
                .filter(salle -> ancienNom.equals(salle.getNom()) || nouveauNom.equals(salle.getNom()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Salle de démonstration introuvable : " + ancienNom + " / " + nouveauNom));
    }

    private Participant requireSeedParticipant(String matricule) {
        return participantRepository.findByMatriculeIgnoreCase(matricule)
                .orElseThrow(() -> new IllegalStateException("Participant seed introuvable : " + matricule));
    }

    private void ensurePenalizedDemoParticipant() {
        Participant participant = participantRepository.findByMatriculeIgnoreCase("L10002")
                .orElseGet(() -> participantRepository.save(Participant.builder()
                        .matricule("L10002")
                        .nom("Penalise")
                        .prenom("Demo")
                        .email("demo.penalise@gmail.com")
                        .typeParticipant(TypeParticipant.VISITEUR)
                        .solde(0.0)
                        .centre(null)
                        .passwordHash(passwordEncoder.encode(demoParticipantPassword))
                        .build()));

        seedActivePenalty(participant);
    }

    private void ensureDemoParticipantsCredentials() {
        updateDemoParticipantCredentials("G1001", "lucas.martin@gmail.com");
        updateDemoParticipantCredentials("G1002", "emma.dubois@gmail.com");
        updateDemoParticipantCredentials("S10001", "tom.bernard@gmail.com");
        updateDemoParticipantCredentials("S10002", "sarah.leroy@gmail.com");
        updateDemoParticipantCredentials("L10001", "alex.petit@gmail.com");
        updateDemoParticipantCredentials("L10002", "demo.penalise@gmail.com");
    }

    private void updateDemoParticipantCredentials(String matricule, String email) {
        participantRepository.findByMatriculeIgnoreCase(matricule).ifPresent(participant -> {
            // Une initialisation ne doit jamais réinitialiser le mot de passe
            // d'un compte existant à chaque redémarrage.
            boolean modifie = false;
            if (!StringUtils.hasText(participant.getEmail())) {
                participant.setEmail(email);
                modifie = true;
            }
            if (!StringUtils.hasText(participant.getPasswordHash())) {
                participant.setPasswordHash(passwordEncoder.encode(demoParticipantPassword));
                modifie = true;
            }
            if (modifie) {
                participantRepository.save(participant);
            }
        });
    }

    private void saveDemoAdmin(Administrateur admin) {
        admin.setPasswordHash(passwordEncoder.encode(admin.getMatricule()));
        administrateurRepository.save(admin);
    }

    private void ensureDemoAdminCredentials() {
        // Maintenir les identifiants historiques des bases déjà initialisées.
        updateDemoAdminCredentials("admin@padel.com");
        updateDemoAdminCredentials("admin.lyon@padel.com");
        updateDemoAdminCredentials("admin.paris@padel.com");
        updateDemoAdminCredentials("admin@atelierconnect.example");
        updateDemoAdminCredentials("admin.bruxelles@atelierconnect.example");
        updateDemoAdminCredentials("admin.namur@atelierconnect.example");
    }

    private void updateDemoAdminCredentials(String email) {
        administrateurRepository.findByEmail(email).ifPresent(admin -> {
            // Préserver les identifiants de connexion déjà personnalisés.
            if (!StringUtils.hasText(admin.getPasswordHash())) {
                admin.setPasswordHash(passwordEncoder.encode(admin.getMatricule()));
                administrateurRepository.save(admin);
            }
        });
    }

    private void seedActivePenalty(Participant participant) {
        if (penaliteRepository.existsByParticipantIdAndDateFinAfter(participant.getId(), LocalDate.now())) {
            return;
        }

        penaliteRepository.save(Penalite.builder()
                .participant(participant)
                .dateFin(LocalDate.now().plusWeeks(1))
                .motif("Participant de démonstration suspendu")
                .build());
    }

    private void seedAteliers(
            Salle salleCreation,
            Salle salleFormation,
            Salle salleCollaborative,
            Salle salleNumerique,
            Participant lucas,
            Participant emma,
            Participant tom,
            Participant sarah,
            Participant alex
    ) {
        Atelier atelierPublicBruxelles = createSeedAtelier(salleCreation, lucas, LocalDate.now().plusDays(2).atTime(10, 0),
                TypeAtelier.PUBLIC, StatutAtelier.PLANIFIE, 0);
        createSeedReservation(atelierPublicBruxelles, lucas, StatutReservation.EN_ATTENTE, StatutPaiement.EN_ATTENTE);

        Atelier atelierPriveBruxelles = createSeedAtelier(salleFormation, tom, LocalDate.now().plusDays(3).atTime(14, 0),
                TypeAtelier.PRIVE, StatutAtelier.PLANIFIE, 1);
        createSeedReservation(atelierPriveBruxelles, tom, StatutReservation.CONFIRMEE, StatutPaiement.PAYE);

        Atelier atelierPublicNamur = createSeedAtelier(salleNumerique, sarah, LocalDate.now().plusDays(4).atTime(18, 0),
                TypeAtelier.PUBLIC, StatutAtelier.PLANIFIE, 2);
        createSeedReservation(atelierPublicNamur, sarah, StatutReservation.CONFIRMEE, StatutPaiement.PAYE);
        createSeedReservation(atelierPublicNamur, emma, StatutReservation.CONFIRMEE, StatutPaiement.PAYE);

        Atelier atelierCompletBruxelles = createSeedAtelier(salleCollaborative, lucas, LocalDate.now().plusDays(5).atTime(16, 0),
                TypeAtelier.PUBLIC, StatutAtelier.PLANIFIE, 4);
        createSeedReservation(atelierCompletBruxelles, lucas, StatutReservation.CONFIRMEE, StatutPaiement.PAYE);
        createSeedReservation(atelierCompletBruxelles, emma, StatutReservation.CONFIRMEE, StatutPaiement.PAYE);
        createSeedReservation(atelierCompletBruxelles, tom, StatutReservation.CONFIRMEE, StatutPaiement.PAYE);
        createSeedReservation(atelierCompletBruxelles, alex, StatutReservation.CONFIRMEE, StatutPaiement.PAYE);

        log.info("DataInitializer : {} ateliers créés", atelierRepository.count());
    }

    private Atelier createSeedAtelier(
            Salle salle,
            Participant organisateur,
            LocalDateTime dateDebut,
            TypeAtelier typeAtelier,
            StatutAtelier statut,
            int nbParticipantsActuels
    ) {
        return atelierRepository.save(Atelier.builder()
                .salle(salle)
                .organisateur(organisateur)
                .dateDebut(dateDebut)
                .dateFin(dateDebut.plusMinutes(AtelierRules.DUREE_MINUTES))
                .typeAtelier(typeAtelier)
                .statut(statut)
                .nbParticipantsActuels(nbParticipantsActuels)
                .prixTotal(AtelierRules.PRIX_TOTAL)
                .prixParParticipant(AtelierRules.PRIX_PARTICIPANT)
                .build());
    }

    private void createSeedReservation(
            Atelier atelier,
            Participant participant,
            StatutReservation statutReservation,
            StatutPaiement statutPaiement
    ) {
        Reservation reservation = reservationRepository.save(Reservation.builder()
                .atelier(atelier)
                .participant(participant)
                .statut(statutReservation)
                .build());

        Paiement paiement = paiementRepository.save(Paiement.builder()
                .reservation(reservation)
                .montant(atelier.getPrixParParticipant())
                .statut(statutPaiement)
                .datePaiement(statutPaiement == StatutPaiement.PAYE ? LocalDateTime.now() : null)
                .build());

        reservation.setPaiement(paiement);
    }
}
