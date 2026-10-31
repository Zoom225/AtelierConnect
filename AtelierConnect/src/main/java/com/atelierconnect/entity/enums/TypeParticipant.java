package com.atelierconnect.entity.enums;

/** Catégories métier exposées par l'API AtelierConnect. */
public enum TypeParticipant {
    PARTENAIRE,
    ABONNE,
    VISITEUR;

    // Alias Java transitoires pour les anciens tests et appels internes.
    @Deprecated public static final TypeParticipant GLOBAL = PARTENAIRE;
    @Deprecated public static final TypeParticipant SITE = ABONNE;
    @Deprecated public static final TypeParticipant LIBRE = VISITEUR;

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
