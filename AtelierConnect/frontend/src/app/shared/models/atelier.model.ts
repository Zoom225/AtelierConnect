import { StatutAtelier, TypeAtelier } from './enums.model';

export interface AtelierRequest {
  salleId: number;
  organisateurId: number;
  date: string;
  heureDebut: string;
  typeAtelier: TypeAtelier;
}

export interface AtelierResponse {
  id: number;
  salleId: number;
  salleNom: string;
  centreNom: string;
  organisateurId: number;
  organisateurNom: string;
  date: string;
  heureDebut: string;
  heureFin: string;
  typeAtelier: TypeAtelier;
  statut: StatutAtelier;
  nbParticipantsActuels: number;
  prixParParticipant: number;
  dateConversionPublic: string | null;
  /** Accord daté exposé par le backend. Optionnel pour la compatibilité des anciens tests. */
  accordOuverturePublique?: boolean | null;
  dateAccordOuverturePublique?: string | null;
}

export interface CreateAtelierRequest {
  salleId: number;
  atelierDate: string;
  atelierType: TypeAtelier;
}
