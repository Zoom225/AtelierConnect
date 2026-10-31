package com.padell.padell.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.padell.padell.entity.enums.StatutAtelier;
import com.padell.padell.entity.enums.TypeAtelier;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Atelier extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terrain_id", nullable = false)
    @JsonBackReference("terrain-matches")
    private Salle terrain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisateur_id", nullable = false)
    private Participant organisateur;

    @Column(nullable = false)
    private LocalDateTime dateDebut;

    @Column(nullable = false)
    private LocalDateTime dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeAtelier typeMatch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutAtelier statut;

    @Column(nullable = false)
    private Integer nbJoueursActuels;

    @Column(nullable = false)
    private Double prixTotal;

    @Column(nullable = false)
    private Double prixParJoueur;

    private LocalDateTime dateConversionPublic;

    @Column(name = "accord_ouverture_publique")
    private Boolean accordOuverturePublique;

    @Column(name = "date_accord_ouverture_publique")
    private LocalDateTime dateAccordOuverturePublique;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("match-reservations")
    private List<Reservation> reservations;

    // L'entité est maintenant un simple POJO qui représente la table de la base de données.
    // Toute la logique de manipulation des dates a été déplacée dans la couche service.
}
