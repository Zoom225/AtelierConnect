package com.padell.padell.service.impl;

import com.padell.padell.dto.request.MatchRequest;
import com.padell.padell.entity.*;
import com.padell.padell.entity.enums.*;
import com.padell.padell.exception.BusinessException;
import com.padell.padell.exception.ResourceNotFoundException;
import com.padell.padell.mapper.MatchMapper;
import com.padell.padell.dto.request.CreateMatchRequest;
import com.padell.padell.dto.response.MatchDto;
import com.padell.padell.repository.*;
import com.padell.padell.service.AtelierService;
import com.padell.padell.service.ParticipantService;
import com.padell.padell.service.SalleService;
import com.padell.padell.service.AtelierRules;
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
    private final MatchMapper matchMapper;

    @Override
    @Transactional
    public MatchDto createMatch(CreateMatchRequest request, String username) {
        Participant organisateur = membreRepository.findByMatriculeIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Membre non trouvé pour l'utilisateur: " + username));

        Salle terrain = terrainService.getById(request.terrainId());

        // Regle metier : un membre avec un solde impaye ne peut pas creer de match.
        if (membreService.hasOutstandingBalance(organisateur.getId())) {
            throw new BusinessException("Vous avez un solde impayé et ne pouvez pas créer de match.");
        }
        // Regle metier : un membre penalise ne peut pas creer de match.
        if (membreService.hasActivePenalty(organisateur.getId())) {
            throw new BusinessException("Vous avez une pénalité active et ne pouvez pas créer de match.");
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
            throw new BusinessException("Ce créneau est déjà réservé sur le terrain : " + terrain.getId());
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
        log.info("Match créé avec succès (ID: {}) par l'utilisateur {}", savedMatch.getId(), username);

        return matchMapper.toMatchDto(savedMatch);
    }

    @Override
    public List<MatchDto> findAllMatches() {
        return matchRepository.findAll().stream()
                .map(matchMapper::toMatchDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<MatchDto> getPublicAvailableMatches() {
        return matchRepository.findByTypeAtelierAndStatut(TypeAtelier.PUBLIC, StatutAtelier.PLANIFIE)
                .stream()
                .map(matchMapper::toMatchDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<MatchDto> findByOrganisateur(Long organisateurId) {
        return matchRepository.findByOrganisateurId(organisateurId)
                .stream()
                .map(matchMapper::toMatchDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<MatchDto> findBySite(Long siteId) {
        return matchRepository.findBySalleCentreId(siteId)
                .stream()
                .map(matchMapper::toMatchDto)
                .collect(Collectors.toList());
    }

    @Override
    public Atelier getMatchEntityById(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Match non trouvé avec l'ID : " + id));
    }

    @Override
    public MatchDto getMatchDtoById(Long id) {
        return matchMapper.toMatchDto(getMatchEntityById(id));
    }

    @Override
    @Transactional
    public MatchDto updateMatch(Long matchId, MatchRequest request) {
        Atelier match = getMatchEntityById(matchId);

        // Regle metier : un match annule ne peut plus etre modifie.
        if (match.getStatut() == StatutAtelier.ANNULE) {
            throw new BusinessException("Impossible de modifier un match annulé.");
        }
        // Regle metier : seul l'organisateur peut modifier son match.
        if (!match.getOrganisateur().getId().equals(request.getOrganisateurId())) {
            throw new BusinessException("Seul l'organisateur peut modifier ce match.");
        }

        Salle terrain = terrainService.getById(request.getTerrainId());
        LocalDateTime dateDebut = request.getDate().atTime(request.getHeureDebut());
        LocalDateTime dateFin = dateDebut.plusMinutes(AtelierRules.DUREE_MINUTES);

        validateSiteNotClosed(terrain, dateDebut.toLocalDate());
        validateSiteOpeningHours(terrain, dateDebut.toLocalTime(), dateFin.toLocalTime());

        // Regle metier : la modification ne doit pas creer de chevauchement de creneau.
        if (!isSlotAvailableExcluding(terrain, dateDebut, dateFin, matchId)) {
            throw new BusinessException("Ce créneau est déjà réservé sur le terrain : " + terrain.getId());
        }

        match.setTerrain(terrain);
        match.setDateDebut(dateDebut);
        match.setDateFin(dateFin);
        match.setTypeMatch(request.getTypeMatch());

        return matchMapper.toMatchDto(matchRepository.save(match));
    }

    @Override
    @Transactional
    public void cancelMatch(Long matchId, Long requesterId) {
        Atelier match = getMatchEntityById(matchId);

        // Regle metier : un match deja annule ne peut pas etre annule une seconde fois.
        if (match.getStatut() == StatutAtelier.ANNULE) {
            throw new BusinessException("Ce match est déjà annulé.");
        }
        // Regle metier : seul l'organisateur peut annuler son match.
        if (!match.getOrganisateur().getId().equals(requesterId)) {
            throw new BusinessException("Seul l'organisateur peut annuler ce match.");
        }

        match.setStatut(StatutAtelier.ANNULE);
        matchRepository.save(match);
        log.info("Match {} annulé par l'organisateur {}", matchId, requesterId);
    }

    @Override
    @Transactional
    public void checkAndConvertExpiredPrivateMatches() {
        LocalDate openingDay = LocalDate.now().plusDays(AtelierRules.OUVERTURE_PUBLIQUE_JOURS);
        LocalDateTime startOfDay = openingDay.atStartOfDay();
        LocalDateTime endOfDay = openingDay.atTime(LocalTime.MAX);

        // Regle metier : a J-1, un match prive incomplet redevient public et penalise son organisateur.
        List<Atelier> expiredMatches = matchRepository
                .findByDateDebutBetweenAndStatut(startOfDay, endOfDay, StatutAtelier.PLANIFIE)
                .stream()
                .filter(m -> m.getTypeMatch() == TypeAtelier.PRIVE
                        && Boolean.TRUE.equals(m.getAccordOuverturePublique())
                        && m.getNbJoueursActuels() < AtelierRules.CAPACITE)
                .toList();
        //sheduler active converti en public la veille
        if (!expiredMatches.isEmpty()) {
            expiredMatches.forEach(m -> convertToPublic(m.getId()));
            log.info("Scheduler : {} match(s) privé(s) ont été converti(s) en public", expiredMatches.size());
        }
    }


    @Override
    @Transactional
    public void convertToPublic(Long matchId) {
        Atelier match = getMatchEntityById(matchId);
        // Regle metier : un match deja public ne peut pas etre converti en public.
        if (match.getTypeMatch() == TypeAtelier.PUBLIC) {
            throw new BusinessException("Le match est déjà public.");
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
            throw new BusinessException("Le match est déjà complet.");
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
        Atelier match = getMatchEntityById(matchId);
        // Regle metier : impossible de retirer un joueur d'un match vide.
        if (match.getNbJoueursActuels() <= 0) {
            throw new BusinessException("Le match n'a aucun joueur.");
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
            throw new BusinessException("La date du match est déjà passée.");
        }
        // Regle metier : le delai de reservation depend du type de membre.
        if (daysUntilMatch > maxDaysAhead) {
            throw new BusinessException("Le membre " + membre.getTypeMembre()
                    + " ne peut pas réserver plus de " + maxDaysAhead + " jours à l'avance.");
        }
    }
    //methode fermeture globaux de site
    private void validateSiteNotClosed(Salle terrain, LocalDate date) {
        boolean closedGlobally = jourFermetureRepository.existsByDateAndGlobalTrue(date);
        boolean closedForSite = jourFermetureRepository.existsByDateAndSiteId(date, terrain.getSite().getId());
        // Regle metier : aucun match ne peut etre cree sur un site ferme .jour fermeture globaux.
        if (closedGlobally || closedForSite) {
            throw new BusinessException("Le site est fermé à la date : " + date);
        }
    }
        //methode les horaires d'ouverture propre pour un site
    private void validateSiteOpeningHours(Salle terrain, LocalTime heureDebut, LocalTime heureFin) {
        LocalTime openingTime = terrain.getSite().getHeureOuverture();
        LocalTime closingTime = terrain.getSite().getHeureFermeture();
        // Regle metier : le match doit respecter les horaires d'ouverture du site.
        if (heureDebut.isBefore(openingTime) || heureFin.isAfter(closingTime)) {
            throw new BusinessException("Le match est en dehors des heures d'ouverture du site (" + openingTime + " - " + closingTime + ").");
        }
    }

    private void validateSiteAccessForOrganizer(Participant organisateur, Salle terrain) {
        if (organisateur.getTypeMembre() == TypeParticipant.SITE) {
            // Regle metier : un membre SITE ne peut creer un match que sur son propre site.
            if (organisateur.getSite() == null || !organisateur.getSite().getId().equals(terrain.getSite().getId())) {
                throw new BusinessException("Un membre SITE ne peut créer un match que sur son propre site.");
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
