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
import jakarta.persistence.EntityManager;
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

    private final AtelierRepository atelierRepository;
    private final ParticipantRepository participantRepository;
    private final ReservationRepository reservationRepository;
    private final PaiementRepository paiementRepository;
    private final JourFermetureRepository jourFermetureRepository;
    private final SalleService salleService;
    private final ParticipantService participantService;
    private final AtelierMapper atelierMapper;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public AtelierDto createAtelier(CreateAtelierRequest request, String username) {
        Participant organisateur = participantRepository.findByMatriculeIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Participant introuvable pour l'utilisateur : " + username));

        // Serialize slot validation and insertion for this room, including the break.
        Salle salle = salleService.getByIdForUpdate(request.salleId());

        // Regle metier : un participant avec un solde impaye ne peut pas creer de atelier.
        if (participantService.hasOutstandingBalance(organisateur.getId())) {
            throw new BusinessException("Vous avez un solde impayé et ne pouvez pas créer d'atelier.");
        }
        // Regle metier : un participant penalise ne peut pas creer de atelier.
        if (participantService.hasActivePenalty(organisateur.getId())) {
            throw new BusinessException("Vous avez une suspension active et ne pouvez pas créer d'atelier.");
        }

        validateBookingDelay(organisateur, request.atelierDate().toLocalDate());
        validateCentreAccessForOrganizer(organisateur, salle);
        //Service metier debut et fin atelier
        LocalDateTime dateDebut = request.atelierDate();
        if (dateDebut.isBefore(LocalDateTime.now())) {
            throw new BusinessException("Un atelier ne peut pas être créé dans le passé.");
        }
        LocalDateTime dateFin = dateDebut.plusMinutes(AtelierRules.DUREE_MINUTES);

        validateCentreNotClosed(salle, dateDebut.toLocalDate());
        validateCentreOpeningHours(salle, dateDebut.toLocalTime(), dateFin.toLocalTime());

        // Regle metier : une salle ne peut pas avoir deux atelier sur le meme creneau horaire.
        if (!isSlotAvailable(salle, dateDebut, dateFin)) {
            throw new BusinessException("Ce créneau est déjà réservé pour la salle : " + salle.getId());
        }

        Atelier atelier = new Atelier();
        atelier.setOrganisateur(organisateur);
        atelier.setSalle(salle);
        atelier.setDateDebut(dateDebut);
        atelier.setDateFin(dateFin);
        atelier.setTypeAtelier(TypeAtelier.valueOf(request.atelierType()));
        atelier.setStatut(StatutAtelier.PLANIFIE);
        atelier.setNbParticipantsActuels(0);
        atelier.setPrixTotal(AtelierRules.PRIX_TOTAL);
        atelier.setPrixParParticipant(AtelierRules.PRIX_PARTICIPANT);

        Atelier savedAtelier = atelierRepository.save(atelier);
        // Regle metier : l'organisateur commence avec une reservation en attente et un paiement associe.
        createPendingReservationForOrganizer(savedAtelier, organisateur);
        log.info("Atelier créé avec succès (ID: {}) par l'utilisateur {}", savedAtelier.getId(), username);

        return atelierMapper.toAtelierDto(savedAtelier);
    }

    @Override
    public List<AtelierDto> findAllAteliers() {
        return atelierRepository.findAll().stream()
                .map(atelierMapper::toAtelierDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AtelierDto> getPublicAvailableAteliers() {
        return atelierRepository.findByTypeAtelierAndStatut(TypeAtelier.PUBLIC, StatutAtelier.PLANIFIE)
                .stream()
                .map(atelierMapper::toAtelierDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AtelierDto> findByOrganisateur(Long organisateurId) {
        return atelierRepository.findByOrganisateurId(organisateurId)
                .stream()
                .map(atelierMapper::toAtelierDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AtelierDto> findByCentre(Long centreId) {
        return atelierRepository.findBySalleCentreId(centreId)
                .stream()
                .map(atelierMapper::toAtelierDto)
                .collect(Collectors.toList());
    }

    @Override
    public Atelier getAtelierEntityById(Long id) {
        return atelierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Atelier introuvable avec l'identifiant : " + id));
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Atelier getAtelierEntityByIdForUpdate(Long id) {
        Atelier atelier = atelierRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Atelier introuvable avec l'identifiant : " + id));
        // A scheduler or controller may have loaded the entity before a concurrent commit.
        entityManager.refresh(atelier);
        return atelier;
    }

    @Override
    public AtelierDto getAtelierDtoById(Long id) {
        return atelierMapper.toAtelierDto(getAtelierEntityById(id));
    }

    @Override
    @Transactional
    public AtelierDto updateAtelier(Long atelierId, AtelierRequest request) {
        Atelier atelier = getAtelierEntityByIdForUpdate(atelierId);

        // Regle metier : un atelier annule ne peut plus etre modifie.
        if (atelier.getStatut() == StatutAtelier.ANNULE) {
            throw new BusinessException("Impossible de modifier un atelier annulé.");
        }
        // Regle metier : seul l'organisateur peut modifier son atelier.
        if (!atelier.getOrganisateur().getId().equals(request.getOrganisateurId())) {
            throw new BusinessException("Seul l'organisateur peut modifier cet atelier.");
        }

        Salle salle = salleService.getByIdForUpdate(request.getSalleId());
        // Modifier un atelier ne doit pas contourner les contrôles de sa création.
        validateBookingDelay(atelier.getOrganisateur(), request.getDate());
        validateCentreAccessForOrganizer(atelier.getOrganisateur(), salle);

        LocalDateTime dateDebut = request.getDate().atTime(request.getHeureDebut());
        if (!dateDebut.isAfter(LocalDateTime.now())) {
            throw new BusinessException("Un atelier ne peut pas être déplacé dans le passé.");
        }
        LocalDateTime dateFin = dateDebut.plusMinutes(AtelierRules.DUREE_MINUTES);

        validateCentreNotClosed(salle, dateDebut.toLocalDate());
        validateCentreOpeningHours(salle, dateDebut.toLocalTime(), dateFin.toLocalTime());

        // Regle metier : la modification ne doit pas creer de chevauchement de creneau.
        if (!isSlotAvailableExcluding(salle, dateDebut, dateFin, atelierId)) {
            throw new BusinessException("Ce créneau est déjà réservé pour la salle : " + salle.getId());
        }

        atelier.setSalle(salle);
        atelier.setDateDebut(dateDebut);
        atelier.setDateFin(dateFin);
        atelier.setTypeAtelier(request.getTypeAtelier());

        return atelierMapper.toAtelierDto(atelierRepository.save(atelier));
    }

    @Override
    @Transactional
    public void cancelAtelier(Long atelierId, Long requesterId) {
        Atelier atelier = getAtelierEntityByIdForUpdate(atelierId);

        // Regle metier : un atelier deja annule ne peut pas etre annule une seconde fois.
        if (atelier.getStatut() == StatutAtelier.ANNULE) {
            throw new BusinessException("Cet atelier est déjà annulé.");
        }
        // Regle metier : seul l'organisateur peut annuler son atelier.
        if (!atelier.getOrganisateur().getId().equals(requesterId)) {
            throw new BusinessException("Seul l'organisateur peut annuler cet atelier.");
        }

        atelier.setStatut(StatutAtelier.ANNULE);
        atelierRepository.save(atelier);
        log.info("Atelier {} annulé par l'organisateur {}", atelierId, requesterId);
    }

    @Override
    @Transactional
    public void checkAndConvertExpiredPrivateAteliers() {
        LocalDate openingDay = LocalDate.now().plusDays(AtelierRules.OUVERTURE_PUBLIQUE_JOURS);
        LocalDateTime startOfDay = openingDay.atStartOfDay();
        LocalDateTime endOfDay = openingDay.atTime(LocalTime.MAX);

        // À J-2 : conversion uniquement des ateliers privés planifiés,
        // incomplets et avec accord explicite enregistré de l'organisateur.
        List<Atelier> expiredAteliers = atelierRepository
                .findByDateDebutBetweenAndStatut(startOfDay, endOfDay, StatutAtelier.PLANIFIE)
                .stream()
                .filter(m -> m.getTypeAtelier() == TypeAtelier.PRIVE
                        && Boolean.TRUE.equals(m.getAccordOuverturePublique())
                        && m.getDateAccordOuverturePublique() != null
                        && (m.getNbParticipantsActuels() == null
                                || m.getNbParticipantsActuels() < AtelierRules.CAPACITE))
                .toList();
        // Le scheduler applique la même validation métier que l'API.
        if (!expiredAteliers.isEmpty()) {
            expiredAteliers.forEach(m -> convertToPublic(m.getId()));
            log.info("Scheduler : {} atelier(s) privé(s) ouvert(s) au public", expiredAteliers.size());
        }
    }


    @Override
    @Transactional
    public void convertToPublic(Long atelierId) {
        Atelier atelier = getAtelierEntityByIdForUpdate(atelierId);
        // Regle metier : un atelier deja public ne peut pas etre converti en public.
        if (atelier.getTypeAtelier() == TypeAtelier.PUBLIC) {
            throw new BusinessException("Cet atelier est déjà public.");
        }
        if (!Boolean.TRUE.equals(atelier.getAccordOuverturePublique())
                || atelier.getDateAccordOuverturePublique() == null) {
            throw new BusinessException("L'accord explicite de l'organisateur est requis pour ouvrir cet atelier au public.");
        }
        if (atelier.getStatut() != StatutAtelier.PLANIFIE || !atelier.getDateDebut().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Seul un atelier privé planifié à venir peut être ouvert au public.");
        }
        if (atelier.getNbParticipantsActuels() != null && atelier.getNbParticipantsActuels() >= AtelierRules.CAPACITE) {
            throw new BusinessException("Un atelier complet ne peut pas être ouvert au public.");
        }
        if (atelier.getDateDebut().toLocalDate().isAfter(LocalDate.now().plusDays(AtelierRules.OUVERTURE_PUBLIQUE_JOURS))) {
            throw new BusinessException("L'ouverture au public est possible à partir de J-2.");
        }
        atelier.setTypeAtelier(TypeAtelier.PUBLIC);
        atelier.setDateConversionPublic(LocalDateTime.now());
        atelierRepository.save(atelier);
        log.info("Atelier {} ouvert au public après accord de l'organisateur", atelierId);
    }

    @Override
    @Transactional
    public void consentToPublicOpening(Long atelierId, Long requesterId) {
        Atelier atelier = getAtelierEntityByIdForUpdate(atelierId);
        if (!atelier.getOrganisateur().getId().equals(requesterId)) {
            throw new BusinessException("Seul l'organisateur peut autoriser l'ouverture au public.");
        }
        if (atelier.getTypeAtelier() != TypeAtelier.PRIVE || atelier.getStatut() != StatutAtelier.PLANIFIE
                || !atelier.getDateDebut().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Cet atelier ne peut pas recevoir un accord d'ouverture au public.");
        }
        if (Boolean.TRUE.equals(atelier.getAccordOuverturePublique())) {
            throw new BusinessException("L'accord d'ouverture au public est déjà enregistré.");
        }
        atelier.setAccordOuverturePublique(true);
        atelier.setDateAccordOuverturePublique(LocalDateTime.now());
        atelierRepository.save(atelier);
    }

    @Override
    @Transactional
    public void incrementParticipants(Long atelierId) {
        Atelier atelier = getAtelierEntityByIdForUpdate(atelierId);
        incrementParticipantsForAtelier(atelier, true);
    }

    private void incrementParticipantsForAtelier(Atelier atelier, boolean save) {
        // Regle metier : un atelier collaboratif doit contenir au maximum 6 participants.
        // Des que les 6 places sont occupees, l'atelier devient complet.
        if (atelier.getNbParticipantsActuels() >= AtelierRules.CAPACITE) {
            throw new BusinessException("Cet atelier est déjà complet.");
        }
        atelier.setNbParticipantsActuels(atelier.getNbParticipantsActuels() + 1);
        if (atelier.getNbParticipantsActuels() == AtelierRules.CAPACITE) {
            atelier.setStatut(StatutAtelier.COMPLET);
        }
        if (save) {
            atelierRepository.save(atelier);
        }
    }

    @Override
    @Transactional
    public void decrementParticipants(Long atelierId) {
        // Le paiement et l'annulation ajustent le compteur sous le même verrou d'atelier.
        Atelier atelier = getAtelierEntityByIdForUpdate(atelierId);
        // Regle metier : impossible de retirer un participant d'un atelier vide.
        if (atelier.getNbParticipantsActuels() <= 0) {
            throw new BusinessException("Cet atelier ne comporte aucun participant confirmé.");
        }
        atelier.setNbParticipantsActuels(atelier.getNbParticipantsActuels() - 1);
        if (atelier.getStatut() == StatutAtelier.COMPLET) {
            atelier.setStatut(StatutAtelier.PLANIFIE);
        }
        atelierRepository.save(atelier);
    }
    //la règle des 20 minutes entre deux atelier
    private boolean isSlotAvailable(Salle salle, LocalDateTime start, LocalDateTime end) {
        int breakMinutes = AtelierRules.PAUSE_MINUTES;
        List<Atelier> existingAteliers = atelierRepository.findOverlappingAteliers(
                salle.getId(),
                start.minusMinutes(breakMinutes),
                end.plusMinutes(breakMinutes),
                StatutAtelier.ANNULE
        );
        return existingAteliers.isEmpty();
    }

    private boolean isSlotAvailableExcluding(Salle salle, LocalDateTime start, LocalDateTime end, Long excludedAtelierId) {
        int breakMinutes = AtelierRules.PAUSE_MINUTES;
        List<Atelier> existingAteliers = atelierRepository.findOverlappingAteliers(
                salle.getId(),
                start.minusMinutes(breakMinutes),
                end.plusMinutes(breakMinutes),
                StatutAtelier.ANNULE
        );
        return existingAteliers.stream().allMatch(m -> m.getId().equals(excludedAtelierId));
    }

    private void validateBookingDelay(Participant participant, LocalDate atelierDate) {
        long daysUntilAtelier = ChronoUnit.DAYS.between(LocalDate.now(), atelierDate);
        int maxDaysAhead = AtelierRules.delaiReservation(participant.getTypeParticipant());

        // Regle metier : la date de l'atelier ne peut pas etre dans le passe.
        if (daysUntilAtelier < 0) {
            throw new BusinessException("La date de l'atelier est déjà passée.");
        }
        // Regle metier : le delai de reservation depend du type de participant.
        if (daysUntilAtelier > maxDaysAhead) {
            throw new BusinessException("Le participant " + participant.getTypeParticipant()
                    + " ne peut pas réserver plus de " + maxDaysAhead + " jours à l'avance.");
        }
    }
    //methode fermeture globaux de centre
    private void validateCentreNotClosed(Salle salle, LocalDate date) {
        boolean closedGlobally = jourFermetureRepository.existsByDateAndGlobalTrue(date);
        boolean closedForCentre = jourFermetureRepository.existsByDateAndCentreId(date, salle.getCentre().getId());
        // Regle metier : aucun atelier ne peut etre cree sur un centre ferme .jour fermeture globaux.
        if (closedGlobally || closedForCentre) {
            throw new BusinessException("Le centre est fermé à la date : " + date);
        }
    }
        //methode les horaires d'ouverture propre pour un centre
    private void validateCentreOpeningHours(Salle salle, LocalTime heureDebut, LocalTime heureFin) {
        LocalTime openingTime = salle.getCentre().getHeureOuverture();
        LocalTime closingTime = salle.getCentre().getHeureFermeture();
        // Regle metier : l'atelier doit respecter les horaires d'ouverture du centre.
        if (heureDebut.isBefore(openingTime) || heureFin.isAfter(closingTime)
                || (openingTime.isBefore(closingTime) && heureFin.isBefore(heureDebut))) {
            throw new BusinessException("L'atelier est en dehors des horaires d'ouverture du centre (" + openingTime + " - " + closingTime + ").");
        }
    }

    private void validateCentreAccessForOrganizer(Participant organisateur, Salle salle) {
        if (organisateur.getTypeParticipant() == TypeParticipant.ABONNE) {
            // Regle metier : un participant SITE ne peut creer un atelier que sur son propre centre.
            if (organisateur.getCentre() == null || !organisateur.getCentre().getId().equals(salle.getCentre().getId())) {
                throw new BusinessException("Un abonné ne peut créer un atelier que dans son centre de rattachement.");
            }
        }
        if (organisateur.getTypeParticipant() == TypeParticipant.VISITEUR
                && !Boolean.TRUE.equals(salle.getCentre().getOuvertVisiteurs())) {
            throw new BusinessException("Ce centre n'est pas ouvert aux visiteurs.");
        }
    }

    private void createPendingReservationForOrganizer(Atelier atelier, Participant organisateur) {
        Reservation reservation = Reservation.builder()
                .atelier(atelier)
                .participant(organisateur)
                .statut(StatutReservation.EN_ATTENTE)
                .build();
        reservation = reservationRepository.save(reservation);

        Paiement paiement = Paiement.builder()
                .reservation(reservation)
                .montant(atelier.getPrixParParticipant())
                .statut(StatutPaiement.EN_ATTENTE)
                .build();
        paiement = paiementRepository.save(paiement);
        reservation.setPaiement(paiement);
    }
}
