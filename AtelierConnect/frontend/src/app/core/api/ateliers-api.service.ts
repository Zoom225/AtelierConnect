import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateAtelierRequest, AtelierRequest, AtelierResponse } from '../../shared/models/atelier.model';
import { apiUrl } from './api-url';
import { MemberSessionService } from '../auth/member-session.service';

@Injectable({ providedIn: 'root' })
export class AteliersApiService {
  private readonly memberSession = inject(MemberSessionService);

  constructor(private readonly http: HttpClient) {}

  getAll(): Observable<AtelierResponse[]> {
    return this.http.get<AtelierResponse[]>(apiUrl('/ateliers'));
  }

  getById(id: number): Observable<AtelierResponse> {
    return this.http.get<AtelierResponse>(apiUrl(`/ateliers/${id}`));
  }

  getPublic(): Observable<AtelierResponse[]> {
    return this.http.get<AtelierResponse[]>(apiUrl('/ateliers/public'));
  }

  getByCentre(centreId: number): Observable<AtelierResponse[]> {
    return this.http.get<AtelierResponse[]>(apiUrl(`/ateliers/centre/${centreId}`));
  }

  getByOrganisateur(organisateurId: number): Observable<AtelierResponse[]> {
    return this.http.get<AtelierResponse[]>(apiUrl(`/ateliers/organisateur/${organisateurId}`));
  }

  create(payload: CreateAtelierRequest): Observable<AtelierResponse> {
    const token = this.memberSession.token();
    const headers = token
      ? new HttpHeaders({ Authorization: `Bearer ${token}` })
      : new HttpHeaders();
    return this.http.post<AtelierResponse>(apiUrl('/ateliers'), payload, { headers });
  }

  update(id: number, payload: AtelierRequest): Observable<AtelierResponse> {
    return this.http.put<AtelierResponse>(apiUrl(`/ateliers/${id}`), payload);
  }

  cancel(id: number, requesterId: number): Observable<void> {
    return this.http.patch<void>(apiUrl(`/ateliers/${id}/cancel?requesterId=${requesterId}`), {});
  }

  convertToPublic(id: number): Observable<void> {
    return this.http.patch<void>(apiUrl(`/ateliers/${id}/convert-public`), {});
  }

  /** Consentement explicite de l'organisateur, contrôlé côté Spring Security. */
  consentToPublicOpening(id: number): Observable<void> {
    return this.http.patch<void>(apiUrl(`/ateliers/${id}/consent-open-public`), {});
  }
}
