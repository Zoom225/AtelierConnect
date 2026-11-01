import { LegacyParticipantCategory, TypeParticipant, toParticipantCategory } from './enums.model';

export interface LoginRequest {
  matricule: string;
  password: string; // Added password field
}

export interface ParticipantRequest {
  matricule: string;
  nom: string;
  prenom: string;
  email: string;
  categorie: TypeParticipant;
  centreId?: number;
  password?: string;
}

export interface ParticipantResponse {
  id: number;
  matricule: string;
  nom: string;
  prenom: string;
  email: string;
  categorie: TypeParticipant;
  centreId: number | null;
  centreNom: string | null;
  solde: number;
  token?: string;
}

export interface LoginResponse extends ParticipantResponse {
  token: string;
}

/** Préfère la catégorie native du backend avec repli sur le champ JSON historique. */
export function categorieParticipant(participant: {
  categorie?: TypeParticipant;
  typeParticipant?: LegacyParticipantCategory;
  typeMembre?: LegacyParticipantCategory;
}): TypeParticipant {
  return participant.categorie ?? toParticipantCategory(participant.typeParticipant ?? participant.typeMembre ?? 'LIBRE');
}
