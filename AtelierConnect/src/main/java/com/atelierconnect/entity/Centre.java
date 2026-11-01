package com.atelierconnect.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Centre extends BaseEntity {

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String adresse;

    @Column(nullable = false)
    private LocalTime heureOuverture;

    @Column(nullable = false)
    private LocalTime heureFermeture;

    @Column(name = "duree_match_minutes", nullable = false)
    private Integer dureeAtelierMinutes;

    @Column(name = "duree_entre_match_minutes", nullable = false)
    private Integer dureeEntreAtelierMinutes;

    @Column(nullable = false)
    private Integer anneeCivile;

    @Column(name = "ouvert_visiteurs")
    private Boolean ouvertVisiteurs;

    @OneToMany(mappedBy = "centre", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("site-terrains")
    @Builder.Default
    private Set<Salle> salles = new HashSet<>();

    @OneToMany(mappedBy = "centre", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("site-joursFermeture")
    @Builder.Default
    private Set<JourFermeture> joursFermeture = new HashSet<>();
}
