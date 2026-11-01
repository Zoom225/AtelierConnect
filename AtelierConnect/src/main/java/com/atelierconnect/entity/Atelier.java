package com.atelierconnect.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.atelierconnect.entity.enums.StatutAtelier;
import com.atelierconnect.entity.enums.TypeAtelier;
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
    private Salle salle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisateur_id", nullable = false)
    private Participant organisateur;

    @Column(nullable = false)
    private LocalDateTime dateDebut;

    @Column(nullable = false)
    private LocalDateTime dateFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_match", nullable = false)
    private TypeAtelier typeAtelier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutAtelier statut;

    @Column(name = "nb_joueurs_actuels", nullable = false)
    private Integer nbParticipantsActuels;

    @Column(nullable = false)
    private Double prixTotal;

    @Column(name = "prix_par_joueur", nullable = false)
    private Double prixParParticipant;

    private LocalDateTime dateConversionPublic;

    @Column(name = "accord_ouverture_publique")
    private Boolean accordOuverturePublique;

    @Column(name = "date_accord_ouverture_publique")
    private LocalDateTime dateAccordOuverturePublique;

    @OneToMany(mappedBy = "atelier", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("match-reservations")
    private List<Reservation> reservations;

    // L'entité est maintenant un simple POJO qui représente la table de la base de données.
    // Toute la logique de manipulation des dates a été déplacée dans la couche service.
}
