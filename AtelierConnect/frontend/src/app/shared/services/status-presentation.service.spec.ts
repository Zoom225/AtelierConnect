import { TestBed } from '@angular/core/testing';
import { afterEach, describe, expect, it } from 'vitest';
import { LanguageService } from '../../core/i18n/language.service';
import { StatusPresentationService } from './status-presentation.service';

describe('StatusPresentationService', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('traduit les statuts avec la langue courante, même après un changement', () => {
    const language = TestBed.inject(LanguageService);
    const status = TestBed.inject(StatusPresentationService);
    language.setLocale('fr');
    const french = status.paymentLabel('PAYE');
    expect(french).toBe(language.t('common.paymentPaid'));
    language.setLocale('en');
    expect(status.paymentLabel('PAYE')).toBe(language.t('common.paymentPaid'));
    expect(status.paymentLabel('PAYE')).not.toBe(french);
    expect(status.reservationLabel('CONFIRMEE')).toBe(language.t('common.reservationConfirmed'));
    expect(status.atelierLabel('COMPLET')).toBe(language.t('common.atelierFull'));
  });

  it('affiche un libellé neutre lorsque le paiement est absent', () => {
    const language = TestBed.inject(LanguageService);
    const status = TestBed.inject(StatusPresentationService);
    expect(status.paymentLabel(undefined)).toBe(language.t('common.none'));
    expect(status.paymentBadge(undefined)).toBe('ds-badge-neutral');
  });

  it('conserve les couleurs distinctes des paiements annulés dans les deux espaces', () => {
    const status = TestBed.inject(StatusPresentationService);
    expect(status.paymentBadge('ANNULE')).toBe('ds-badge-neutral');
    expect(status.paymentBadge('ANNULE', 'ds-badge-danger')).toBe('ds-badge-danger');
  });
});
