export type TypeAdministrateur = 'GLOBAL' | 'SITE';
export type LegacyParticipantCategory = 'GLOBAL' | 'SITE' | 'LIBRE';
export type TypeAtelier = 'PRIVE' | 'PUBLIC';
export type StatutAtelier = 'PLANIFIE' | 'COMPLET' | 'ANNULE';
export type StatutPaiement = 'EN_ATTENTE' | 'PAYE' | 'REMBOURSE' | 'ANNULE';
export type StatutReservation = 'EN_ATTENTE' | 'CONFIRMEE' | 'ANNULEE';

/** Catégories métier AtelierConnect ; les anciens codes sont conservés dans les API transitoires. */
export type TypeParticipant = 'PARTENAIRE' | 'ABONNE' | 'VISITEUR';

export function toParticipantCategory(type: LegacyParticipantCategory | TypeParticipant): TypeParticipant {
  switch (type) {
    case 'GLOBAL': return 'PARTENAIRE';
    case 'SITE': return 'ABONNE';
    case 'LIBRE': return 'VISITEUR';
    default: return type;
  }
}
