import { TypeMembre, TypeParticipant, toParticipantCategory } from './enums.model';

export interface LoginRequest {
  matricule: string;
  password: string; // Added password field
}

export interface MembreRequest {
  matricule: string;
  nom: string;
  prenom: string;
  email: string;
  typeMembre: TypeMembre;
  categorie?: TypeParticipant;
  siteId?: number;
  password?: string;
}

export interface MembreResponse {
  id: number;
  matricule: string;
  nom: string;
  prenom: string;
  email: string;
  typeMembre: TypeMembre;
  categorie?: TypeParticipant;
  siteId: number | null;
  siteNom: string | null;
  solde: number;
  token?: string;
}

export interface LoginResponse extends MembreResponse {
  token: string;
}

/** Préfère la catégorie native du backend avec repli sur le champ JSON historique. */
export function categorieParticipant(participant: Pick<MembreResponse, 'categorie' | 'typeMembre'>): TypeParticipant {
  return participant.categorie ?? toParticipantCategory(participant.typeMembre);
}
