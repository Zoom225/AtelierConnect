import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AteliersApiService } from '../../../core/api/ateliers-api.service';
import { ReservationsApiService } from '../../../core/api/reservations-api.service';
import { CentresApiService } from '../../../core/api/centres-api.service';
import { MemberSessionService } from '../../../core/auth/member-session.service';
import { AtelierResponse } from '../../../shared/models/atelier.model';
import { MemberPublicAteliersPage } from './member-public-ateliers.page';

describe('MemberPublicAteliersPage', () => {
  const farFuture = '2099-05-20';
  const ateliers: AtelierResponse[] = [
    {
      id: 1,
      salleId: 1,
      salleNom: 'Court A',
      centreNom: 'Lyon',
      organisateurId: 1,
      organisateurNom: 'Tom Bernard',
      date: farFuture,
      heureDebut: '11:00:00',
      heureFin: '12:30:00',
      typeAtelier: 'PUBLIC',
      statut: 'PLANIFIE',
      nbParticipantsActuels: 2,
      prixParParticipant: 15,
      dateConversionPublic: null
    },
    {
      id: 2,
      salleId: 2,
      salleNom: 'Court B',
      centreNom: 'Paris',
      organisateurId: 9,
      organisateurNom: 'Emma Dubois',
      date: farFuture,
      heureDebut: '13:00:00',
      heureFin: '14:30:00',
      typeAtelier: 'PUBLIC',
      statut: 'PLANIFIE',
      nbParticipantsActuels: 1,
      prixParParticipant: 15,
      dateConversionPublic: null
    }
  ];

  let ateliersApiMock: {
    getPublic: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    cancel: ReturnType<typeof vi.fn>;
  };
  let reservationsApiMock: { create: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    ateliersApiMock = {
      getPublic: vi.fn().mockReturnValue(of(ateliers)),
      update: vi.fn().mockImplementation((id: number, payload: Partial<AtelierResponse>) =>
        of({ ...ateliers.find((m) => m.id === id)!, ...payload } as AtelierResponse)
      ),
      cancel: vi.fn().mockReturnValue(of(void 0))
    };

    reservationsApiMock = {
      create: vi.fn().mockReturnValue(of({ id: 10 }))
    };

    await TestBed.configureTestingModule({
      imports: [MemberPublicAteliersPage],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: AteliersApiService, useValue: ateliersApiMock },
        { provide: CentresApiService, useValue: { getAll: vi.fn().mockReturnValue(of([{ id: 1, nom: 'Lyon' }, { id: 2, nom: 'Paris' }])) } },
        { provide: ReservationsApiService, useValue: reservationsApiMock },
        { provide: MemberSessionService, useValue: { memberId: vi.fn().mockReturnValue(1) } }
      ]
    }).compileComponents();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    TestBed.resetTestingModule();
  });

  it('identifie correctement les matchs de l’organisateur connecté', () => {
    const fixture = TestBed.createComponent(MemberPublicAteliersPage);
    const component = fixture.componentInstance;

    expect(component.isOrganizer(ateliers[0])).toBe(true);
    expect(component.isOrganizer(ateliers[1])).toBe(false);
  });

  it('affiche les matchs de l utilisateur en premier dans la liste filtree', () => {
    const fixture = TestBed.createComponent(MemberPublicAteliersPage);
    const component = fixture.componentInstance;

    const ordered = component.filteredAteliers();

    expect(ordered[0].id).toBe(1);
    expect(component.isOrganizer(ordered[0])).toBe(true);
  });

  it('affiche le badge Mon atelier pour le atelier organisé par le participant connecté', () => {
    const fixture = TestBed.createComponent(MemberPublicAteliersPage);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Mon atelier');
  });

  it('met à jour un atelier organisé depuis la liste publique', () => {
    const fixture = TestBed.createComponent(MemberPublicAteliersPage);
    const component = fixture.componentInstance;

    component.startEdit(ateliers[0]);
    component.editForm.patchValue({ date: '2099-05-22', heureDebut: '10:30', typeAtelier: 'PRIVE' });
    component.saveEdit(ateliers[0]);

    expect(ateliersApiMock.update).toHaveBeenCalledWith(
      1,
      expect.objectContaining({
        salleId: 1,
        organisateurId: 1,
        date: '2099-05-22',
        heureDebut: '10:30',
        typeAtelier: 'PRIVE'
      })
    );
  });

  it('supprime (annule) un atelier organisé depuis la liste publique', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const fixture = TestBed.createComponent(MemberPublicAteliersPage);
    const component = fixture.componentInstance;

    component.cancelOwnAtelier(ateliers[0]);

    expect(ateliersApiMock.cancel).toHaveBeenCalledWith(1, 1);
  });

  it('affiche un message clair après une demande de participation', () => {
    const fixture = TestBed.createComponent(MemberPublicAteliersPage);
    const component = fixture.componentInstance;

    component.joinAtelier(ateliers[1]);

    expect(component.message()).toContain("Votre place ne sera confirmée qu'après paiement");
  });
});
