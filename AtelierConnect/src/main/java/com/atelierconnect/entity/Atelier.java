package com.atelierconnect.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.atelierconnect.entity.enums.StatutAtelier;
import com.atelierconnect.entity.enums.TypeAtelier;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Atelier collaboratif et ses reservations. */
@Entity
@Table(name = "ateliers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Atelier extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salle_id", nullable = false)
    @JsonBackReference("salle-ateliers")
    private Salle salle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisateur_id", nullable = false)
    private Participant organisateur;

    @Column(nullable = false)
    private LocalDateTime dateDebut;

    @Column(nullable = false)
    private LocalDateTime dateFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_atelier", nullable = false)
    private TypeAtelier typeAtelier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutAtelier statut;

    @Column(name = "nb_participants_actuels", nullable = false)
    private Integer nbParticipantsActuels;

    @Column(nullable = false)
    private Double prixTotal;

    @Column(name = "prix_par_participant", nullable = false)
    private Double prixParParticipant;

    private LocalDateTime dateConversionPublic;

    @Column(name = "accord_ouverture_publique")
    private Boolean accordOuverturePublique;

    @Column(name = "date_accord_ouverture_publique")
    private LocalDateTime dateAccordOuverturePublique;

    @OneToMany(mappedBy = "atelier", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("atelier-reservations")
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();
}
