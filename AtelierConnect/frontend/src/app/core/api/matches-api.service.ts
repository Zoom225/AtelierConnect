import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateMatchRequest, MatchRequest, MatchResponse } from '../../shared/models/match.model';
import { apiUrl } from './api-url';
import { MemberSessionService } from '../auth/member-session.service';

@Injectable({ providedIn: 'root' })
export class MatchesApiService {
  private readonly memberSession = inject(MemberSessionService);

  constructor(private readonly http: HttpClient) {}

  getAll(): Observable<MatchResponse[]> {
    return this.http.get<MatchResponse[]>(apiUrl('/ateliers'));
  }

  getById(id: number): Observable<MatchResponse> {
    return this.http.get<MatchResponse>(apiUrl(`/ateliers/${id}`));
  }

  getPublic(): Observable<MatchResponse[]> {
    return this.http.get<MatchResponse[]>(apiUrl('/ateliers/public'));
  }

  getBySite(siteId: number): Observable<MatchResponse[]> {
    return this.http.get<MatchResponse[]>(apiUrl(`/ateliers/site/${siteId}`));
  }

  getByOrganisateur(organisateurId: number): Observable<MatchResponse[]> {
    return this.http.get<MatchResponse[]>(apiUrl(`/ateliers/organisateur/${organisateurId}`));
  }

  create(payload: CreateMatchRequest): Observable<MatchResponse> {
    const token = this.memberSession.token();
    const headers = token
      ? new HttpHeaders({ Authorization: `Bearer ${token}` })
      : new HttpHeaders();
    return this.http.post<MatchResponse>(apiUrl('/ateliers'), payload, { headers });
  }

  update(id: number, payload: MatchRequest): Observable<MatchResponse> {
    return this.http.put<MatchResponse>(apiUrl(`/ateliers/${id}`), payload);
  }

  cancel(id: number, requesterId: number): Observable<void> {
    return this.http.patch<void>(apiUrl(`/ateliers/${id}/cancel?requesterId=${requesterId}`), {});
  }

  convertToPublic(id: number): Observable<void> {
    return this.http.patch<void>(apiUrl(`/ateliers/${id}/convert-public`), {});
  }
}
