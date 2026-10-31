package com.padell.padell.entity.enums;

public enum TypeParticipantLegacy {
    GLOBAL,   // Gxxxx — tous les sites, 3 semaines avant
    SITE,     // Sxxxxx — son site uniquement, 2 semaines avant
    LIBRE;    // Valeurs SQL historiques conservées pour les lignes existantes.

    public TypeParticipant toCategorie() {
        return switch (this) {
            case GLOBAL -> TypeParticipant.PARTENAIRE;
            case SITE -> TypeParticipant.ABONNE;
            case LIBRE -> TypeParticipant.VISITEUR;
        };
    }
}
