import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CentreRequest, CentreResponse } from '../../shared/models/centre-salle.model';
import { apiUrl } from './api-url';

@Injectable({ providedIn: 'root' })
export class CentresApiService {
  constructor(private readonly http: HttpClient) {}

  getAll(): Observable<CentreResponse[]> {
    return this.http.get<CentreResponse[]>(apiUrl('/centres'));
  }

  getById(id: number): Observable<CentreResponse> {
    return this.http.get<CentreResponse>(apiUrl(`/centres/${id}`));
  }

  create(payload: CentreRequest): Observable<CentreResponse> {
    return this.http.post<CentreResponse>(apiUrl('/centres'), payload);
  }

  update(id: number, payload: CentreRequest): Observable<CentreResponse> {
    return this.http.put<CentreResponse>(apiUrl(`/centres/${id}`), payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(apiUrl(`/centres/${id}`));
  }
}

