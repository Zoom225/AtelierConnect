package com.atelierconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Journal conservé même après suppression d'une ancienne séance ou d'un compte. */
@Entity
@Table(name = "mouvements_dette", indexes = @Index(name = "idx_dette_participant", columnList = "participant_id,id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_mouvements_dette_reference", columnNames = "reference_source"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MouvementDette extends BaseEntity {
    public enum Nature { SOLDE_INITIAL, IMPUTATION_PRIVE, IMPUTATION_IMPAYE, REGLEMENT_DETTE, REGLEMENT_RESERVATION }
    @Column(name = "participant_id", nullable = false) private Long participantId;
    @Column(name = "atelier_source_id") private Long atelierId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Nature nature;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal montant;
    @Column(name = "reference_source", nullable = false, length = 150) private String referenceSource;
    @Column(nullable = false) private String description;
    @Column(nullable = false) private LocalDateTime dateOperation;
}
