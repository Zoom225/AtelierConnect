import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { MatSelect } from '@angular/material/select';
import { ActivatedRoute } from '@angular/router';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AteliersApiService } from '../../../core/api/ateliers-api.service';
import { ReservationsApiService } from '../../../core/api/reservations-api.service';
import { CentresApiService } from '../../../core/api/centres-api.service';
import { SallesApiService } from '../../../core/api/salles-api.service';
import { MemberSessionService } from '../../../core/auth/member-session.service';
import { AtelierResponse } from '../../../shared/models/atelier.model';
import { ParticipantResponse } from '../../../shared/models/participant.model';
import { CentreResponse, SalleResponse } from '../../../shared/models/centre-salle.model';
import { MemberCreateAtelierPage } from './member-create-atelier.page';

const formatDate = (date: Date): string => {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
};

describe('MemberCreateAtelierPage', () => {
  const member: ParticipantResponse = {
    id: 1,
    matricule: 'S10001',
    nom: 'Bernard',
    prenom: 'Tom',
    email: 'tom.bernard@email.com',
    categorie: 'ABONNE',
    centreId: 1,
    centreNom: 'Centre collaboratif Lyon',
    solde: 0
  };

  const centres: CentreResponse[] = [
    {
      id: 1,
      nom: 'Centre collaboratif Lyon',
      adresse: 'Lyon',
      heureOuverture: '08:00',
      heureFermeture: '22:00',
      dureeAtelierMinutes: 120,
      dureeEntreAtelierMinutes: 20,
      anneeCivile: 2026
    }
  ];

  const salles: SalleResponse[] = [
    { id: 10, nom: 'Salle A', centreId: 1, centreNom: 'Centre collaboratif Lyon' }
  ];

  const createdAtelier: AtelierResponse = {
    id: 99,
    salleId: 10,
    salleNom: 'Salle A',
    centreNom: 'Centre collaboratif Lyon',
    organisateurId: 1,
    organisateurNom: 'Tom Bernard',
    date: '2026-05-10',
    heureDebut: '09:00',
    heureFin: '10:30',
    typeAtelier: 'PRIVE',
    statut: 'PLANIFIE',
    nbParticipantsActuels: 1,
    prixParParticipant: 15,
    dateConversionPublic: null
  };

  let centresApiMock: { getAll: ReturnType<typeof vi.fn> };
  let sallesApiMock: { getByCentre: ReturnType<typeof vi.fn> };
  let ateliersApiMock: { create: ReturnType<typeof vi.fn> };
  let reservationsApiMock: { createPrivateInvites: ReturnType<typeof vi.fn> };
  let memberSessionMock: {
    isAuthenticated: ReturnType<typeof vi.fn>;
    member: ReturnType<typeof vi.fn>;
  };
  let activatedRouteMock: { snapshot: { queryParamMap: { get: ReturnType<typeof vi.fn> } } };

  beforeEach(async () => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 4, 10, 8, 0));
    centresApiMock = {
      getAll: vi.fn().mockReturnValue(of(centres))
    };
    sallesApiMock = {
      getByCentre: vi.fn().mockReturnValue(of(salles))
    };
    ateliersApiMock = {
      create: vi.fn().mockReturnValue(of(createdAtelier))
    };
    reservationsApiMock = {
      createPrivateInvites: vi.fn().mockReturnValue(of([]))
    };
    memberSessionMock = {
      isAuthenticated: vi.fn().mockReturnValue(true),
      member: vi.fn().mockReturnValue(member)
    };
    activatedRouteMock = {
      snapshot: {
        queryParamMap: {
          get: vi.fn().mockReturnValue(null)
        }
      }
    };

    await TestBed.configureTestingModule({
      imports: [MemberCreateAtelierPage],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: CentresApiService, useValue: centresApiMock },
        { provide: SallesApiService, useValue: sallesApiMock },
        { provide: AteliersApiService, useValue: ateliersApiMock },
        { provide: ReservationsApiService, useValue: reservationsApiMock },
        { provide: MemberSessionService, useValue: memberSessionMock },
        { provide: ActivatedRoute, useValue: activatedRouteMock }
      ]
    }).compileComponents();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
    TestBed.resetTestingModule();
  });

  it('affiche le tarif atelier de 90 EUR même si la salle conserve un ancien prix', () => {
    sallesApiMock.getByCentre.mockReturnValue(of([{ ...salles[0], prix: 60 }]));
    const fixture = TestBed.createComponent(MemberCreateAtelierPage);
    fixture.detectChanges();
    const select = fixture.debugElement.query(By.css('mat-select[formControlName="salleId"]')).componentInstance as MatSelect;
    select.open();
    fixture.detectChanges();
    const option = document.querySelector('mat-option');
    expect(option?.textContent).toContain('90 EUR');
    expect(option?.textContent).not.toContain('60 EUR');
    select.close();
  });

  it('initialise le site, le salle et la date minimale pour un participant SITE', () => {
    const fixture = TestBed.createComponent(MemberCreateAtelierPage);
    const component = fixture.componentInstance;

    expect(component.isCentreMember()).toBe(true);
    expect(component.form.controls.centreId.value).toBe(1);
    expect(component.form.controls.salleId.value).toBe(10);
    expect(component.form.controls.date.value).toBe('2026-05-11');
    expect(sallesApiMock.getByCentre).toHaveBeenCalledWith(1);
  });

  it('préselectionne le type PRIVE si le query param type=PRIVE est fourni', () => {
    activatedRouteMock.snapshot.queryParamMap.get.mockReturnValue('PRIVE');
    const fixture = TestBed.createComponent(MemberCreateAtelierPage);
    const component = fixture.componentInstance;

    expect(component.form.controls.typeAtelier.value).toBe('PRIVE');
  });

  it('bloque un visiteur si aucun centre ne lui est ouvert sans envoyer de requête de création', () => {
    memberSessionMock.member.mockReturnValue({ ...member, categorie: 'VISITEUR', centreId: null });
    centresApiMock.getAll.mockReturnValue(of([
      { ...centres[0], ouvertVisiteurs: null },
      { ...centres[0], id: 2, ouvertVisiteurs: false }
    ]));
    const fixture = TestBed.createComponent(MemberCreateAtelierPage);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    component.submit();
    expect(component.centres()).toEqual([]);
    expect(sallesApiMock.getByCentre).not.toHaveBeenCalled();
    expect(ateliersApiMock.create).not.toHaveBeenCalled();
    expect(component.errorMessage()).toContain("Aucun centre n'est actuellement ouvert aux visiteurs");
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
  });

  it('propose au visiteur uniquement les centres explicitement ouverts', () => {
    memberSessionMock.member.mockReturnValue({ ...member, categorie: 'VISITEUR', centreId: null });
    const openCentre = { ...centres[0], id: 2, ouvertVisiteurs: true };
    centresApiMock.getAll.mockReturnValue(of([
      { ...centres[0], ouvertVisiteurs: false }, openCentre
    ]));
    const component = TestBed.createComponent(MemberCreateAtelierPage).componentInstance;
    expect(component.centres()).toEqual([openCentre]);
    expect(component.form.controls.centreId.value).toBe(2);
    expect(sallesApiMock.getByCentre).toHaveBeenCalledWith(2);
    expect(component.errorMessage()).toBe('');
  });

  it('conserve le centre de rattachement de l’abonné même si les visiteurs sont refusés', () => {
    centresApiMock.getAll.mockReturnValue(of([{ ...centres[0], ouvertVisiteurs: false }]));
    const component = TestBed.createComponent(MemberCreateAtelierPage).componentInstance;
    expect(component.centres()).toHaveLength(1);
    expect(component.form.controls.centreId.value).toBe(1);
    expect(component.errorMessage()).toBe('');
  });

  it('ne masque pas les erreurs inattendues renvoyées pendant le chargement', () => {
    centresApiMock.getAll.mockReturnValue(throwError(() => ({ error: { message: 'Une erreur inattendue est survenue' } })));
    const component = TestBed.createComponent(MemberCreateAtelierPage).componentInstance;
    expect(component.errorMessage()).toBe('Une erreur inattendue est survenue');
  });

  it('propose une date future même si la page est ouverte après 09:00', () => {
    vi.setSystemTime(new Date(2026, 4, 10, 21, 0));
    const component = TestBed.createComponent(MemberCreateAtelierPage).componentInstance;
    expect(component.form.controls.date.value).toBe('2026-05-11');
    expect(component.form.controls.heureDebut.value).toBe('09:00');
    expect(component.form.hasError('pastAtelierDateTime')).toBe(false);
  });

  it('bloque une heure passée sans envoyer de POST et affiche une erreur lisible', () => {
    vi.setSystemTime(new Date(2026, 4, 10, 15, 0));
    const fixture = TestBed.createComponent(MemberCreateAtelierPage);
    const component = fixture.componentInstance;
    component.form.patchValue({ date: component.todayDate(), heureDebut: '09:00' });
    component.submit();
    fixture.detectChanges();
    expect(ateliersApiMock.create).not.toHaveBeenCalled();
    expect(component.form.hasError('pastAtelierDateTime')).toBe(true);
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('dans le futur');
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
  });

  it('autorise un créneau futur le jour même', () => {
    vi.setSystemTime(new Date(2026, 4, 10, 15, 0));
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const component = TestBed.createComponent(MemberCreateAtelierPage).componentInstance;
    component.form.patchValue({ date: component.todayDate(), heureDebut: '16:00' });
    component.submit();
    expect(ateliersApiMock.create).toHaveBeenCalledWith({
      salleId: 10, atelierDate: '2026-05-10T16:00:00', atelierType: 'PUBLIC'
    });
  });

  it('revérifie la date si le formulaire est resté ouvert trop longtemps', () => {
    const component = TestBed.createComponent(MemberCreateAtelierPage).componentInstance;
    component.form.patchValue({ date: component.todayDate(), heureDebut: '09:00' });
    expect(component.form.valid).toBe(true);
    vi.setSystemTime(new Date(2026, 4, 10, 10, 0));
    component.submit();
    expect(ateliersApiMock.create).not.toHaveBeenCalled();
    expect(component.errorMessage()).toContain('dans le futur');
  });

  it('bloque la soumission si la date est au delà de la dernière date autorisée', () => {
    const fixture = TestBed.createComponent(MemberCreateAtelierPage);
    const component = fixture.componentInstance;
    const tooLateDate = new Date(component.maxBookingDate());
    tooLateDate.setDate(tooLateDate.getDate() + 1);

    component.form.patchValue({
      date: formatDate(tooLateDate),
      heureDebut: '09:00',
      typeAtelier: 'PUBLIC'
    });

    component.submit();

    expect(component.form.controls.date.hasError('maxBookingDate')).toBe(true);
    expect(ateliersApiMock.create).not.toHaveBeenCalled();
    expect(component.errorMessage()).toContain('Dernière date autorisée');
    expect(component.errorMessage()).toContain(component.maxBookingDate());
  });

  it('crée un atelier privé et ajoute les participants invités', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    vi.useFakeTimers();
    const fixture = TestBed.createComponent(MemberCreateAtelierPage);
    const component = fixture.componentInstance;

    component.form.patchValue({
      date: component.todayDate(),
      heureDebut: '09:00',
      typeAtelier: 'PRIVE',
      player1: 'g1002',
      player2: 'l10001',
      player3: 'g1003',
      player4: 's10001',
      player5: 'g1004'
    });

    component.submit();

    expect(ateliersApiMock.create).toHaveBeenCalledWith({
      salleId: 10,
      atelierDate: `${component.todayDate()}T09:00:00`,
      atelierType: 'PRIVE',
    });
    expect(reservationsApiMock.createPrivateInvites).toHaveBeenCalledWith({
      atelierId: 99,
      inviteeMatricules: ['G1002', 'L10001', 'G1003', 'S10001', 'G1004']
    });
    expect(component.message()).toContain('Les invitations sont en attente de paiement');
    vi.runAllTimers();
    expect(navigateSpy).toHaveBeenCalledWith(
      ['/member/reservations'],
      expect.objectContaining({
        state: expect.objectContaining({
          createdAtelierId: 99
        })
      })
    );
    vi.useRealTimers();
  });

  it('traduit le message backend de délai en message frontend compréhensible', () => {
    ateliersApiMock.create.mockReturnValue(
      throwError(() => ({ error: { message: 'Le participant ABONNE ne peut pas réserver plus de 15 jours à l\'avance.' } }))
    );
    const fixture = TestBed.createComponent(MemberCreateAtelierPage);
    const component = fixture.componentInstance;

    component.form.patchValue({
      date: component.todayDate(),
      heureDebut: '09:00',
      typeAtelier: 'PUBLIC'
    });

    component.submit();

    expect(component.loading()).toBe(false);
    expect(component.errorMessage()).toContain('Le profil ABONNE ne peut pas réserver plus de 15 jours');
    expect(component.errorMessage()).toContain(component.maxBookingDate());
  });
  it('affiche une erreur métier sans code HTTP technique', () => {
    ateliersApiMock.create.mockReturnValue(
      throwError(() => ({
        status: 400,
        error: {
          status: 400,
          message: 'Vous avez une pénalité active et ne pouvez pas créer de atelier.'
        }
      }))
    );
    const fixture = TestBed.createComponent(MemberCreateAtelierPage);
    const component = fixture.componentInstance;

    component.form.patchValue({
      date: component.todayDate(),
      heureDebut: '09:00',
      typeAtelier: 'PUBLIC'
    });

    component.submit();

    expect(component.errorMessage()).toContain('Vous avez une pénalité active');
    expect(component.errorMessage()).not.toContain('code 400');
    expect(component.errorMessage()).not.toContain('Erreur :');
  });
});
