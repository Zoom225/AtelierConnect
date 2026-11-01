import { StatutPaiement, StatutReservation } from './enums.model';

export interface ReservationRequest {
  atelierId: number;
  participantId: number;
  requesterId: number;
}

export interface ReservationInviteRequest {
  atelierId: number;
  inviteeMatricules: string[];
}

export interface PaiementResponse {
  id: number;
  reservationId: number | null;
  montant: number;
  statut: StatutPaiement;
  datePaiement: string | null;
}

export interface ReservationResponse {
  id: number;
  atelierId: number;
  atelierDateTime: string;
  participantId: number;
  participantNom: string;
  statut: StatutReservation;
  paiement: PaiementResponse | null;
}

