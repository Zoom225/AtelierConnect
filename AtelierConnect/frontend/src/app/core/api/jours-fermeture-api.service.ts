import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { JourFermetureRequest, JourFermetureResponse } from '../../shared/models/centre-salle.model';
import { apiUrl } from './api-url';

@Injectable({ providedIn: 'root' })
export class JoursFermetureApiService {
  constructor(private readonly http: HttpClient) {}

  getAll(): Observable<JourFermetureResponse[]> {
    return this.http.get<JourFermetureResponse[]>(apiUrl('/jours-fermeture'));
  }

  getGlobal(): Observable<JourFermetureResponse[]> {
    return this.http.get<JourFermetureResponse[]>(apiUrl('/jours-fermeture/global'));
  }

  getByCentre(centreId: number): Observable<JourFermetureResponse[]> {
    return this.http.get<JourFermetureResponse[]>(apiUrl(`/jours-fermeture/centre/${centreId}`));
  }

  create(payload: JourFermetureRequest): Observable<JourFermetureResponse> {
    return this.http.post<JourFermetureResponse>(apiUrl('/jours-fermeture'), payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(apiUrl(`/jours-fermeture/${id}`));
  }
}

