import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  SalleRequest,
  SalleResponse
} from '../../shared/models/centre-salle.model';
import { apiUrl } from './api-url';

@Injectable({ providedIn: 'root' })
export class SallesApiService {
  constructor(private readonly http: HttpClient) {}

  getAll(): Observable<SalleResponse[]> {
    return this.http.get<SalleResponse[]>(apiUrl('/salles'));
  }

  getById(id: number): Observable<SalleResponse> {
    return this.http.get<SalleResponse>(apiUrl(`/salles/${id}`));
  }

  getByCentre(centreId: number): Observable<SalleResponse[]> {
    return this.http.get<SalleResponse[]>(apiUrl(`/salles/centre/${centreId}`));
  }

  create(payload: SalleRequest): Observable<SalleResponse> {
    return this.http.post<SalleResponse>(apiUrl('/salles'), payload);
  }

  update(id: number, payload: SalleRequest): Observable<SalleResponse> {
    return this.http.put<SalleResponse>(apiUrl(`/salles/${id}`), payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(apiUrl(`/salles/${id}`));
  }
}

