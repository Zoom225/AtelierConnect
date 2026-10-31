package com.atelierconnect.controller;

import com.atelierconnect.dto.request.CentreRequest;
import com.atelierconnect.dto.response.CentreResponse;
import com.atelierconnect.entity.Centre;
import com.atelierconnect.mapper.CentreMapper;
import com.atelierconnect.service.CentreService;
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
@RequestMapping({"/api/centres", "/api/sites"})
@RequiredArgsConstructor
@Tag(name = "Centres", description = "Gestion des centres accueillant les ateliers collaboratifs.")
public class CentreController {

    private final CentreService siteService;
    private final CentreMapper siteMapper;
    private final AdminAuthorizationService adminAuthorizationService;

    @Operation(
            summary = "Créer un centre",
            description = "Crée un centre avec ses horaires, la durée des ateliers et la durée des pauses. " +
                    "Accessible uniquement à un admin GLOBAL.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Centre créé",
                    content = @Content(schema = @Schema(implementation = CentreResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide ou erreur de validation",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<CentreResponse> create(@Valid @RequestBody CentreRequest request) {
        adminAuthorizationService.requireGlobalAdmin();
        Centre site = siteMapper.toEntity(request);
        Centre saved = siteService.create(site);
        return ResponseEntity.status(HttpStatus.CREATED).body(siteMapper.toResponse(saved));
    }

    @Operation(
            summary = "Lister les centres",
            description = "Retourne la liste des centres. Endpoint public."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des centres retournée",
                    content = @Content(schema = @Schema(implementation = CentreResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<CentreResponse>> getAll() {
        List<CentreResponse> sites = siteService.getAll()
                .stream()
                .map(siteMapper::toResponse)
                .toList();
        return ResponseEntity.ok(sites);
    }

    @Operation(
            summary = "Consulter un centre",
            description = "Retourne un centre par son identifiant. Endpoint public."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Centre trouvé",
                    content = @Content(schema = @Schema(implementation = CentreResponse.class))),
            @ApiResponse(responseCode = "404", description = "Centre introuvable",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<CentreResponse> getById(
            @Parameter(description = "Identifiant du centre", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(siteMapper.toResponse(siteService.getById(id)));
    }

    @Operation(
            summary = "Modifier un centre",
            description = "Modifie un centre existant. Accessible uniquement à un administrateur global.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Centre modifié",
                    content = @Content(schema = @Schema(implementation = CentreResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide ou erreur de validation",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Centre introuvable",
                    content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<CentreResponse> update(
            @Parameter(description = "ID du site à modifier", required = true)
            @PathVariable Long id,
            @Valid @RequestBody CentreRequest request) {
        adminAuthorizationService.requireGlobalAdmin();
        Centre site = siteMapper.toEntity(request);
        Centre updated = siteService.update(id, site);
        return ResponseEntity.ok(siteMapper.toResponse(updated));
    }

    @Operation(
            summary = "Supprimer un centre",
            description = "Supprime définitivement un centre et ses données associées. Accessible uniquement à un administrateur global.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Centre supprimé"),
            @ApiResponse(responseCode = "403", description = "Accès refusé",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Centre introuvable",
                    content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID du site à supprimer", required = true)
            @PathVariable Long id) {
        adminAuthorizationService.requireGlobalAdmin();
        siteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
