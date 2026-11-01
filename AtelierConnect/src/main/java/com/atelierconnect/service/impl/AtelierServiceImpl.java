package com.atelierconnect.service.impl;

import com.atelierconnect.dto.request.AtelierRequest;
import com.atelierconnect.entity.*;
import com.atelierconnect.entity.enums.*;
import com.atelierconnect.exception.BusinessException;
import com.atelierconnect.exception.ResourceNotFoundException;
import com.atelierconnect.mapper.AtelierMapper;
import com.atelierconnect.dto.request.CreateAtelierRequest;
import com.atelierconnect.dto.response.AtelierDto;
import com.atelierconnect.repository.*;
import com.atelierconnect.service.AtelierService;
import com.atelierconnect.service.ParticipantService;
import com.atelierconnect.service.SalleService;
import com.atelierconnect.service.AtelierRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AtelierServiceImpl implements AtelierService {

    private final AtelierRepository matchRepository;
    private final ParticipantRepository membreRepository;
    private final ReservationRepository reservationRepository;
    private final PaiementRepository paiementRepository;
    private final JourFermetureRepository jourFermetureRepository;
    private final SalleService terrainService;
    private final ParticipantService membreService;
    private final AtelierMapper matchMapper;

    @Override
    @Transactional
    public AtelierDto createMatch(CreateAtelierRequest request, String username) {
        Participant organisateur = membreRepository.findByMatriculeIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Participant introuvable pour l'utilisateur : " + username));

        Salle terrain = terrainService.getById(request.terrainId());

        // Regle metier : un membre avec un solde impaye ne peut pas creer de match.
        if (membreService.hasOutstandingBalance(organisateur.getId())) {
            throw new BusinessException("Vous avez un solde impayé et ne pouvez pas créer d'atelier.");
        }
        // Regle metier : un membre penalise ne peut pas creer de match.
        if (membreService.hasActivePenalty(organisateur.getId())) {
            throw new BusinessException("Vous avez une suspension active et ne pouvez pas créer d'atelier.");
        }

        validateBookingDelay(organisateur, request.matchDate().toLocalDate());
        validateSiteAccessForOrganizer(organisateur, terrain);
        //Service metier debut et fin match
        LocalDateTime dateDebut = request.matchDate();
        if (dateDebut.isBefore(LocalDateTime.now())) {
            throw new BusinessException("Un atelier ne peut pas être créé dans le passé.");
        }
        LocalDateTime dateFin = dateDebut.plusMinutes(AtelierRules.DUREE_MINUTES);

        validateSiteNotClosed(terrain, dateDebut.toLocalDate());
        validateSiteOpeningHours(terrain, dateDebut.toLocalTime(), dateFin.toLocalTime());

        // Regle metier : un terrain ne peut pas avoir deux matchs sur le meme creneau horaire.
        if (!isSlotAvailable(terrain, dateDebut, dateFin)) {
            throw new BusinessException("Ce créneau est déjà réservé pour la salle : " + terrain.getId());
        }

        Atelier match = new Atelier();
        match.setOrganisateur(organisateur);
        match.setTerrain(terrain);
        match.setDateDebut(dateDebut);
        match.setDateFin(dateFin);
        match.setTypeMatch(TypeAtelier.valueOf(request.matchType()));
        match.setStatut(StatutAtelier.PLANIFIE);
        match.setNbJoueursActuels(0);
        match.setPrixTotal(AtelierRules.PRIX_TOTAL);
        match.setPrixParJoueur(AtelierRules.PRIX_PARTICIPANT);

        Atelier savedMatch = matchRepository.save(match);
        // Regle metier : l'organisateur commence avec une reservation en attente et un paiement associe.
        createPendingReservationForOrganizer(savedMatch, organisateur);
        log.info("Atelier créé avec succès (ID: {}) par l'utilisateur {}", savedMatch.getId(), username);

        return matchMapper.toAtelierDto(savedMatch);
    }

    @Override
    public List<AtelierDto> findAllMatches() {
        return matchRepository.findAll().stream()
                .map(matchMapper::toAtelierDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AtelierDto> getPublicAvailableMatches() {
        return matchRepository.findByTypeAtelierAndStatut(TypeAtelier.PUBLIC, StatutAtelier.PLANIFIE)
                .stream()
                .map(matchMapper::toAtelierDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AtelierDto> findByOrganisateur(Long organisateurId) {
        return matchRepository.findByOrganisateurId(organisateurId)
                .stream()
                .map(matchMapper::toAtelierDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AtelierDto> findBySite(Long siteId) {
        return matchRepository.findBySalleCentreId(siteId)
                .stream()
                .map(matchMapper::toAtelierDto)
                .collect(Collectors.toList());
    }

    @Override
    public Atelier getMatchEntityById(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Atelier introuvable avec l'identifiant : " + id));
    }

    @Override
    public AtelierDto getMatchDtoById(Long id) {
        return matchMapper.toAtelierDto(getMatchEntityById(id));
    }

    @Override
    @Transactional
    public AtelierDto updateMatch(Long matchId, AtelierRequest request) {
        Atelier match = getMatchEntityById(matchId);

        // Regle metier : un match annule ne peut plus etre modifie.
        if (match.getStatut() == StatutAtelier.ANNULE) {
            throw new BusinessException("Impossible de modifier un atelier annulé.");
        }
        // Regle metier : seul l'organisateur peut modifier son match.
        if (!match.getOrganisateur().getId().equals(request.getOrganisateurId())) {
            throw new BusinessException("Seul l'organisateur peut modifier cet atelier.");
        }

        Salle terrain = terrainService.getById(request.getTerrainId());
        // Modifier un atelier ne doit pas contourner les contrôles de sa création.
        validateBookingDelay(match.getOrganisateur(), request.getDate());
        validateSiteAccessForOrganizer(match.getOrganisateur(), terrain);

        LocalDateTime dateDebut = request.getDate().atTime(request.getHeureDebut());
        if (!dateDebut.isAfter(LocalDateTime.now())) {
            throw new BusinessException("Un atelier ne peut pas être déplacé dans le passé.");
        }
        LocalDateTime dateFin = dateDebut.plusMinutes(AtelierRules.DUREE_MINUTES);

        validateSiteNotClosed(terrain, dateDebut.toLocalDate());
        validateSiteOpeningHours(terrain, dateDebut.toLocalTime(), dateFin.toLocalTime());

        // Regle metier : la modification ne doit pas creer de chevauchement de creneau.
        if (!isSlotAvailableExcluding(terrain, dateDebut, dateFin, matchId)) {
            throw new BusinessException("Ce créneau est déjà réservé pour la salle : " + terrain.getId());
        }

        match.setTerrain(terrain);
        match.setDateDebut(dateDebut);
        match.setDateFin(dateFin);
        match.setTypeMatch(request.getTypeMatch());

        return matchMapper.toAtelierDto(matchRepository.save(match));
    }

    @Override
    @Transactional
    public void cancelMatch(Long matchId, Long requesterId) {
        Atelier match = getMatchEntityById(matchId);

        // Regle metier : un match deja annule ne peut pas etre annule une seconde fois.
        if (match.getStatut() == StatutAtelier.ANNULE) {
            throw new BusinessException("Cet atelier est déjà annulé.");
        }
        // Regle metier : seul l'organisateur peut annuler son match.
        if (!match.getOrganisateur().getId().equals(requesterId)) {
            throw new BusinessException("Seul l'organisateur peut annuler cet atelier.");
        }

        match.setStatut(StatutAtelier.ANNULE);
        matchRepository.save(match);
        log.info("Atelier {} annulé par l'organisateur {}", matchId, requesterId);
    }

    @Override
    @Transactional
    public void checkAndConvertExpiredPrivateAteliers() {
        LocalDate openingDay = LocalDate.now().plusDays(AtelierRules.OUVERTURE_PUBLIQUE_JOURS);
        LocalDateTime startOfDay = openingDay.atStartOfDay();
        LocalDateTime endOfDay = openingDay.atTime(LocalTime.MAX);

        // À J-2 : conversion uniquement des ateliers privés planifiés,
        // incomplets et avec accord explicite enregistré de l'organisateur.
        List<Atelier> expiredMatches = matchRepository
                .findByDateDebutBetweenAndStatut(startOfDay, endOfDay, StatutAtelier.PLANIFIE)
                .stream()
                .filter(m -> m.getTypeMatch() == TypeAtelier.PRIVE
                        && Boolean.TRUE.equals(m.getAccordOuverturePublique())
                        && (m.getNbJoueursActuels() == null
                                || m.getNbJoueursActuels() < AtelierRules.CAPACITE))
                .toList();
        // Le scheduler applique la même validation métier que l'API.
        if (!expiredMatches.isEmpty()) {
            expiredMatches.forEach(m -> convertToPublic(m.getId()));
            log.info("Scheduler : {} atelier(s) privé(s) ouvert(s) au public", expiredMatches.size());
        }
    }


    @Override
    @Transactional
    public void convertToPublic(Long matchId) {
        Atelier match = getMatchEntityById(matchId);
        // Regle metier : un match deja public ne peut pas etre converti en public.
        if (match.getTypeMatch() == TypeAtelier.PUBLIC) {
            throw new BusinessException("Cet atelier est déjà public.");
        }
        if (!Boolean.TRUE.equals(match.getAccordOuverturePublique())) {
            throw new BusinessException("L'accord explicite de l'organisateur est requis pour ouvrir cet atelier au public.");
        }
        if (match.getStatut() != StatutAtelier.PLANIFIE || !match.getDateDebut().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Seul un atelier privé planifié à venir peut être ouvert au public.");
        }
        if (match.getDateDebut().toLocalDate().isAfter(LocalDate.now().plusDays(AtelierRules.OUVERTURE_PUBLIQUE_JOURS))) {
            throw new BusinessException("L'ouverture au public est possible à partir de J-2.");
        }
        match.setTypeMatch(TypeAtelier.PUBLIC);
        match.setDateConversionPublic(LocalDateTime.now());
        matchRepository.save(match);
        log.info("Atelier {} ouvert au public après accord de l'organisateur", matchId);
    }

    @Override
    @Transactional
    public void consentToPublicOpening(Long matchId, Long requesterId) {
        Atelier match = getMatchEntityById(matchId);
        if (!match.getOrganisateur().getId().equals(requesterId)) {
            throw new BusinessException("Seul l'organisateur peut autoriser l'ouverture au public.");
        }
        if (match.getTypeMatch() != TypeAtelier.PRIVE || match.getStatut() != StatutAtelier.PLANIFIE
                || !match.getDateDebut().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Cet atelier ne peut pas recevoir un accord d'ouverture au public.");
        }
        if (Boolean.TRUE.equals(match.getAccordOuverturePublique())) {
            throw new BusinessException("L'accord d'ouverture au public est déjà enregistré.");
        }
        match.setAccordOuverturePublique(true);
        match.setDateAccordOuverturePublique(LocalDateTime.now());
        matchRepository.save(match);
    }

    @Override
    @Transactional
    public void incrementPlayers(Long matchId) {
        Atelier match = getMatchEntityById(matchId);
        incrementPlayersForMatch(match, true);
    }

    private void incrementPlayersForMatch(Atelier match, boolean save) {
        // Regle metier : un match de padel doit contenir exactement 4 joueurs.
        // Des que les 4 places sont occupees, le match devient complet.
        if (match.getNbJoueursActuels() >= AtelierRules.CAPACITE) {
            throw new BusinessException("Cet atelier est déjà complet.");
        }
        match.setNbJoueursActuels(match.getNbJoueursActuels() + 1);
        if (match.getNbJoueursActuels() == AtelierRules.CAPACITE) {
            match.setStatut(StatutAtelier.COMPLET);
        }
        if (save) {
            matchRepository.save(match);
        }
    }

    @Override
    @Transactional
    public void decrementPlayers(Long matchId) {
        // Le paiement et l'annulation ajustent le compteur sous le même verrou d'atelier.
        Atelier match = matchRepository.findByIdForUpdate(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Atelier introuvable avec l'ID : " + matchId));
        // Regle metier : impossible de retirer un joueur d'un match vide.
        if (match.getNbJoueursActuels() <= 0) {
            throw new BusinessException("Cet atelier ne comporte aucun participant confirmé.");
        }
        match.setNbJoueursActuels(match.getNbJoueursActuels() - 1);
        if (match.getStatut() == StatutAtelier.COMPLET) {
            match.setStatut(StatutAtelier.PLANIFIE);
        }
        matchRepository.save(match);
    }
    //la règle des 15 minutes entre deux matchs
    private boolean isSlotAvailable(Salle terrain, LocalDateTime start, LocalDateTime end) {
        int breakMinutes = AtelierRules.PAUSE_MINUTES;
        List<Atelier> existingMatches = matchRepository.findOverlappingAteliers(
                terrain.getId(),
                start.minusMinutes(breakMinutes),
                end.plusMinutes(breakMinutes),
                StatutAtelier.ANNULE
        );
        return existingMatches.isEmpty();
    }

    private boolean isSlotAvailableExcluding(Salle terrain, LocalDateTime start, LocalDateTime end, Long excludedMatchId) {
        int breakMinutes = AtelierRules.PAUSE_MINUTES;
        List<Atelier> existingMatches = matchRepository.findOverlappingAteliers(
                terrain.getId(),
                start.minusMinutes(breakMinutes),
                end.plusMinutes(breakMinutes),
                StatutAtelier.ANNULE
        );
        return existingMatches.stream().allMatch(m -> m.getId().equals(excludedMatchId));
    }

    private void validateBookingDelay(Participant membre, LocalDate matchDate) {
        long daysUntilMatch = ChronoUnit.DAYS.between(LocalDate.now(), matchDate);
        int maxDaysAhead = AtelierRules.delaiReservation(membre.getTypeMembre());

        // Regle metier : la date du match ne peut pas etre dans le passe.
        if (daysUntilMatch < 0) {
            throw new BusinessException("La date de l'atelier est déjà passée.");
        }
        // Regle metier : le delai de reservation depend du type de membre.
        if (daysUntilMatch > maxDaysAhead) {
            throw new BusinessException("Le participant " + membre.getTypeMembre()
                    + " ne peut pas réserver plus de " + maxDaysAhead + " jours à l'avance.");
        }
    }
    //methode fermeture globaux de site
    private void validateSiteNotClosed(Salle terrain, LocalDate date) {
        boolean closedGlobally = jourFermetureRepository.existsByDateAndGlobalTrue(date);
        boolean closedForSite = jourFermetureRepository.existsByDateAndSiteId(date, terrain.getSite().getId());
        // Regle metier : aucun match ne peut etre cree sur un site ferme .jour fermeture globaux.
        if (closedGlobally || closedForSite) {
            throw new BusinessException("Le centre est fermé à la date : " + date);
        }
    }
        //methode les horaires d'ouverture propre pour un site
    private void validateSiteOpeningHours(Salle terrain, LocalTime heureDebut, LocalTime heureFin) {
        LocalTime openingTime = terrain.getSite().getHeureOuverture();
        LocalTime closingTime = terrain.getSite().getHeureFermeture();
        // Regle metier : le match doit respecter les horaires d'ouverture du site.
        if (heureDebut.isBefore(openingTime) || heureFin.isAfter(closingTime)) {
            throw new BusinessException("L'atelier est en dehors des horaires d'ouverture du centre (" + openingTime + " - " + closingTime + ").");
        }
    }

    private void validateSiteAccessForOrganizer(Participant organisateur, Salle terrain) {
        if (organisateur.getTypeMembre() == TypeParticipant.SITE) {
            // Regle metier : un membre SITE ne peut creer un match que sur son propre site.
            if (organisateur.getSite() == null || !organisateur.getSite().getId().equals(terrain.getSite().getId())) {
                throw new BusinessException("Un abonné ne peut créer un atelier que dans son centre de rattachement.");
            }
        }
        if (organisateur.getTypeMembre() == TypeParticipant.LIBRE
                && !Boolean.TRUE.equals(terrain.getSite().getOuvertVisiteurs())) {
            throw new BusinessException("Ce centre n'est pas ouvert aux visiteurs.");
        }
    }

    private void createPendingReservationForOrganizer(Atelier match, Participant organisateur) {
        Reservation reservation = Reservation.builder()
                .match(match)
                .membre(organisateur)
                .statut(StatutReservation.EN_ATTENTE)
                .build();
        reservation = reservationRepository.save(reservation);

        Paiement paiement = Paiement.builder()
                .reservation(reservation)
                .montant(match.getPrixParJoueur())
                .statut(StatutPaiement.EN_ATTENTE)
                .build();
        paiement = paiementRepository.save(paiement);
        reservation.setPaiement(paiement);
    }
}
