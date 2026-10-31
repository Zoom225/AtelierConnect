package com.padell.padell.entity;

import com.padell.padell.entity.enums.TypeParticipant;
import com.padell.padell.entity.enums.TypeParticipantConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "membres")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Participant extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String matricule;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    private String email;

    @Column
    private String passwordHash; // Added passwordHash field

    @Convert(converter = TypeParticipantConverter.class)
    @Column(name = "type_membre", nullable = false)
    private TypeParticipant typeMembre;

    @Column(nullable = false)
    private Double solde;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Centre site;

    @OneToMany(mappedBy = "organisateur", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Atelier> matchesOrganises = new HashSet<>();

    @OneToMany(mappedBy = "membre", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Reservation> reservations = new HashSet<>();

    @OneToMany(mappedBy = "membre", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Penalite> penalites = new HashSet<>();
}
