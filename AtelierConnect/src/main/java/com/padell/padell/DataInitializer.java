package com.padell.padell;

import com.padell.padell.entity.Administrateur;
import com.padell.padell.entity.JourFermeture;
import com.padell.padell.entity.Atelier;
import com.padell.padell.entity.Participant;
import com.padell.padell.entity.Paiement;
import com.padell.padell.entity.Penalite;
import com.padell.padell.entity.Reservation;
import com.padell.padell.entity.Centre;
import com.padell.padell.entity.Salle;
import com.padell.padell.entity.enums.StatutAtelier;
import com.padell.padell.entity.enums.StatutPaiement;
import com.padell.padell.entity.enums.StatutReservation;
import com.padell.padell.entity.enums.TypeAdministrateur;
import com.padell.padell.entity.enums.TypeAtelier;
import com.padell.padell.entity.enums.TypeParticipant;
import com.padell.padell.repository.AdministrateurRepository;
import com.padell.padell.repository.JourFermetureRepository;
import com.padell.padell.repository.MatchRepository;
import com.padell.padell.repository.MembreRepository;
import com.padell.padell.repository.PaiementRepository;
import com.padell.padell.repository.PenaliteRepository;
import com.padell.padell.repository.ReservationRepository;
import com.padell.padell.repository.SiteRepository;
import com.padell.padell.repository.TerrainRepository;
import com.padell.padell.service.AtelierRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    @Value("${APP_DEMO_MEMBER_PASSWORD}")
    private String demoMemberPassword;
    @Value("${APP_DEMO_ADMIN_PASSWORD}")
    private String demoAdminPassword;
    private final SiteRepository siteRepository;
    private final TerrainRepository terrainRepository;
    private final MembreRepository membreRepository;
    private final AdministrateurRepository administrateurRepository;
    private final JourFermetureRepository jourFermetureRepository;
    private final MatchRepository matchRepository;
    private final ReservationRepository reservationRepository;
    private final PaiementRepository paiementRepository;
    private final PenaliteRepository penaliteRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        migrateLegacyMemberCredentials();

        if (siteRepository.count() > 0) {
            if (matchRepository.count() == 0) {
                log.info("DataInitializer : base de donnees deja initialisee, ajout des matchs d'exemple...");
                seedMatchesFromExistingData();
            }
            ensurePenalizedDemoMember();
            ensureDemoMembersCredentials();
            ensureDemoAdminCredentials();
            log.info("DataInitializer : base de donnees deja initialisee, initialisation ignoree...");
            return;
        }

        log.info("DataInitializer : initialisation de la base de donnees...");

        Centre siteLyon = siteRepository.save(Centre.builder()
                .nom("Padel Club Lyon")
                .adresse("12 rue de la Republique, Lyon")
                .heureOuverture(LocalTime.of(8, 0))
                .heureFermeture(LocalTime.of(22, 0))
                .dureeMatchMinutes(AtelierRules.DUREE_MINUTES)
                .dureeEntreMatchMinutes(AtelierRules.PAUSE_MINUTES)
                .anneeCivile(2025)
                .ouvertVisiteurs(true)
                .build());

        Centre siteParis = siteRepository.save(Centre.builder()
                .nom("Padel Club Paris")
                .adresse("45 avenue des Champs, Paris")
                .heureOuverture(LocalTime.of(7, 0))
                .heureFermeture(LocalTime.of(23, 0))
                .dureeMatchMinutes(AtelierRules.DUREE_MINUTES)
                .dureeEntreMatchMinutes(AtelierRules.PAUSE_MINUTES)
                .anneeCivile(2025)
                .ouvertVisiteurs(true)
                .build());

        log.info("DataInitializer : {} sites crees", siteRepository.count());
////-------------------Terrains--------------------//
        Salle courtA = terrainRepository.save(Salle.builder().nom("Court A").site(siteLyon).prix(AtelierRules.PRIX_TOTAL).build());
        Salle courtB = terrainRepository.save(Salle.builder().nom("Court B").site(siteLyon).prix(AtelierRules.PRIX_TOTAL).build());
        Salle courtC = terrainRepository.save(Salle.builder().nom("Court C").site(siteLyon).prix(AtelierRules.PRIX_TOTAL).build());
        Salle court1 = terrainRepository.save(Salle.builder().nom("Court 1").site(siteParis).prix(AtelierRules.PRIX_TOTAL).build());
        terrainRepository.save(Salle.builder().nom("Court 2").site(siteParis).prix(AtelierRules.PRIX_TOTAL).build());

        log.info("DataInitializer : {} terrains crees", terrainRepository.count());
