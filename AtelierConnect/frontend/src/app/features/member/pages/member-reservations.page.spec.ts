import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AteliersApiService } from '../../../core/api/ateliers-api.service';
import { PaiementsApiService } from '../../../core/api/paiements-api.service';
import { ReservationsApiService } from '../../../core/api/reservations-api.service';
import { MemberSessionService } from '../../../core/auth/member-session.service';
import { AtelierResponse } from '../../../shared/models/atelier.model';
import { MemberReservationsPage } from './member-reservations.page';

describe('MemberReservationsPage', () => {
  const organizedAteliers: AtelierResponse[] = [
    {
      id: 1,
      salleId: 1,
      salleNom: 'Court A',
      centreNom: 'Lyon',
      organisateurId: 1,
      organisateurNom: 'Tom Bernard',
      date: '2026-05-14',
      heureDebut: '09:00:00',
      heureFin: '10:30:00',
      typeAtelier: 'PUBLIC',
      statut: 'PLANIFIE',
      nbParticipantsActuels: 2,
      prixParParticipant: 15,
      dateConversionPublic: null
    },
    {
      id: 2,
      salleId: 1,
      salleNom: 'Court A',
      centreNom: 'Lyon',
      organisateurId: 1,
      organisateurNom: 'Tom Bernard',
      date: '2026-05-20',
      heureDebut: '11:00:00',
      heureFin: '12:30:00',
      typeAtelier: 'PRIVE',
      statut: 'PLANIFIE',
      nbParticipantsActuels: 1,
      prixParParticipant: 15,
      dateConversionPublic: null
    }
  ];

  let ateliersApiMock: {
    getByOrganisateur: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    cancel: ReturnType<typeof vi.fn>;
    consentToPublicOpening: ReturnType<typeof vi.fn>;
  };
  let reservationsApiMock: {
    getByParticipant: ReturnType<typeof vi.fn>;
    getByAtelier: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    createPrivateInvites: ReturnType<typeof vi.fn>;
    cancel: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    ateliersApiMock = {
      getByOrganisateur: vi.fn().mockReturnValue(of(organizedAteliers)),
      update: vi.fn().mockImplementation((id: number, payload: Partial<AtelierResponse>) =>
        of({ ...organizedAteliers.find((m) => m.id === id)!, ...payload } as AtelierResponse)
      ),
      cancel: vi.fn().mockReturnValue(of(void 0)),
      consentToPublicOpening: vi.fn().mockReturnValue(of(void 0))
    };

    reservationsApiMock = {
      getByParticipant: vi.fn().mockReturnValue(of([])),
      getByAtelier: vi.fn().mockReturnValue(of([])),
      create: vi.fn().mockReturnValue(of({ id: 1 })),
      createPrivateInvites: vi.fn().mockReturnValue(of([])),
      cancel: vi.fn().mockReturnValue(of(void 0))
    };

    await TestBed.configureTestingModule({
      imports: [MemberReservationsPage],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: AteliersApiService, useValue: ateliersApiMock },
        { provide: ReservationsApiService, useValue: reservationsApiMock },
        { provide: PaiementsApiService, useValue: { pay: vi.fn() } },
        { provide: MemberSessionService, useValue: { memberId: vi.fn().mockReturnValue(1) } }
      ]
    }).compileComponents();

    const router = TestBed.inject(Router);
    vi.spyOn(router, 'getCurrentNavigation').mockReturnValue({
      extras: {
        state: {
          createdAtelierId: 2,
          successMessage: 'Atelier privé créé. Les invitations sont en attente de paiement des participants.'
        }
      }
    } as any);
    window.history.replaceState(
      {
        createdAtelierId: 2,
        successMessage: 'Atelier privé créé. Les invitations sont en attente de paiement des participants.'
      },
      ''
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
    TestBed.resetTestingModule();
  });

  it('n expose que les matchs PRIVE dans la gestion des participants', () => {
    const fixture = TestBed.createComponent(MemberReservationsPage);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.organisedAteliers()).toHaveLength(2);
    expect(component.managedAteliers()).toHaveLength(1);
    expect(component.managedAteliers()[0].id).toBe(2);
    expect(component.managedAtelierId()).toBe(2);
    expect(component.message()).toContain('Atelier privé créé');
    expect(fixture.nativeElement.textContent).toContain('Court A');
    expect(fixture.nativeElement.textContent).toContain('Planifie');
  });

  it('bloque la selection d un atelier PUBLIC dans la gestion des participants', () => {
    const fixture = TestBed.createComponent(MemberReservationsPage);
    const component = fixture.componentInstance;
    reservationsApiMock.getByAtelier.mockClear();

    component.onManagedAtelierChange(1);

    expect(component.managedAtelierId()).toBeNull();
    expect(component.errorMessage()).toContain('réservée aux ateliers PRIVÉS');
    expect(reservationsApiMock.getByAtelier).not.toHaveBeenCalled();
  });

  it('modifie un atelier PRIVÉ organisé', () => {
    const fixture = TestBed.createComponent(MemberReservationsPage);
    const component = fixture.componentInstance;

    component.onManagedAtelierChange(2);
    component.managedAtelierForm.patchValue({ date: '2026-05-22', heureDebut: '13:30', typeAtelier: 'PRIVE' });
    component.updateManagedAtelier();

    expect(ateliersApiMock.update).toHaveBeenCalledWith(
      2,
      expect.objectContaining({
        salleId: 1,
        organisateurId: 1,
        date: '2026-05-22',
        heureDebut: '13:30',
        typeAtelier: 'PRIVE'
      })
    );
    expect(component.message()).toContain('Atelier mis à jour');
  });

  it('enregistre uniquement un consentement explicitement confirmé', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const component = TestBed.createComponent(MemberReservationsPage).componentInstance;

    component.onManagedAtelierChange(2);
    component.consentToPublicOpening();

    expect(ateliersApiMock.consentToPublicOpening).toHaveBeenCalledWith(2);
    expect(component.selectedManagedAtelier()?.accordOuverturePublique).toBe(true);
    expect(component.message()).toContain('accord');
  });

  it('ne transmet aucun consentement si l’organisateur refuse de confirmer', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    const component = TestBed.createComponent(MemberReservationsPage).componentInstance;

    component.onManagedAtelierChange(2);
    component.consentToPublicOpening();

    expect(ateliersApiMock.consentToPublicOpening).not.toHaveBeenCalled();
  });

  it('ne marque pas un autre atelier privé comme consenti', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const secondPrivate = { ...organizedAteliers.find(atelier => atelier.id === 2)!, id: 3,
      accordOuverturePublique: false };
    ateliersApiMock.getByOrganisateur.mockReturnValue(of([...organizedAteliers, secondPrivate]));
    const component = TestBed.createComponent(MemberReservationsPage).componentInstance;
    component.onManagedAtelierChange(2);
    component.consentToPublicOpening();
    expect(component.organisedAteliers().find(atelier => atelier.id === 2)?.accordOuverturePublique).toBe(true);
    expect(component.organisedAteliers().find(atelier => atelier.id === 3)?.accordOuverturePublique).toBe(false);
  });

  it('restaure l’accord depuis le DTO après rechargement et empêche son renvoi', () => {
    const ateliersWithPersistedConsent: AtelierResponse[] = organizedAteliers.map(atelier =>
      atelier.id === 2
        ? { ...atelier, accordOuverturePublique: true, dateAccordOuverturePublique: '2026-10-31T12:00:00' }
        : atelier
    );
    ateliersApiMock.getByOrganisateur.mockReturnValue(of(ateliersWithPersistedConsent));

    const fixture = TestBed.createComponent(MemberReservationsPage);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    expect(component.selectedManagedAtelier()?.accordOuverturePublique).toBe(true);
    const consentButton = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>
    ).find(button => button.textContent?.includes('accord pour une éventuelle ouverture'));
    expect(consentButton).toBeDefined();
    expect(consentButton?.disabled).toBe(true);

    component.consentToPublicOpening();
    expect(ateliersApiMock.consentToPublicOpening).not.toHaveBeenCalled();
  });

  it('supprime (annule) un atelier PRIVÉ organisé', () => {
    const fixture = TestBed.createComponent(MemberReservationsPage);
    const component = fixture.componentInstance;

    component.onManagedAtelierChange(2);
    component.requestDeleteManagedAtelier();
    component.deleteManagedAtelier();

    expect(ateliersApiMock.cancel).toHaveBeenCalledWith(2, 1);
    expect(component.managedAtelierId()).toBeNull();
    expect(ateliersApiMock.getByOrganisateur).toHaveBeenCalled();
  });
});
