package com.padell.padell.entity.enums;

/** Catégories métier exposées par l'API AtelierConnect. */
public enum TypeParticipant {
    PARTENAIRE,
    ABONNE,
    VISITEUR;

    public TypeMembre toLegacyType() {
        return switch (this) {
            case PARTENAIRE -> TypeMembre.GLOBAL;
            case ABONNE -> TypeMembre.SITE;
            case VISITEUR -> TypeMembre.LIBRE;
        };
    }
}
