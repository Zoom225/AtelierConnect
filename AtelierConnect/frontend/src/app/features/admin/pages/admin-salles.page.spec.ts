import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CentresApiService } from '../../../core/api/centres-api.service';
import { SallesApiService } from '../../../core/api/salles-api.service';
import { AdminSessionService } from '../../../core/auth/admin-session.service';
import { CentreResponse, SalleResponse } from '../../../shared/models/centre-salle.model';
import { AdminSallesPage } from './admin-salles.page';

describe('AdminSallesPage', () => {
  const site: CentreResponse = {
    id: 1,
    nom: 'Padel Club Lyon',
    adresse: 'Lyon',
    heureOuverture: '08:00',
    heureFermeture: '22:00',
    dureeAtelierMinutes: 90,
    dureeEntreAtelierMinutes: 15,
    anneeCivile: 2026
  };

  const salles: SalleResponse[] = [
    { id: 10, nom: 'Court A', centreId: 1, centreNom: 'Padel Club Lyon', prix: 60 },
    { id: 11, nom: 'Court B', centreId: 1, centreNom: 'Padel Club Lyon', prix: 60 }
  ];

  let sallesApiMock: {
    getAll: ReturnType<typeof vi.fn>;
    getByCentre: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };
  let centresApiMock: {
    getAll: ReturnType<typeof vi.fn>;
    getById: ReturnType<typeof vi.fn>;
  };
  let adminSessionMock: {
    isGlobalAdmin: ReturnType<typeof vi.fn>;
    isCentreAdmin: ReturnType<typeof vi.fn>;
    centreId: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    sallesApiMock = {
      getAll: vi.fn().mockReturnValue(of(salles)),
      getByCentre: vi.fn().mockReturnValue(of(salles)),
      create: vi.fn().mockReturnValue(of(salles[0])),
      update: vi.fn().mockReturnValue(of(salles[0])),
      delete: vi.fn().mockReturnValue(of(void 0))
    };
    centresApiMock = {
      getAll: vi.fn().mockReturnValue(of([site])),
      getById: vi.fn().mockReturnValue(of(site))
    };
    adminSessionMock = {
      isGlobalAdmin: vi.fn().mockReturnValue(false),
      isCentreAdmin: vi.fn().mockReturnValue(true),
      centreId: vi.fn().mockReturnValue(1)
    };

    await TestBed.configureTestingModule({
      imports: [AdminSallesPage],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: SallesApiService, useValue: sallesApiMock },
        { provide: CentresApiService, useValue: centresApiMock },
        { provide: AdminSessionService, useValue: adminSessionMock }
      ]
    }).compileComponents();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    TestBed.resetTestingModule();
  });

  it('charge uniquement le site et les salles de l admin SITE', () => {
    const fixture = TestBed.createComponent(AdminSallesPage);
    const component = fixture.componentInstance;

    expect(sallesApiMock.getByCentre).toHaveBeenCalledWith(1);
    expect(sallesApiMock.getAll).not.toHaveBeenCalled();
    expect(centresApiMock.getById).toHaveBeenCalledWith(1);
    expect(centresApiMock.getAll).not.toHaveBeenCalled();
    expect(component.salles()).toEqual(salles);
    expect(component.centres()).toEqual([site]);
    expect(component.form.controls.centreId.value).toBe(1);
    expect(component.errorMessage()).toBe('');
  });
});
