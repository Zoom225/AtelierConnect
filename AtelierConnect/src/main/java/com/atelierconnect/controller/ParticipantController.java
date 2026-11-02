package com.atelierconnect.controller;

import com.atelierconnect.config.JwtConfig;
import com.atelierconnect.dto.request.ParticipantLoginRequest;
import com.atelierconnect.dto.request.ParticipantRequest;
import com.atelierconnect.dto.response.ParticipantResponse;
import com.atelierconnect.entity.Participant;
import com.atelierconnect.mapper.ParticipantMapper;
import com.atelierconnect.service.ParticipantService;
import com.atelierconnect.service.impl.CurrentParticipantService;
import com.atelierconnect.service.impl.ParticipantCreationService;
import com.atelierconnect.service.impl.ParticipantAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/participants", "/api/membres"})
@RequiredArgsConstructor
@Tag(name = "Membres", description = "Endpoints de gestion des participants collaboratif. " +
        "Les participants se connectent avec leur matricule via /api/participants/login.")
public class ParticipantController {

    private final ParticipantService participantService;
    private final ParticipantMapper participantMapper;
    private final JwtConfig jwtConfig;
    private final ParticipantAccessService participantAccessService;
    private final ParticipantCreationService participantCreationService;
    private final CurrentParticipantService currentParticipantService;

