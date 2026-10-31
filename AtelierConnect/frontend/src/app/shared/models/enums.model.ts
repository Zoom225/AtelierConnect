export type TypeAdministrateur = 'GLOBAL' | 'SITE';
export type TypeMembre = 'GLOBAL' | 'SITE' | 'LIBRE';
export type TypeMatch = 'PRIVE' | 'PUBLIC';
export type StatutMatch = 'PLANIFIE' | 'COMPLET' | 'ANNULE';
export type StatutPaiement = 'EN_ATTENTE' | 'PAYE' | 'REMBOURSE' | 'ANNULE';
export type StatutReservation = 'EN_ATTENTE' | 'CONFIRMEE' | 'ANNULEE';

/** Catégories métier AtelierConnect ; les anciens codes sont conservés dans les API transitoires. */
export type TypeParticipant = 'PARTENAIRE' | 'ABONNE' | 'VISITEUR';

export function toParticipantCategory(type: TypeMembre): TypeParticipant {
  switch (type) {
    case 'GLOBAL': return 'PARTENAIRE';
    case 'SITE': return 'ABONNE';
    case 'LIBRE': return 'VISITEUR';
  }
}
