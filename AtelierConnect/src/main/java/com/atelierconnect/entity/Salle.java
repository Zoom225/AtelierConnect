package com.atelierconnect.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/** Salle accueillant les ateliers du centre. */
@Entity
@Table(name = "salles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Salle extends BaseEntity {

    @Column(nullable = false)
    private String nom;

    @Column
    private Double prix;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centre_id", nullable = false)
    @JsonBackReference("centre-salles")
    private Centre centre;

    @OneToMany(mappedBy = "salle", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("salle-ateliers")
    @Builder.Default
    private List<Atelier> ateliers = new ArrayList<>();
}
