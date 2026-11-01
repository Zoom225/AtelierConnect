package com.atelierconnect.controller;

import com.atelierconnect.dto.request.ReservationInviteRequest;
import com.atelierconnect.dto.request.ReservationRequest;
import com.atelierconnect.dto.response.ReservationResponse;
import com.atelierconnect.entity.Participant;
import com.atelierconnect.entity.Reservation;
import com.atelierconnect.mapper.ReservationMapper;
import com.atelierconnect.service.ReservationService;
import com.atelierconnect.service.AtelierService;
import com.atelierconnect.service.impl.AdminAuthorizationService;
import com.atelierconnect.service.impl.CurrentParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Réservations", description = "Endpoints de gestion des réservations de atelier. " +
        "Une réservation relie un participant à un atelier et génère automatiquement un paiement en attente. " +
        "La place n'est confirmée qu'après paiement : premier payé, premier servi.")
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationMapper reservationMapper;
    private final CurrentParticipantService currentParticipantService;
    private final AtelierService atelierService;
    private final AdminAuthorizationService adminAuthorizationService;

    @Operation(
            summary = "Créer une réservation",
            description = "Crée une réservation pour le participant connecté sur un atelier donné. " +
                    "Un paiement EN_ATTENTE est créé automatiquement. " +
                    "Pour un atelier PUBLIC, le participant ne peut réserver que pour lui-même. " +
                    "Pour un atelier PRIVÉ, seul l'organisateur peut ajouter des participants. " +
                    "Accessible uniquement à un participant authentifié.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Réservation créée avec un paiement en attente",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Règle métier non respectée : atelier complet, participant déjà inscrit, pénalité, solde dû, mauvais centre ou restriction de atelier privé",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Atelier ou participant introuvable",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request) {
        Participant currentParticipant = currentParticipantService.currentParticipant();
        Reservation reservation = reservationService.create(
                request.getAtelierId(),
                request.getParticipantId(),
                currentParticipant.getId()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationMapper.toResponse(reservation));
    }

    @Operation(
            summary = "Ajouter des participants à un atelier privé",
            description = "Crée plusieurs réservations à partir de matricules. " +
                    "Le participant connecté doit être l'organisateur de l'atelier privé. " +
                    "Aucune fiche complète d'un autre participant n'est exposée au front.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Réservations créées",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Règle métier non respectée ou matricule introuvable",
                    content = @Content)
    })
    @PostMapping("/private-invites")
    public ResponseEntity<List<ReservationResponse>> createPrivateInvites(
            @Valid @RequestBody ReservationInviteRequest request) {
        Participant currentParticipant = currentParticipantService.currentParticipant();
        List<ReservationResponse> reservations = reservationService
                .createForInvites(request.getAtelierId(), currentParticipant.getId(), request.getInviteeMatricules())
                .stream()
                .map(reservationMapper::toResponse)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(reservations);
    }

    @Operation(
            summary = "Consulter une réservation",
            description = "Retourne une réservation par son ID. " +
                    "Accessible uniquement au participant propriétaire de la réservation.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservation trouvée",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable",
                    content = @Content)
    })
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<ReservationResponse> getById(
            @Parameter(description = "ID de la réservation", required = true)
            @PathVariable("id") Long id) {
        Reservation reservation = reservationService.getById(id);
        currentParticipantService.requireCurrentParticipant(reservation.getParticipant().getId());
        return ResponseEntity.ok(reservationMapper.toResponse(reservation));
    }

    @Operation(
            summary = "Lister les réservations d'un atelier",
            description = "Retourne les réservations liées à un atelier. " +
                    "Accessible aux participants et aux administrateurs selon la configuration de sécurité.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservations de l'atelier retournées",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Atelier introuvable",
                    content = @Content)
    })
    @GetMapping({"/match/{matchId}", "/atelier/{matchId}"})
    public ResponseEntity<List<ReservationResponse>> getByAtelierId(
            @Parameter(description = "ID de l'atelier", required = true)
            @PathVariable("matchId") Long atelierId) {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (admin) {
            adminAuthorizationService.checkAtelierAccess(atelierId);
        } else {
            currentParticipantService.requireCurrentParticipant(
                    atelierService.getAtelierEntityById(atelierId).getOrganisateur().getId());
        }
        List<ReservationResponse> reservations = reservationService.getByAtelierId(atelierId)
                .stream()
                .map(reservationMapper::toResponse)
                .toList();
        return ResponseEntity.ok(reservations);
    }

    @Operation(
            summary = "Lister les réservations d'un participant",
            description = "Retourne les réservations du participant indiqué. " +
                    "Accessible uniquement au participant connecté correspondant à cet ID.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservations du participant retournées",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Participant introuvable",
                    content = @Content)
    })
    @GetMapping({"/membre/{membreId}", "/participant/{membreId}"})
    public ResponseEntity<List<ReservationResponse>> getByParticipantId(
            @Parameter(description = "ID du participant", required = true)
            @PathVariable("membreId") Long participantId) {
        currentParticipantService.requireCurrentParticipant(participantId);
        List<ReservationResponse> reservations = reservationService.getByParticipantId(participantId)
                .stream()
                .map(reservationMapper::toResponse)
                .toList();
        return ResponseEntity.ok(reservations);
    }

    @Operation(
            summary = "Annuler une réservation",
            description = "Annule une réservation existante. " +
                    "Si le paiement était PAYE, il passe à REMBOURSE. " +
                    "Si la réservation était seulement EN_ATTENTE, le nombre de participants confirmés n'est pas décrémenté. " +
                    "Accessible uniquement au participant propriétaire de la réservation.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Réservation annulée"),
            @ApiResponse(responseCode = "400", description = "Réservation déjà annulée",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable",
                    content = @Content)
    })
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(
            @Parameter(description = "ID de la réservation à annuler", required = true)
            @PathVariable("id") Long id) {
        currentParticipantService.requireCurrentParticipant(reservationService.getById(id).getParticipant().getId());
        reservationService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
