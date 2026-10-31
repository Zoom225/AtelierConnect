package com.atelierconnect.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "terrains")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Salle extends BaseEntity {

    @Column(nullable = false)
    private String nom;

    @Column(nullable = true)
    private Double prix;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    @JsonBackReference("site-terrains")
    private Centre site;

    @OneToMany(mappedBy = "terrain", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("terrain-matches")
    private List<Atelier> matches;

    public Double getPrix() {
        return prix;
    }
}
