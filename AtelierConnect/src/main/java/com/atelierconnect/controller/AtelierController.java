package com.atelierconnect.controller;

import com.atelierconnect.dto.request.AtelierRequest;
import com.atelierconnect.entity.Participant;
import com.atelierconnect.dto.request.CreateAtelierRequest;
import com.atelierconnect.dto.response.AtelierDto;
import com.atelierconnect.entity.enums.TypeAtelier;
import com.atelierconnect.repository.ReservationRepository;
import com.atelierconnect.service.AtelierService;
import com.atelierconnect.service.impl.AdminAuthorizationService;
import com.atelierconnect.service.impl.CurrentParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/ateliers", "/api/matches"})
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ateliers", description = "Gestion des ateliers collaboratifs ; /api/matches reste un alias transitoire.")
public class AtelierController {

    private final AtelierService matchService;
    private final AdminAuthorizationService adminAuthorizationService;
    private final CurrentParticipantService currentMemberService;
    private final ReservationRepository reservationRepository;

    @Operation(
            summary = "Créer un atelier",
            description = "Crée un atelier PUBLIC ou PRIVÉ pour le participant connecté. " +
                    "Les délais de réservation PARTENAIRE, ABONNE et VISITEUR s'appliquent à la création de l'atelier. " +
                    "Accessible uniquement à un participant authentifié.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @PostMapping
    public ResponseEntity<AtelierDto> createMatch(@Valid @RequestBody CreateAtelierRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        log.info("Requête de création d'atelier reçue de l'utilisateur '{}' pour la salle ID {}", username, request.terrainId());

        if (username == null || "anonymousUser".equals(username)) {
            log.warn("Tentative de création d'atelier par un utilisateur non authentifié.");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        AtelierDto createdMatch = matchService.createMatch(request, username);
        return new ResponseEntity<>(createdMatch, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Lister tous les ateliers",
            description = "Retourne les ateliers visibles pour l'administrateur connecté. " +
                    "Un administrateur global voit tous les ateliers ; un administrateur de centre voit seulement ceux de son centre.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @GetMapping
    public ResponseEntity<List<AtelierDto>> getAllMatches() {
        return ResponseEntity.ok(adminAuthorizationService.filterMatchDtos(matchService.findAllMatches()));
    }

    @Operation(
            summary = "Lister les ateliers publics disponibles",
            description = "Retourne les ateliers PUBLICS planifiés et encore disponibles. Endpoint public."
    )
    @GetMapping("/public")
    public ResponseEntity<List<AtelierDto>> getPublicMatches() {
        return ResponseEntity.ok(matchService.getPublicAvailableMatches());
    }

    @Operation(
            summary = "Consulter un atelier",
            description = "Retourne le détail d'un atelier. Les ateliers privés sont limités aux personnes autorisées.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @GetMapping("/{id}")
    public ResponseEntity<AtelierDto> getById(
            @Parameter(description = "ID de l’atelier", required = true)
            @PathVariable Long id) {
        AtelierDto atelier = matchService.getMatchDtoById(id);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (admin) {
            adminAuthorizationService.checkMatchAccess(id);
        } else if (atelier.typeMatch() == TypeAtelier.PRIVE) {
            Long participantId = currentMemberService.currentMemberId();
            if (!participantId.equals(atelier.organisateurId())
                    && !reservationRepository.existsByMatchIdAndMembreId(id, participantId)) {
                throw new AccessDeniedException("Accès refusé à cet atelier privé.");
            }
        }
        return ResponseEntity.ok(atelier);
    }

    @Operation(
            summary = "Lister les ateliers organisés par un participant",
            description = "Retourne les ateliers organisés par le participant connecté. " +
                    "Le participant connecté doit correspondre à l'ID fourni.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @GetMapping("/organisateur/{organisateurId}")
    public ResponseEntity<List<AtelierDto>> getByOrganisateur(
            @Parameter(description = "ID de l'organisateur", required = true)
            @PathVariable Long organisateurId) {
        currentMemberService.requireCurrentMember(organisateurId);
        return ResponseEntity.ok(matchService.findByOrganisateur(organisateurId));
    }

    @Operation(
            summary = "Lister les ateliers d'un centre",
            description = "Retourne les ateliers d'un centre. Accessible à un administrateur autorisé pour ce centre.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @GetMapping("/site/{siteId}")
    public ResponseEntity<List<AtelierDto>> getBySite(
            @Parameter(description = "ID du centre", required = true)
            @PathVariable Long siteId) {
        adminAuthorizationService.checkSiteAccess(siteId);
        return ResponseEntity.ok(matchService.findBySite(siteId));
    }

    @Operation(
            summary = "Modifier un atelier",
            description = "Modifie un atelier existant. Accessible uniquement au participant organisateur.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @PutMapping("/{id}")
    public ResponseEntity<AtelierDto> updateMatch(
            @Parameter(description = "ID de l’atelier", required = true)
            @PathVariable Long id,
            @Valid @RequestBody AtelierRequest request) {
        currentMemberService.requireCurrentMember(matchService.getMatchEntityById(id).getOrganisateur().getId());
        return ResponseEntity.ok(matchService.updateMatch(id, request));
    }

    @Operation(
            summary = "Annuler un atelier",
            description = "Annule un atelier existant. Accessible uniquement au participant organisateur.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelMatch(
            @Parameter(description = "ID de l’atelier", required = true)
            @PathVariable Long id,
            @RequestParam(required = false) Long requesterId) {
        Participant currentMember = currentMemberService.currentMember();
        if (requesterId != null && !currentMember.getId().equals(requesterId)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        matchService.cancelMatch(id, currentMember.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Ouvrir un atelier privé au public",
            description = "Ouvre un atelier privé au public à partir de J-2, uniquement après accord enregistré de l'organisateur. " +
                    "Accessible à un admin GLOBAL ou à un admin SITE dans son périmètre.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @PatchMapping("/{id}/convert-public")
    public ResponseEntity<Void> convertToPublic(
            @Parameter(description = "ID de l’atelier", required = true)
            @PathVariable Long id) {
        adminAuthorizationService.checkMatchAccess(id); // Ligne modifiée
        matchService.convertToPublic(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Autoriser l'ouverture publique d'un atelier privé",
            description = "Enregistre l'accord explicite et daté de l'organisateur. L'ouverture effective est possible à partir de J-2.",
            security = @SecurityRequirement(name = "Bearer Auth"))
    @PatchMapping("/{id}/consent-open-public")
    public ResponseEntity<Void> consentToPublicOpening(@PathVariable Long id) {
        matchService.consentToPublicOpening(id, currentMemberService.currentMemberId());
        return ResponseEntity.noContent().build();
    }
}
