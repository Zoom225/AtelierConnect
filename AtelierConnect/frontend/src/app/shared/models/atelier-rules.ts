import { TypeMembre } from './enums.model';

// Nouvelles règles AtelierConnect. Les codes historiques du backend restent
// acceptés pendant la transition de l'interface Angular.
export const ATELIER_CAPACITY = 6;
export const ATELIER_DURATION_MINUTES = 120;
export const ATELIER_BREAK_MINUTES = 20;
export const ATELIER_TOTAL_PRICE_EUR = 90;
export const ATELIER_PRICE_PER_PARTICIPANT_EUR = 15;

export const BOOKING_MAX_DAYS: Record<TypeMembre, number> = {
  GLOBAL: 30, // PARTENAIRE
  SITE: 15,   // ABONNE
  LIBRE: 7    // VISITEUR
};
