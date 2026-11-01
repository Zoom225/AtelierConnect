package com.atelierconnect.controller;

import com.atelierconnect.dto.request.JourFermetureRequest;
import com.atelierconnect.dto.response.JourFermetureResponse;
import com.atelierconnect.entity.Administrateur;
import com.atelierconnect.entity.JourFermeture;
import com.atelierconnect.entity.Centre;
import com.atelierconnect.entity.enums.TypeAdministrateur;
import com.atelierconnect.exception.BusinessException;
import com.atelierconnect.repository.AdministrateurRepository;
import com.atelierconnect.repository.JourFermetureRepository;
import com.atelierconnect.service.CentreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jours-fermeture")
@RequiredArgsConstructor
@Tag(name = "Jours de fermeture", description = "Endpoints de gestion des fermetures globales ou propres à un centre.")
public class JourFermetureController {

    private final JourFermetureRepository jourFermetureRepository;
    private final CentreService centreService;
    private final AdministrateurRepository administrateurRepository;

    @Operation(
            summary = "Lister les jours de fermeture visibles",
            description = "Retourne les fermetures visibles pour l'administrateur connecté. " +
                    "Un admin GLOBAL voit tout ; un admin SITE voit les fermetures globales et celles de son centre.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @GetMapping
    public ResponseEntity<List<JourFermetureResponse>> getAll() {
        Administrateur admin = getAuthenticatedAdmin();
        return ResponseEntity.ok(
                jourFermetureRepository.findAll().stream()
                        .filter(jour -> canAccessJour(admin, jour))
                        .map(this::toResponse)
                        .toList()
        );
    }

    @Operation(
            summary = "Lister les fermetures globales",
            description = "Retourne les fermetures qui bloquent tous les centres. Accessible uniquement aux administrateurs.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @GetMapping("/global")
    public ResponseEntity<List<JourFermetureResponse>> getGlobal() {
        return ResponseEntity.ok(
                jourFermetureRepository.findByGlobalTrue().stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    @Operation(
            summary = "Lister les fermetures d'un centre",
            description = "Retourne les fermetures propres à un centre. " +
                    "Accessible à un admin GLOBAL ou à un admin SITE dans son périmètre.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @GetMapping({"/centre/{siteId}", "/site/{siteId}"})
    public ResponseEntity<List<JourFermetureResponse>> getByCentre(
            @Parameter(description = "ID du centre", required = true)
            @PathVariable("siteId") Long centreId) {
        Administrateur admin = getAuthenticatedAdmin();
        validateCentreAccess(admin, centreId);
        return ResponseEntity.ok(
                jourFermetureRepository.findByCentreId(centreId).stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    @Operation(
            summary = "Créer un jour de fermeture",
            description = "Crée une fermeture globale ou une fermeture propre à un centre. " +
                    "Seul un admin GLOBAL peut créer une fermeture globale. " +
                    "Un admin SITE ne peut créer une fermeture que sur son propre centre.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @PostMapping
    public ResponseEntity<JourFermetureResponse> create(@Valid @RequestBody JourFermetureRequest request) {
        Administrateur admin = getAuthenticatedAdmin();

        if (Boolean.TRUE.equals(request.getGlobal())) {
            if (admin.getTypeAdministrateur() != TypeAdministrateur.GLOBAL) {
                throw new BusinessException("Seul un admin GLOBAL peut créer une fermeture globale.");
            }
        } else if (request.getCentreId() == null) {
            throw new BusinessException("Un centre est obligatoire pour une fermeture non globale.");
        } else {
            validateCentreAccess(admin, request.getCentreId());
        }

        Centre centre = Boolean.TRUE.equals(request.getGlobal()) ? null : centreService.getById(request.getCentreId());
        JourFermeture jour = JourFermeture.builder()
                .date(request.getDate())
                .raison(request.getRaison())
                .global(Boolean.TRUE.equals(request.getGlobal()))
                .centre(centre)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(jourFermetureRepository.save(jour)));
    }

    @Operation(
            summary = "Supprimer un jour de fermeture",
            description = "Supprime une fermeture. " +
                    "Seul un admin GLOBAL peut supprimer une fermeture globale. " +
                    "Un admin SITE ne peut supprimer que les fermetures de son centre.",
            security = @SecurityRequirement(name = "Bearer Auth")
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID du jour de fermeture", required = true)
            @PathVariable("id") Long id) {
        Administrateur admin = getAuthenticatedAdmin();
        JourFermeture jour = jourFermetureRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Jour de fermeture introuvable."));

        if (Boolean.TRUE.equals(jour.getGlobal()) && admin.getTypeAdministrateur() != TypeAdministrateur.GLOBAL) {
            throw new BusinessException("Seul un admin GLOBAL peut supprimer une fermeture globale.");
        }
        if (jour.getCentre() != null) {
            validateCentreAccess(admin, jour.getCentre().getId());
        }

        jourFermetureRepository.delete(jour);
        return ResponseEntity.noContent().build();
    }

    private Administrateur getAuthenticatedAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException("Authentification administrateur requise.");
        }

        return administrateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new BusinessException("Administrateur introuvable."));
    }

    private boolean canAccessJour(Administrateur admin, JourFermeture jour) {
        return admin.getTypeAdministrateur() == TypeAdministrateur.GLOBAL
                || Boolean.TRUE.equals(jour.getGlobal())
                || (jour.getCentre() != null && admin.getCentre() != null && jour.getCentre().getId().equals(admin.getCentre().getId()));
    }

    private void validateCentreAccess(Administrateur admin, Long centreId) {
        if (admin.getTypeAdministrateur() == TypeAdministrateur.GLOBAL) {
            return;
        }
        if (admin.getCentre() == null || !admin.getCentre().getId().equals(centreId)) {
            throw new BusinessException("Un admin SITE ne peut agir que sur son propre centre.");
        }
    }

    private JourFermetureResponse toResponse(JourFermeture jour) {
        return new JourFermetureResponse(
                jour.getId(),
                jour.getDate(),
                jour.getRaison(),
                jour.getGlobal(),
                jour.getCentre() != null ? jour.getCentre().getId() : null,
                jour.getCentre() != null ? jour.getCentre().getNom() : null
        );
    }
}
