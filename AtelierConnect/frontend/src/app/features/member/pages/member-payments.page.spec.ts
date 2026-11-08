import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { PaiementsApiService } from '../../../core/api/paiements-api.service';
import { MemberSessionService } from '../../../core/auth/member-session.service';
import { PaiementResponse } from '../../../shared/models/reservation.model';
import { MemberPaymentsPage } from './member-payments.page';

describe('MemberPaymentsPage', () => {
  const pendingPayment: PaiementResponse = {
    id: 10,
    reservationId: 22,
    montant: 15,
    statut: 'EN_ATTENTE',
    datePaiement: null
  };

  const paidPayment: PaiementResponse = {
    id: 10,
    reservationId: 22,
    montant: 15,
    statut: 'PAYE',
    datePaiement: '2026-08-04T19:00:00'
  };

  let paiementsApiMock: {
    getByParticipant: ReturnType<typeof vi.fn>;
    pay: ReturnType<typeof vi.fn>;
    getDebt: ReturnType<typeof vi.fn>;
    payDebt: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    paiementsApiMock = {
      getByParticipant: vi.fn().mockReturnValue(of([pendingPayment])),
      pay: vi.fn().mockReturnValue(of(paidPayment)),
      getDebt: vi.fn().mockReturnValue(of({ solde: 0, suspensionActive: false, mouvements: [] })),
      payDebt: vi.fn().mockReturnValue(of({ solde: 0, suspensionActive: true, mouvements: [] }))
    };

    await TestBed.configureTestingModule({
      imports: [MemberPaymentsPage],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: PaiementsApiService, useValue: paiementsApiMock },
        { provide: MemberSessionService, useValue: { memberId: vi.fn().mockReturnValue(1) } }
      ]
    }).compileComponents();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    TestBed.resetTestingModule();
  });

  it('permet de payer un paiement en attente et affiche une confirmation', () => {
    const fixture = TestBed.createComponent(MemberPaymentsPage);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.pay(pendingPayment);

    expect(paiementsApiMock.pay).toHaveBeenCalledWith(22);
    expect(component.message()).toContain('Paiement effectue avec succes');
    expect(component.payments()[0].statut).toBe('PAYE');
  });

  it('règle une dette sans réservation et conserve la suspension', () => {
    paiementsApiMock.getByParticipant.mockReturnValue(of([]));
    paiementsApiMock.getDebt.mockReturnValue(of({ solde: 90, suspensionActive: true, mouvements: [] }));
    const fixture = TestBed.createComponent(MemberPaymentsPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="pay-debt"]').textContent).toContain('90,00');
    fixture.componentInstance.payDebt();
    expect(paiementsApiMock.payDebt).toHaveBeenCalledWith(90, expect.any(String));
    expect(fixture.componentInstance.debt()?.solde).toBe(0);
    expect(fixture.componentInstance.debt()?.suspensionActive).toBe(true);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="pay-debt"]')).toBeNull();
  });

  it('empêche le double clic et réutilise la même clé après une réponse perdue', () => {
    paiementsApiMock.getDebt.mockReturnValue(of({ solde: 90, suspensionActive: false, mouvements: [] }));
    const pending = new Subject<any>();
    paiementsApiMock.payDebt.mockReturnValue(pending);
    const fixture = TestBed.createComponent(MemberPaymentsPage);
    fixture.componentInstance.payDebt();
    fixture.componentInstance.payDebt();
    expect(paiementsApiMock.payDebt).toHaveBeenCalledOnce();
    const key = paiementsApiMock.payDebt.mock.calls[0][1];
    pending.error({ status: 0 });
    paiementsApiMock.payDebt.mockReturnValue(of({ solde: 0, suspensionActive: false, mouvements: [] }));
    fixture.componentInstance.payDebt();
    expect(paiementsApiMock.payDebt).toHaveBeenLastCalledWith(90, key);
  });

  it('explique un solde modifié et ne présente pas le règlement comme réussi', () => {
    paiementsApiMock.getDebt.mockReturnValueOnce(of({ solde: 60, suspensionActive: false, mouvements: [] }))
      .mockReturnValue(of({ solde: 90, suspensionActive: false, mouvements: [] }));
    paiementsApiMock.payDebt.mockReturnValue(throwError(() => ({ status: 400, error: { message: 'Votre solde a changé.' } })));
    const fixture = TestBed.createComponent(MemberPaymentsPage);
    fixture.componentInstance.payDebt();
    expect(fixture.componentInstance.errorMessage()).toContain('Votre solde a changé');
    expect(fixture.componentInstance.message()).toBe('');
    expect(fixture.componentInstance.debt()?.solde).toBe(90);
  });
});
