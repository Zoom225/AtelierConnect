package com.atelierconnect.controller;

import com.atelierconnect.dto.request.SalleRequest;
import com.atelierconnect.dto.response.SalleResponse;
import com.atelierconnect.entity.Salle;
import com.atelierconnect.mapper.SalleMapper;
import com.atelierconnect.service.SalleService;
import com.atelierconnect.service.impl.AdminAuthorizationService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/salles", "/api/terrains"})
@RequiredArgsConstructor
@Tag(name = "Terrains", description = "Endpoints de gestion des salles collaboratif.")
public class SalleController {

    private final SalleService salleService;
    private final SalleMapper salleMapper;
    private final AdminAuthorizationService adminAuthorizationService;

    @Operation(
            summary = "Créer une salle",
            description = "Crée une salle et le rattache à un centre existant. " +
                    "Accessible à un admin GLOBAL ou à un admin SITE dans son périmètre.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Salle créé",
                    content = @Content(schema = @Schema(implementation = SalleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide ou erreur de validation",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Centre introuvable",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<SalleResponse> create(@Valid @RequestBody SalleRequest request) {
        adminAuthorizationService.checkCentreAccess(request.getCentreId());
        Salle salle = salleMapper.toEntity(request);
        Salle saved = salleService.create(salle, request.getCentreId());
        return ResponseEntity.status(HttpStatus.CREATED).body(salleMapper.toResponse(saved));
    }

    @Operation(
            summary = "Lister les salles",
            description = "Retourne la liste des salles. Endpoint public."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des salles retournée",
                    content = @Content(schema = @Schema(implementation = SalleResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<SalleResponse>> getAll() {
        List<SalleResponse> salles = salleService.getAll()
                .stream()
                .map(salleMapper::toResponse)
                .toList();
        return ResponseEntity.ok(salles);
    }

    @Operation(
            summary = "Consulter une salle",
            description = "Retourne une salle par son ID. Endpoint public."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Salle trouvé",
                    content = @Content(schema = @Schema(implementation = SalleResponse.class))),
            @ApiResponse(responseCode = "404", description = "Salle introuvable",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<SalleResponse> getById(
            @Parameter(description = "ID de la salle", required = true)
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(salleMapper.toResponse(salleService.getById(id)));
    }

    @Operation(
            summary = "Lister les salles d'un centre",
            description = "Retourne les salles d'un centre donné. Endpoint public."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Terrains du centre retournés",
                    content = @Content(schema = @Schema(implementation = SalleResponse.class))),
            @ApiResponse(responseCode = "404", description = "Centre introuvable",
                    content = @Content)
    })
    @GetMapping("/site/{siteId}")
    public ResponseEntity<List<SalleResponse>> getByCentreId(
            @Parameter(description = "ID du centre", required = true)
            @PathVariable("siteId") Long centreId) {
        List<SalleResponse> salles = salleService.getByCentreId(centreId)
                .stream()
                .map(salleMapper::toResponse)
                .toList();
        return ResponseEntity.ok(salles);
    }

    @Operation(
            summary = "Modifier une salle",
            description = "Modifie une salle existant. " +
                    "Accessible à un admin GLOBAL ou à un admin SITE dans son périmètre.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Salle modifié",
                    content = @Content(schema = @Schema(implementation = SalleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide ou erreur de validation",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Salle introuvable",
                    content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<SalleResponse> update(
            @Parameter(description = "ID de la salle à modifier", required = true)
            @PathVariable("id") Long id,
            @Valid @RequestBody SalleRequest request) {
        adminAuthorizationService.checkSalleAccess(salleService.getById(id));
        Salle salle = salleMapper.toEntity(request);
        Salle updated = salleService.update(id, salle);
        return ResponseEntity.ok(salleMapper.toResponse(updated));
    }

    @Operation(
            summary = "Supprimer une salle",
            description = "Supprime définitivement une salle et ses atelier associés. " +
                    "Accessible à un admin GLOBAL ou à un admin SITE dans son périmètre.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Salle supprimé"),
            @ApiResponse(responseCode = "403", description = "Accès refusé",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Salle introuvable",
                    content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID de la salle à supprimer", required = true)
            @PathVariable("id") Long id) {
        adminAuthorizationService.checkSalleAccess(salleService.getById(id));
        salleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