    @Operation(
            summary = "Créer un participant",
            description = "Crée un participant avec un matricule unique. " +
                    "Un participant SITE doit être rattaché à un centre. " +
                    "Accessible uniquement aux administrateurs.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Participant créé",
                    content = @Content(schema = @Schema(implementation = ParticipantResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide, format de matricule incorrect ou matricule déjà existant",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Centre introuvable lorsque siteId est fourni",
                    content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') and @participantAccessService.canCreate(#request.centreId)")
    public ResponseEntity<ParticipantResponse> create(@Valid @RequestBody ParticipantRequest request) {
        Participant saved = participantCreationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(participantMapper.toResponse(saved));
    }

    @Operation(
            summary = "Lister les participants",
            description = "Retourne les participants visibles pour l'administrateur connecté.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des participants retournée",
                    content = @Content(schema = @Schema(implementation = ParticipantResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ParticipantResponse>> getAll() {
        List<ParticipantResponse> participants = participantAccessService.filterParticipants(participantService.getAll())
                .stream()
                .map(participantMapper::toResponse)
                .toList();
        return ResponseEntity.ok(participants);
    }

    @Operation(
            summary = "Consulter un participant par ID",
            description = "Retourne un participant par son ID interne. " +
                    "Accessible au participant propriétaire de la donnée ou à un administrateur.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participant trouvé",
                    content = @Content(schema = @Schema(implementation = ParticipantResponse.class))),
            @ApiResponse(responseCode = "404", description = "Participant introuvable",
                    content = @Content)
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN') and @participantAccessService.canReadById(#id)")
    public ResponseEntity<ParticipantResponse> getById(
            @Parameter(description = "ID interne du participant", required = true)
            @PathVariable("id") Long id) {
        Participant participant = participantService.getById(id);
        return ResponseEntity.ok(participantMapper.toResponse(participant));
    }

    @Operation(
            summary = "Consulter un participant par matricule",
            description = "Retourne un participant à partir de son matricule. " +
                    "Accessible aux participants et aux administrateurs authentifiés.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participant trouvé",
                    content = @Content(schema = @Schema(implementation = ParticipantResponse.class))),
            @ApiResponse(responseCode = "404", description = "Participant introuvable avec ce matricule",
                    content = @Content)
    })
    @GetMapping("/matricule/{matricule}")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN') and @participantAccessService.canReadByMatricule(#matricule)")
    public ResponseEntity<ParticipantResponse> getByMatricule(
            @Parameter(description = "Matricule unique du participant", required = true)
            @PathVariable("matricule") String matricule) {
        Participant participant = participantService.getByMatricule(matricule);
        return ResponseEntity.ok(participantMapper.toResponse(participant));
    }

    @Operation(
            summary = "Consulter le participant connecté",
            description = "Retourne les informations du participant authentifié. " +
                    "Accessible uniquement à un participant connecté.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participant connecté trouvé",
                    content = @Content(schema = @Schema(implementation = ParticipantResponse.class))),
            @ApiResponse(responseCode = "404", description = "Participant connecté introuvable",
                    content = @Content)
    })
    @GetMapping("/me")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<ParticipantResponse> me() {
        Participant participant = currentParticipantService.currentParticipant();
        return ResponseEntity.ok(participantMapper.toResponse(participant));
    }

    @Operation(
            summary = "Vérifier la pénalité active d'un participant",
            description = "Retourne true si le participant a une pénalité active. " +
                    "Pendant 7 jours, le participant ne peut plus créer ni rejoindre de atelier. " +
                    "Accessible au participant propriétaire de la donnée ou à un administrateur.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut de pénalité retourné",
                    content = @Content(schema = @Schema(implementation = Boolean.class))),
            @ApiResponse(responseCode = "404", description = "Participant introuvable",
                    content = @Content)
    })
    @GetMapping("/{id}/penalty")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN') and @participantAccessService.canReadById(#id)")
    public ResponseEntity<Boolean> hasActivePenalty(
            @Parameter(description = "ID du participant à vérifier", required = true)
            @PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(participantService.hasActivePenalty(id));
        } catch (com.atelierconnect.exception.ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @Operation(
            summary = "Vérifier le solde dû d'un participant",
            description = "Retourne true si le participant a un solde dû. " +
                    "Un participant avec un solde dû ne peut pas créer de nouveau atelier tant que ce solde n'est pas réglé. " +
                    "Le solde est automatiquement ajouté au prochain paiement. " +
                    "Accessible au participant propriétaire de la donnée ou à un administrateur.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut de solde retourné",
                    content = @Content(schema = @Schema(implementation = Boolean.class))),
            @ApiResponse(responseCode = "404", description = "Participant introuvable",
                    content = @Content)
    })
    @GetMapping("/{id}/balance")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN') and @participantAccessService.canReadById(#id)")
    public ResponseEntity<Boolean> hasOutstandingBalance(
            @Parameter(description = "ID du participant à vérifier", required = true)
            @PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(participantService.hasOutstandingBalance(id));
        } catch (com.atelierconnect.exception.ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @Operation(
            summary = "Modifier un participant",
            description = "Modifie les informations personnelles d'un participant existant. " +
                    "Accessible uniquement aux administrateurs.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participant modifié",
                    content = @Content(schema = @Schema(implementation = ParticipantResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide ou erreur de validation",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé : token administrateur requis",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Participant introuvable",
                    content = @Content)
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and @participantAccessService.canManageById(#id)")
    public ResponseEntity<ParticipantResponse> update(
            @Parameter(description = "ID du participant à modifier", required = true)
            @PathVariable("id") Long id,
            @Valid @RequestBody ParticipantRequest request) {
        Participant participant = participantMapper.toEntity(request);
        Participant updated = participantService.update(id, participant);
        return ResponseEntity.ok(participantMapper.toResponse(updated));
    }

    @Operation(
            summary = "Supprimer un participant",
            description = "Supprime définitivement un participant et ses données associées. " +
                    "Accessible uniquement aux administrateurs.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Participant supprimé"),
            @ApiResponse(responseCode = "403", description = "Accès refusé : token administrateur requis",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Participant introuvable",
                    content = @Content)
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and @participantAccessService.canManageById(#id)")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID du participant à supprimer", required = true)
            @PathVariable("id") Long id) {
        participantService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Connexion participant",
            description = "Authentifie un participant avec son matricule et retourne un JWT pour accéder aux ressources protégées."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion réussie : JWT retourné",
                    content = @Content(schema = @Schema(implementation = ParticipantResponse.class))),
            @ApiResponse(responseCode = "404", description = "Participant introuvable avec ce matricule",
                    content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<ParticipantResponse> login(@Valid @RequestBody ParticipantLoginRequest request) {
        Participant participant = participantService.authenticate(request.getMatricule(), request.getPassword());
        ParticipantResponse response = participantMapper.toResponse(participant);
        String token = jwtConfig.generateToken(participant.getMatricule(), participant.getTypeParticipant().name(), "MEMBER");
        response.setToken(token);
        return ResponseEntity.ok(response);
    }
}
