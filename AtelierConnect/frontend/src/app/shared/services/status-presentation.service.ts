import { Injectable, inject } from '@angular/core';
import { LanguageService } from '../../core/i18n/language.service';
import { TranslationKey } from '../../core/i18n/translations';
import { StatutAtelier, StatutReservation, TypeAtelier, TypeParticipant } from '../models/enums.model';

const PAYMENT_LABELS: Record<string, TranslationKey> = {
  PAYE: 'common.paymentPaid',
  EN_ATTENTE: 'common.paymentPending',
  REMBOURSE: 'common.paymentRefunded',
  ANNULE: 'common.paymentCanceled'
};

const RESERVATION_LABELS: Record<StatutReservation, TranslationKey> = {
  CONFIRMEE: 'common.reservationConfirmed',
  EN_ATTENTE: 'common.reservationPending',
  ANNULEE: 'common.reservationCanceled'
};

const ATELIER_LABELS: Record<StatutAtelier, TranslationKey> = {
  PLANIFIE: 'common.atelierPlanned',
  COMPLET: 'common.atelierFull',
  ANNULE: 'common.atelierCanceled'
};

/** Libellés communs traduits à l'affichage, pour suivre les changements de langue. */
@Injectable({ providedIn: 'root' })
export class StatusPresentationService {
  private readonly language = inject(LanguageService);

  paymentLabel(status: string | undefined): string {
    return this.language.t(PAYMENT_LABELS[status ?? ''] ?? 'common.none');
  }

  reservationLabel(status: StatutReservation): string {
    return this.language.t(RESERVATION_LABELS[status] ?? 'common.reservationCanceled');
  }

  atelierLabel(status: StatutAtelier): string {
    return this.language.t(ATELIER_LABELS[status] ?? 'common.atelierCanceled');
  }

  atelierTypeLabel(type: TypeAtelier): string {
    return this.language.t(type === 'PRIVE' ? 'member.create.private' : 'member.create.public');
  }

  reservationBadge(status: StatutReservation): string {
    if (status === 'CONFIRMEE') return 'ds-badge-success';
    return status === 'EN_ATTENTE' ? 'ds-badge-warning' : 'ds-badge-danger';
  }

  paymentBadge(status: string | undefined, canceledBadge = 'ds-badge-neutral'): string {
    switch (status) {
      case 'PAYE': return 'ds-badge-success';
      case 'EN_ATTENTE': return 'ds-badge-warning';
      case 'REMBOURSE': return 'ds-badge-info';
      case 'ANNULE': return canceledBadge;
      default: return 'ds-badge-neutral';
    }
  }

  participantBadge(type: TypeParticipant): string {
    if (type === 'PARTENAIRE') return 'ds-badge-info';
    return type === 'ABONNE' ? 'ds-badge-success' : 'ds-badge-warning';
  }

  catalogueAtelierBadge(status: StatutAtelier): string {
    if (status === 'PLANIFIE') return 'ds-badge-success';
    return status === 'COMPLET' ? 'ds-badge-warning' : 'ds-badge-danger';
  }

  catalogueAtelierTypeBadge(type: TypeAtelier): string {
    return type === 'PRIVE' ? 'ds-badge-info' : 'ds-badge-neutral';
  }
}
