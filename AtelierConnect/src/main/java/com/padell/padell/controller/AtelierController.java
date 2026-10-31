package com.padell.padell.controller;

import com.padell.padell.dto.request.AtelierRequest;
import com.padell.padell.entity.Participant;
import com.padell.padell.dto.request.CreateAtelierRequest;
import com.padell.padell.dto.response.AtelierDto;
import com.padell.padell.entity.enums.TypeAtelier;
import com.padell.padell.repository.ReservationRepository;
import com.padell.padell.service.AtelierService;
import com.padell.padell.service.impl.AdminAuthorizationService;
import com.padell.padell.service.impl.CurrentParticipantService;
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
            summary = "Créer un match",
            description = "Crée un match PUBLIC ou PRIVÉ pour le membre connecté. " +
                    "Les limites de réservation GLOBAL, SITE et LIBRE s'appliquent à la création du match. " +
                    "Accessible uniquement à un membre authentifié.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @PostMapping
    public ResponseEntity<AtelierDto> createMatch(@Valid @RequestBody CreateAtelierRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        log.info("Requête de création de match reçue de l'utilisateur '{}' pour le terrain ID {}", username, request.terrainId());

        if (username == null || "anonymousUser".equals(username)) {
            log.warn("Tentative de création de match par un utilisateur non authentifié.");
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        AtelierDto createdMatch = matchService.createMatch(request, username);
        return new ResponseEntity<>(createdMatch, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Lister tous les matchs",
            description = "Retourne les matchs visibles pour l'administrateur connecté. " +
                    "Un admin GLOBAL voit tous les matchs ; un admin SITE voit uniquement ceux de son site.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @GetMapping
    public ResponseEntity<List<AtelierDto>> getAllMatches() {
        return ResponseEntity.ok(adminAuthorizationService.filterMatchDtos(matchService.findAllMatches()));
    }

    @Operation(
            summary = "Lister les matchs publics disponibles",
            description = "Retourne les matchs PUBLICS planifiés et encore disponibles. Endpoint public."
    )
    @GetMapping("/public")
    public ResponseEntity<List<AtelierDto>> getPublicMatches() {
        return ResponseEntity.ok(matchService.getPublicAvailableMatches());
    }

    @Operation(
            summary = "Consulter un match",
            description = "Retourne le détail d'un match. Accessible aux membres et aux administrateurs authentifiés.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @GetMapping("/{id}")
    public ResponseEntity<AtelierDto> getById(
            @Parameter(description = "ID du match", required = true)
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
            summary = "Lister les matchs organisés par un membre",
            description = "Retourne les matchs organisés par le membre connecté. " +
                    "Le membre connecté doit correspondre à l'ID fourni.",
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
            summary = "Lister les matchs d'un site",
            description = "Retourne les matchs d'un site. Accessible à un admin GLOBAL ou à un admin SITE dans son périmètre.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @GetMapping("/site/{siteId}")
    public ResponseEntity<List<AtelierDto>> getBySite(
            @Parameter(description = "ID du site", required = true)
            @PathVariable Long siteId) {
        adminAuthorizationService.checkSiteAccess(siteId);
        return ResponseEntity.ok(matchService.findBySite(siteId));
    }

    @Operation(
            summary = "Modifier un match",
            description = "Modifie un match existant. Accessible uniquement au membre organisateur.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @PutMapping("/{id}")
    public ResponseEntity<AtelierDto> updateMatch(
            @Parameter(description = "ID du match", required = true)
            @PathVariable Long id,
            @Valid @RequestBody AtelierRequest request) {
        currentMemberService.requireCurrentMember(matchService.getMatchEntityById(id).getOrganisateur().getId());
        return ResponseEntity.ok(matchService.updateMatch(id, request));
    }

    @Operation(
            summary = "Annuler un match",
            description = "Annule un match existant. Accessible uniquement au membre organisateur.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelMatch(
            @Parameter(description = "ID du match", required = true)
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
            summary = "Convertir un match privé en public",
            description = "Ouvre un atelier privé au public à partir de J-2, uniquement après accord enregistré de l'organisateur. " +
                    "Accessible à un admin GLOBAL ou à un admin SITE dans son périmètre.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @PatchMapping("/{id}/convert-public")
    public ResponseEntity<Void> convertToPublic(
            @Parameter(description = "ID du match", required = true)
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
