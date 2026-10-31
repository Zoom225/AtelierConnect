package com.atelierconnect.service;

import com.atelierconnect.entity.enums.TypeParticipant;

public final class AtelierRules {
    public static final int CAPACITE = 6;
    public static final int DUREE_MINUTES = 120;
    public static final int PAUSE_MINUTES = 20;
    public static final double PRIX_TOTAL = 90.0;
    public static final double PRIX_PARTICIPANT = PRIX_TOTAL / CAPACITE;
    public static final int PARTENAIRE_JOURS = 30;
    public static final int ABONNE_JOURS = 15;
    public static final int VISITEUR_JOURS = 7;
    public static final int OUVERTURE_PUBLIQUE_JOURS = 2;

    private AtelierRules() {}

    public static int delaiReservation(TypeParticipant type) {
        return switch (type) {
            case PARTENAIRE -> PARTENAIRE_JOURS;
            case ABONNE -> ABONNE_JOURS;
            case VISITEUR -> VISITEUR_JOURS;
        };
    }
}