//-----------------------Jours de fermeture--------------------//
        jourFermetureRepository.save(JourFermeture.builder()
                .date(LocalDate.of(2025, 12, 25))
                .raison("Jour de Noel")
                .global(true)
                .site(null)
                .build());

        jourFermetureRepository.save(JourFermeture.builder()
                .date(LocalDate.of(2025, 1, 1))
                .raison("Jour de l'An")
                .global(true)
                .site(null)
                .build());

        jourFermetureRepository.save(JourFermeture.builder()
                .date(LocalDate.of(2025, 7, 14))
                .raison("Fete nationale - maintenance du site de Lyon")
                .global(false)
                .site(siteLyon)
                .build());

        log.info("DataInitializer : jours de fermeture crees");
//-----------------------Membres--------------------//
        String defaultMemberPasswordHash = passwordEncoder.encode(demoMemberPassword);

        Participant lucas = membreRepository.save(Participant.builder()
                .matricule("G1001")
                .nom("Martin")
                .prenom("Lucas")
                .email("lucas.martin@gmail.com")
                .typeMembre(TypeParticipant.GLOBAL)
                .solde(0.0)
                .site(null)
                .passwordHash(defaultMemberPasswordHash)
                .build());

        Participant emma = membreRepository.save(Participant.builder()
                .matricule("G1002")
                .nom("Dubois")
                .prenom("Emma")
                .email("emma.dubois@gmail.com")
                .typeMembre(TypeParticipant.GLOBAL)
                .solde(0.0)
                .site(null)
                .passwordHash(defaultMemberPasswordHash)
                .build());

        Participant tom = membreRepository.save(Participant.builder()
                .matricule("S10001")
                .nom("Bernard")
                .prenom("Tom")
                .email("tom.bernard@gmail.com")
                .typeMembre(TypeParticipant.SITE)
                .solde(0.0)
                .site(siteLyon)
                .passwordHash(defaultMemberPasswordHash)
                .build());

        Participant sarah = membreRepository.save(Participant.builder()
                .matricule("S10002")
                .nom("Leroy")
                .prenom("Sarah")
                .email("sarah.leroy@gmail.com")
                .typeMembre(TypeParticipant.SITE)
                .solde(0.0)
                .site(siteParis)
                .passwordHash(defaultMemberPasswordHash)
                .build());

        Participant alex = membreRepository.save(Participant.builder()
                .matricule("L10001")
                .nom("Petit")
                .prenom("Alex")
                .email("alex.petit@gmail.com")
                .typeMembre(TypeParticipant.LIBRE)
                .solde(0.0)
                .site(null)
                .passwordHash(defaultMemberPasswordHash)
                .build());

        Participant penalise = membreRepository.save(Participant.builder()
                .matricule("L10002")
                .nom("Penalise")
                .prenom("Demo")
                .email("demo.penalise@gmail.com")
                .typeMembre(TypeParticipant.LIBRE)
                .solde(0.0)
                .site(null)
                .passwordHash(defaultMemberPasswordHash)
                .build());

        log.info("DataInitializer : {} membres crees", membreRepository.count());

        seedActivePenalty(penalise);
        seedMatches(courtA, courtB, courtC, court1, lucas, emma, tom, sarah, alex);
