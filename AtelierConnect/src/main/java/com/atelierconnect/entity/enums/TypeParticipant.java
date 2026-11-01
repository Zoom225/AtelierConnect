package com.atelierconnect.entity.enums;

/** Catégories métier exposées par l'API AtelierConnect. */
public enum TypeParticipant {
    PARTENAIRE,
    ABONNE,
    VISITEUR;

    public TypeParticipantLegacy toLegacyType() {
        return switch (this) {
            case PARTENAIRE -> TypeParticipantLegacy.GLOBAL;
            case ABONNE -> TypeParticipantLegacy.SITE;
            case VISITEUR -> TypeParticipantLegacy.LIBRE;
        };
    }

    public static TypeParticipant fromDatabaseValue(String value) {
        if (value == null) return null;
        return switch (value) {
            case "GLOBAL", "PARTENAIRE" -> PARTENAIRE;
            case "SITE", "ABONNE" -> ABONNE;
            case "LIBRE", "VISITEUR" -> VISITEUR;
            default -> throw new IllegalArgumentException("Catégorie de participant inconnue dans la base.");
        };
    }
}
