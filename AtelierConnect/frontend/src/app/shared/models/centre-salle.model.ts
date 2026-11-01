export interface CentreRequest {
  nom: string;
  adresse: string;
  heureOuverture: string;
  heureFermeture: string;
  dureeAtelierMinutes: number;
  dureeEntreAtelierMinutes: number;
  anneeCivile: number;
  ouvertVisiteurs?: boolean | null;
}

export interface CentreResponse {
  id: number;
  nom: string;
  adresse: string;
  heureOuverture: string;
  heureFermeture: string;
  dureeAtelierMinutes: number;
  dureeEntreAtelierMinutes: number;
  anneeCivile: number;
  ouvertVisiteurs?: boolean | null;
}

export interface SalleRequest {
  nom: string;
  centreId: number;
}

export interface SalleResponse {
  id: number;
  nom: string;
  centreId: number;
  centreNom: string;
  prix: number | null;
}

export interface JourFermetureRequest {
  date: string;
  raison: string;
  global: boolean;
  centreId: number | null;
}

export interface JourFermetureResponse {
  id: number;
  date: string;
  raison: string | null;
  global: boolean;
  centreId: number | null;
  centreNom: string | null;
}