//-----------------------Administrateurs--------------------//
        administrateurRepository.save(Administrateur.builder()
                .matricule("ADMIN001")
                .nom("Admin")
                .prenom("Global")
                .email("admin@padel.com")
                .passwordHash(passwordEncoder.encode(demoAdminPassword))
                .typeAdministrateur(TypeAdministrateur.GLOBAL)
                .site(null)
                .build());

        administrateurRepository.save(Administrateur.builder()
                .matricule("ADMIN002")
                .nom("Admin")
                .prenom("Lyon")
                .email("admin.lyon@padel.com")
                .passwordHash(passwordEncoder.encode(demoAdminPassword))
                .typeAdministrateur(TypeAdministrateur.SITE)
                .site(siteLyon)
                .build());

        administrateurRepository.save(Administrateur.builder()
                .matricule("ADMIN003")
                .nom("Admin")
                .prenom("Paris")
                .email("admin.paris@padel.com")
                .passwordHash(passwordEncoder.encode(demoAdminPassword))
                .typeAdministrateur(TypeAdministrateur.SITE)
                .site(siteParis)
                .build());

        log.info("DataInitializer : {} administrateurs crees", administrateurRepository.count());
        log.info("DataInitializer : initialisation terminee avec succes !");
    }

    private void migrateLegacyMemberCredentials() {
        List<Participant> legacyMembers = membreRepository.findAll().stream()
                .filter(member -> !StringUtils.hasText(member.getPasswordHash()))
                .toList();

        if (legacyMembers.isEmpty()) {
            return;
        }

        legacyMembers.forEach(member -> member.setPasswordHash(passwordEncoder.encode(demoMemberPassword)));
        membreRepository.saveAll(legacyMembers);

        log.warn("DataInitializer : {} compte(s) membre legacy ont recu le mot de passe par defaut", legacyMembers.size());
    }

    private void seedMatchesFromExistingData() {
        Salle courtA = requireSeedTerrain("Court A");
        Salle courtB = requireSeedTerrain("Court B");
        Salle courtC = requireSeedTerrain("Court C");
        Salle court1 = requireSeedTerrain("Court 1");

        Participant lucas = requireSeedMember("G1001");
        Participant emma = requireSeedMember("G1002");
        Participant tom = requireSeedMember("S10001");
        Participant sarah = requireSeedMember("S10002");
        Participant alex = requireSeedMember("L10001");

        seedMatches(courtA, courtB, courtC, court1, lucas, emma, tom, sarah, alex);
    }

    private Salle requireSeedTerrain(String nom) {
        return terrainRepository.findAll().stream()
                .filter(terrain -> terrain.getNom().equals(nom))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Terrain seed introuvable : " + nom));
    }

    private Participant requireSeedMember(String matricule) {
        return membreRepository.findByMatriculeIgnoreCase(matricule)
                .orElseThrow(() -> new IllegalStateException("Membre seed introuvable : " + matricule));
    }

    private void ensurePenalizedDemoMember() {
        Participant membre = membreRepository.findByMatriculeIgnoreCase("L10002")
                .orElseGet(() -> membreRepository.save(Participant.builder()
                        .matricule("L10002")
                        .nom("Penalise")
                        .prenom("Demo")
                        .email("demo.penalise@gmail.com")
                        .typeMembre(TypeParticipant.LIBRE)
                        .solde(0.0)
                        .site(null)
                        .passwordHash(passwordEncoder.encode(demoMemberPassword))
                        .build()));

        seedActivePenalty(membre);
    }

    private void ensureDemoMembersCredentials() {
        updateDemoMemberCredentials("G1001", "lucas.martin@gmail.com");
        updateDemoMemberCredentials("G1002", "emma.dubois@gmail.com");
        updateDemoMemberCredentials("S10001", "tom.bernard@gmail.com");
        updateDemoMemberCredentials("S10002", "sarah.leroy@gmail.com");
        updateDemoMemberCredentials("L10001", "alex.petit@gmail.com");
        updateDemoMemberCredentials("L10002", "demo.penalise@gmail.com");
    }

    private void updateDemoMemberCredentials(String matricule, String email) {
        membreRepository.findByMatriculeIgnoreCase(matricule).ifPresent(membre -> {
            membre.setEmail(email);
            membre.setPasswordHash(passwordEncoder.encode(demoMemberPassword));
            membreRepository.save(membre);
        });
    }

    private void ensureDemoAdminCredentials() {
        updateDemoAdminCredentials("admin@padel.com");
        updateDemoAdminCredentials("admin.lyon@padel.com");
        updateDemoAdminCredentials("admin.paris@padel.com");
    }

    private void updateDemoAdminCredentials(String email) {
        administrateurRepository.findByEmail(email).ifPresent(admin -> {
            admin.setPasswordHash(passwordEncoder.encode(demoAdminPassword));
            administrateurRepository.save(admin);
        });
    }

    private void seedActivePenalty(Participant membre) {
        if (penaliteRepository.existsByMembreIdAndDateFinAfter(membre.getId(), LocalDate.now())) {
            return;
        }

        penaliteRepository.save(Penalite.builder()
                .membre(membre)
                .dateFin(LocalDate.now().plusWeeks(1))
                .motif("Membre de demonstration penalise")
                .build());
    }

    private void seedMatches(
            Salle courtA,
            Salle courtB,
            Salle courtC,
            Salle court1,
            Participant lucas,
            Participant emma,
            Participant tom,
            Participant sarah,
            Participant alex
    ) {
        Atelier publicLyon = createSeedMatch(courtA, lucas, LocalDate.now().plusDays(2).atTime(10, 0),
                TypeAtelier.PUBLIC, StatutAtelier.PLANIFIE, 0);
        createSeedReservation(publicLyon, lucas, StatutReservation.EN_ATTENTE, StatutPaiement.EN_ATTENTE);

        Atelier privateLyon = createSeedMatch(courtB, tom, LocalDate.now().plusDays(3).atTime(14, 0),
                TypeAtelier.PRIVE, StatutAtelier.PLANIFIE, 1);
        createSeedReservation(privateLyon, tom, StatutReservation.CONFIRMEE, StatutPaiement.PAYE);

        Atelier publicParis = createSeedMatch(court1, sarah, LocalDate.now().plusDays(4).atTime(18, 0),
                TypeAtelier.PUBLIC, StatutAtelier.PLANIFIE, 2);
        createSeedReservation(publicParis, sarah, StatutReservation.CONFIRMEE, StatutPaiement.PAYE);
        createSeedReservation(publicParis, emma, StatutReservation.CONFIRMEE, StatutPaiement.PAYE);

        Atelier completeLyon = createSeedMatch(courtC, lucas, LocalDate.now().plusDays(5).atTime(16, 0),
                TypeAtelier.PUBLIC, StatutAtelier.PLANIFIE, 4);
        createSeedReservation(completeLyon, lucas, StatutReservation.CONFIRMEE, StatutPaiement.PAYE);
        createSeedReservation(completeLyon, emma, StatutReservation.CONFIRMEE, StatutPaiement.PAYE);
        createSeedReservation(completeLyon, tom, StatutReservation.CONFIRMEE, StatutPaiement.PAYE);
        createSeedReservation(completeLyon, alex, StatutReservation.CONFIRMEE, StatutPaiement.PAYE);

        log.info("DataInitializer : {} matchs crees", matchRepository.count());
    }

    private Atelier createSeedMatch(
            Salle terrain,
            Participant organisateur,
            LocalDateTime dateDebut,
            TypeAtelier typeMatch,
            StatutAtelier statut,
            int nbJoueursActuels
    ) {
        return matchRepository.save(Atelier.builder()
                .terrain(terrain)
                .organisateur(organisateur)
                .dateDebut(dateDebut)
                .dateFin(dateDebut.plusMinutes(AtelierRules.DUREE_MINUTES))
                .typeMatch(typeMatch)
                .statut(statut)
                .nbJoueursActuels(nbJoueursActuels)
                .prixTotal(AtelierRules.PRIX_TOTAL)
                .prixParJoueur(AtelierRules.PRIX_PARTICIPANT)
                .build());
    }

    private void createSeedReservation(
            Atelier match,
            Participant membre,
            StatutReservation statutReservation,
            StatutPaiement statutPaiement
    ) {
        Reservation reservation = reservationRepository.save(Reservation.builder()
                .match(match)
                .membre(membre)
                .statut(statutReservation)
                .build());

        Paiement paiement = paiementRepository.save(Paiement.builder()
                .reservation(reservation)
                .montant(match.getPrixParJoueur())
                .statut(statutPaiement)
                .datePaiement(statutPaiement == StatutPaiement.PAYE ? LocalDateTime.now() : null)
                .build());

        reservation.setPaiement(paiement);
    }
}
