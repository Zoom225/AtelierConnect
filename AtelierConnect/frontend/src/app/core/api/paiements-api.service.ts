import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PaiementResponse } from '../../shared/models/reservation.model';
import { apiUrl } from './api-url';

export interface DetteResponse {
  solde: number;
  suspensionActive: boolean;
  mouvements: Array<{
    id: number;
    nature: 'SOLDE_INITIAL' | 'IMPUTATION_PRIVE' | 'IMPUTATION_IMPAYE' | 'REGLEMENT_DETTE' | 'REGLEMENT_RESERVATION';
    montant: number;
    description: string;
    atelierId: number | null;
    dateOperation: string;
  }>;
}

@Injectable({ providedIn: 'root' })
export class PaiementsApiService {
  constructor(private readonly http: HttpClient) {}

  getDebt(): Observable<DetteResponse> {
    return this.http.get<DetteResponse>(apiUrl('/paiements/dette'));
  }

  payDebt(montantAttendu: number, cleRequete: string): Observable<DetteResponse> {
    return this.http.post<DetteResponse>(apiUrl('/paiements/dette/pay'), { montantAttendu, cleRequete });
  }

  pay(reservationId: number, _participantId?: number): Observable<PaiementResponse> {
    return this.http.post<PaiementResponse>(
      apiUrl(`/paiements/reservation/${reservationId}/pay`),
      {}
    );
  }

  getById(id: number): Observable<PaiementResponse> {
    return this.http.get<PaiementResponse>(apiUrl(`/paiements/${id}`));
  }

  getByReservation(reservationId: number): Observable<PaiementResponse> {
    return this.http.get<PaiementResponse>(apiUrl(`/paiements/reservation/${reservationId}`));
  }

  getByParticipant(participantId: number): Observable<PaiementResponse[]> {
    return this.http.get<PaiementResponse[]>(apiUrl(`/paiements/participant/${participantId}`));
  }
}
