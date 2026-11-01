import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { JoursFermetureApiService } from '../../../core/api/jours-fermeture-api.service';
import { CentresApiService } from '../../../core/api/centres-api.service';
import { AdminSessionService } from '../../../core/auth/admin-session.service';
import { JourFermetureResponse, CentreResponse } from '../../../shared/models/centre-salle.model';
import { AdminJoursFermeturePage } from './admin-jours-fermeture.page';

describe('AdminJoursFermeturePage', () => {
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

  const jours: JourFermetureResponse[] = [
    { id: 1, date: '2026-08-10', raison: 'Maintenance', global: true, centreId: null, centreNom: null },
    { id: 2, date: '2026-08-11', raison: 'Tournoi', global: false, centreId: 1, centreNom: 'Padel Club Lyon' }
  ];

  let joursApiMock: {
    getAll: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
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
    joursApiMock = {
      getAll: vi.fn().mockReturnValue(of(jours)),
      create: vi.fn().mockReturnValue(of(jours[0])),
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
      imports: [AdminJoursFermeturePage],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: JoursFermetureApiService, useValue: joursApiMock },
        { provide: CentresApiService, useValue: centresApiMock },
        { provide: AdminSessionService, useValue: adminSessionMock }
      ]
    }).compileComponents();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    TestBed.resetTestingModule();
  });

  it('charge uniquement le site de l admin SITE sans erreur parasite', () => {
    const fixture = TestBed.createComponent(AdminJoursFermeturePage);
    const component = fixture.componentInstance;

    expect(joursApiMock.getAll).toHaveBeenCalled();
    expect(centresApiMock.getById).toHaveBeenCalledWith(1);
    expect(centresApiMock.getAll).not.toHaveBeenCalled();
    expect(component.centres()).toEqual([site]);
    expect(component.filteredJours()).toEqual(jours);
    expect(component.form.controls.centreId.value).toBe(1);
    expect(component.errorMessage()).toBe('');
  });
});
