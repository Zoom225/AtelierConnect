package com.atelierconnect.entity.enums;

public enum TypeParticipantLegacy {
    GLOBAL,   // Code SQL historique de PARTENAIRE (30 jours).
    SITE,     // Code SQL historique de ABONNE (15 jours).
    LIBRE;    // Valeurs SQL historiques conservées pour les lignes existantes.

    public TypeParticipant toCategorie() {
        return switch (this) {
            case GLOBAL -> TypeParticipant.PARTENAIRE;
            case SITE -> TypeParticipant.ABONNE;
            case LIBRE -> TypeParticipant.VISITEUR;
        };
    }
}
