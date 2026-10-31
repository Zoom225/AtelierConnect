import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  TerrainRequest,
  TerrainResponse
} from '../../shared/models/site-terrain.model';
import { apiUrl } from './api-url';

@Injectable({ providedIn: 'root' })
export class SallesApiService {
  constructor(private readonly http: HttpClient) {}

  getAll(): Observable<TerrainResponse[]> {
    return this.http.get<TerrainResponse[]>(apiUrl('/salles'));
  }

  getById(id: number): Observable<TerrainResponse> {
    return this.http.get<TerrainResponse>(apiUrl(`/salles/${id}`));
  }

  getBySite(siteId: number): Observable<TerrainResponse[]> {
    return this.http.get<TerrainResponse[]>(apiUrl(`/salles/site/${siteId}`));
  }

  create(payload: TerrainRequest): Observable<TerrainResponse> {
    return this.http.post<TerrainResponse>(apiUrl('/salles'), payload);
  }

  update(id: number, payload: TerrainRequest): Observable<TerrainResponse> {
    return this.http.put<TerrainResponse>(apiUrl(`/salles/${id}`), payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(apiUrl(`/salles/${id}`));
  }
}

