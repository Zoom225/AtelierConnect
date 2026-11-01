import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ParticipantRequest, ParticipantResponse } from '../../shared/models/participant.model';
import { apiUrl } from './api-url';

@Injectable({ providedIn: 'root' })
export class ParticipantsApiService {
  constructor(private readonly http: HttpClient) {}

  getAll(): Observable<ParticipantResponse[]> {
    return this.http.get<ParticipantResponse[]>(apiUrl('/participants'));
  }

  getById(id: number): Observable<ParticipantResponse> {
    return this.http.get<ParticipantResponse>(apiUrl(`/participants/${id}`));
  }

  getCurrent(): Observable<ParticipantResponse> {
    return this.http.get<ParticipantResponse>(apiUrl('/participants/me'));
  }

  getByMatricule(matricule: string): Observable<ParticipantResponse> {
    return this.http.get<ParticipantResponse>(apiUrl(`/participants/matricule/${matricule}`));
  }

  hasPenalty(id: number): Observable<boolean> {
    return this.http.get<boolean>(apiUrl(`/participants/${id}/penalty`));
  }

  hasBalance(id: number): Observable<boolean> {
    return this.http.get<boolean>(apiUrl(`/participants/${id}/balance`));
  }

  create(payload: ParticipantRequest): Observable<ParticipantResponse> {
    return this.http.post<ParticipantResponse>(apiUrl('/participants'), payload);
  }

  update(id: number, payload: ParticipantRequest): Observable<ParticipantResponse> {
    return this.http.put<ParticipantResponse>(apiUrl(`/participants/${id}`), payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(apiUrl(`/participants/${id}`));
  }
}

