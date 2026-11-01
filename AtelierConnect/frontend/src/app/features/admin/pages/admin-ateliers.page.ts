import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AteliersApiService } from '../../../core/api/ateliers-api.service';
import { ReservationsApiService } from '../../../core/api/reservations-api.service';
import { CentresApiService } from '../../../core/api/centres-api.service';
import { AdminSessionService } from '../../../core/auth/admin-session.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { AtelierResponse } from '../../../shared/models/atelier.model';
import { ATELIER_CAPACITY } from '../../../shared/models/atelier-rules';
import { ReservationResponse } from '../../../shared/models/reservation.model';
import { CentreResponse } from '../../../shared/models/centre-salle.model';
import { extractApiErrorMessage } from '../../../shared/utils/api-error.util';

@Component({
  selector: 'app-admin-ateliers-page',
  standalone: true,
  imports: [CommonModule, RouterLink, MatCardModule, MatButtonModule, MatProgressSpinnerModule],
  template: `
    <!-- Administration des ateliers -->
    <div class="adm-atelier-header">
      <div class="adm-atelier-header-inner">
        <div class="adm-atelier-title-block">
<span class="adm-atelier-icon">🧩</span>
          <div>
            <h1 class="adm-atelier-title">{{ language.t('admin.ateliers.title') }}</h1>
            <p class="adm-atelier-sub">{{ language.t('admin.ateliers.subtitle') }}</p>
          </div>
        </div>
        <a routerLink="/admin" class="adm-atelier-back-btn">{{ language.t('common.back') }}</a>
      </div>
    </div>

    <section class="page-shell">
      @if (loading()) {
        <mat-spinner diameter="32"></mat-spinner>
      }
      @if (message()) {
        <p class="status-success">✅ {{ message() }}</p>
      }
      @if (errorMessage()) {
        <p class="status-error">❌ {{ errorMessage() }}</p>
      }

      <div class="grid gap-5 lg:grid-cols-2">
        @for (atelier of filteredAteliers(); track atelier.id) {
          <mat-card class="adm-atelier-card">
            <mat-card-header class="adm-atelier-card-header">
              <div class="adm-atelier-card-title-row">
                <div>
                  <mat-card-title class="adm-atelier-card-title">{{ atelier.salleNom }}</mat-card-title>
                  <mat-card-subtitle class="adm-atelier-card-sub">🏟️ {{ atelier.centreNom }}</mat-card-subtitle>
                </div>
                <span class="ds-badge" [class]="typeBadgeClass(atelier.typeAtelier)">{{ typeLabel(atelier.typeAtelier) }}</span>
              </div>
              <div class="adm-atelier-date-row">
                <span>📅 {{ atelier.date }}</span>
                <span>🕐 {{ atelier.heureDebut }} – {{ atelier.heureFin }}</span>
              </div>
            </mat-card-header>

            <mat-card-content class="adm-atelier-content">
              <div class="adm-atelier-info-grid">
                <div class="adm-info-item">
<span class="adm-info-label">{{ language.t('admin.ateliers.organizer') }}</span>
                  <span class="adm-info-value">{{ atelier.organisateurNom }}</span>
                </div>
                <div class="adm-info-item">
<span class="adm-info-label">{{ language.t('admin.ateliers.status') }}</span>
                  <span class="ds-badge" [class]="statusBadgeClass(atelier.statut)">{{ atelierStatusLabel(atelier.statut) }}</span>
                </div>
                <div class="adm-info-item adm-players-item">
<span class="adm-info-label">{{ language.t('admin.ateliers.players') }}</span>
                  <div class="adm-players-bar">
                    <div class="adm-players-fill" [style.width.%]="(atelier.nbParticipantsActuels / ATELIER_CAPACITY) * 100">
                      <span>{{ atelier.nbParticipantsActuels }}/{{ ATELIER_CAPACITY }}</span>
                    </div>
                  </div>
                </div>
              </div>

              @if (selectedAtelierId() === atelier.id) {
                <div class="adm-reservations-panel">
<p class="adm-reservations-title">{{ language.t('admin.ateliers.reservations') }}</p>
                  @for (reservation of selectedReservations(); track reservation.id) {
                    <div class="adm-reservation-item">
                      <span class="font-medium">{{ reservation.participantNom }}</span>
                      <div class="flex gap-2">
                        <span class="ds-badge" [class]="reservationBadgeClass(reservation.statut)">{{ reservationStatusLabel(reservation.statut) }}</span>
                        <span class="ds-badge" [class]="paymentBadgeClass(reservation.paiement?.statut)">{{ paymentStatusLabel(reservation.paiement?.statut) }}</span>
                      </div>
                    </div>
                  } @empty {
<p class="text-sm text-slate-500 text-center py-2">{{ language.t('admin.ateliers.noReservations') }}</p>
                  }
                </div>
              }
            </mat-card-content>

            <mat-card-actions class="adm-atelier-actions">
              <button class="adm-atelier-btn-details" type="button" (click)="showReservations(atelier)">
{{ selectedAtelierId() === atelier.id ? language.t('admin.ateliers.hideDetails') : language.t('admin.ateliers.showDetails') }}
              </button>
              <button
                class="adm-atelier-btn-convert"
                type="button"
                (click)="convertToPublic(atelier.id)"
                [disabled]="atelier.typeAtelier !== 'PRIVE'"
              >
{{ language.t('admin.ateliers.convert') }}
              </button>
            </mat-card-actions>
          </mat-card>
        } @empty {
          <div class="adm-atelier-empty lg:col-span-2">
            <span>🧩</span>
<p>{{ language.t('admin.ateliers.empty') }}</p>
          </div>
        }
      </div>
    </section>

    <style>
      .adm-atelier-header {
        background: linear-gradient(135deg, #7c2d12 0%, #c2410c 50%, #ea580c 100%);
        padding: 1.5rem 2rem;
        box-shadow: 0 4px 16px rgba(194,65,12,0.3);
      }
      .adm-atelier-header-inner {
        display: flex; align-items: center; justify-content: space-between;
        max-width: 1200px; margin: 0 auto; gap: 1rem;
      }
      .adm-atelier-title-block { display: flex; align-items: center; gap: 1rem; }
      .adm-atelier-icon { font-size: 2.5rem; }
      .adm-atelier-title { font-size: 1.6rem; font-weight: 800; color: #fff; margin: 0; }
      .adm-atelier-sub { color: rgba(255,255,255,0.75); font-size: 0.85rem; margin: 0; }
      .adm-atelier-back-btn {
        background: rgba(255,255,255,0.15); color: #fff; border: 2px solid rgba(255,255,255,0.35);
        border-radius: 9999px; padding: 0.45rem 1.1rem; font-weight: 700;
        font-size: 0.85rem; text-decoration: none; transition: background 0.15s;
        white-space: nowrap;
      }
      .adm-atelier-back-btn:hover { background: rgba(255,255,255,0.25); }

      .adm-atelier-card {
        border-radius: 1.25rem !important;
        border: 1px solid rgba(234,88,12,0.15) !important;
        overflow: hidden;
        border-top: 4px solid #ea580c !important;
        transition: transform 0.15s, box-shadow 0.15s;
      }
      .adm-atelier-card:hover { transform: translateY(-3px); }

      .adm-atelier-card-header { padding: 1rem 1.25rem 0 !important; }
      .adm-atelier-card-title-row { display: flex; align-items: flex-start; justify-content: space-between; gap: 0.75rem; margin-bottom: 0.5rem; }
      .adm-atelier-card-title { font-size: 1.05rem !important; font-weight: 700 !important; color: #1e293b !important; }
      .adm-atelier-card-sub { color: #64748b !important; }
      .adm-atelier-date-row { display: flex; gap: 1rem; font-size: 0.82rem; color: #94a3b8; padding-bottom: 0.5rem; flex-wrap: wrap; }

      .adm-atelier-content { padding: 0.75rem 1.25rem !important; }
      .adm-atelier-info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
      .adm-info-item { display: flex; flex-direction: column; gap: 0.2rem; }
      .adm-players-item { grid-column: 1 / -1; }
      .adm-info-label { font-size: 0.72rem; font-weight: 600; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.04em; }
      .adm-info-value { font-size: 0.9rem; font-weight: 600; color: #1e293b; }

      .adm-players-bar {
        background: #f1f5f9; border-radius: 9999px; height: 1.25rem;
        overflow: hidden; border: 1px solid #e2e8f0; margin-top: 0.2rem;
      }
      .adm-players-fill {
        height: 100%; background: linear-gradient(90deg, #f97316, #ea580c);
        border-radius: 9999px; display: flex; align-items: center;
        justify-content: center; min-width: 2rem;
        font-size: 0.7rem; font-weight: 700; color: #fff;
        transition: width 0.4s ease;
      }

      .adm-reservations-panel {
        margin-top: 0.75rem; border-radius: 0.75rem;
        border: 1px solid #fed7aa; background: #fff7ed; padding: 0.75rem;
      }
      .adm-reservations-title { font-weight: 700; color: #9a3412; font-size: 0.9rem; margin: 0 0 0.5rem; }
      .adm-reservation-item {
        display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 0.5rem;
        padding: 0.5rem 0.75rem; border-radius: 0.5rem; background: #fff;
        margin-bottom: 0.4rem; font-size: 0.85rem; border: 1px solid #fed7aa;
      }

      .adm-atelier-actions {
        display: flex; flex-wrap: wrap; gap: 0.75rem;
        padding: 0.75rem 1.25rem 1rem !important;
        border-top: 1px solid #fef0e6;
      }
      .adm-atelier-btn-details {
        flex: 1; min-width: 130px; padding: 0.5rem 1rem; border-radius: 0.6rem;
        background: #fff7ed; color: #9a3412; border: 1.5px solid #fed7aa;
        font-weight: 700; font-size: 0.85rem; cursor: pointer; transition: background 0.15s;
      }
      .adm-atelier-btn-details:hover { background: #ffedd5; }
      .adm-atelier-btn-convert {
        flex: 1; min-width: 160px; padding: 0.5rem 1rem; border-radius: 0.6rem;
        background: linear-gradient(135deg, #0369a1, #2563eb); color: #fff; border: none;
        font-weight: 700; font-size: 0.85rem; cursor: pointer;
        box-shadow: 0 3px 10px rgba(37,99,235,0.3); transition: transform 0.15s;
      }
      .adm-atelier-btn-convert:hover:not(:disabled) { transform: translateY(-1px); }
      .adm-atelier-btn-convert:disabled { opacity: 0.4; cursor: not-allowed; }

      .adm-atelier-empty {
        text-align: center; padding: 3rem; color: #94a3b8;
        font-size: 1.1rem; display: flex; flex-direction: column; align-items: center; gap: 0.5rem;
      }
      .adm-atelier-empty span { font-size: 3rem; }
    </style>
  `
})
export class AdminAteliersPage {
  readonly ATELIER_CAPACITY = ATELIER_CAPACITY;
  private readonly ateliersApi = inject(AteliersApiService);
  private readonly reservationsApi = inject(ReservationsApiService);
  private readonly centresApi = inject(CentresApiService);
  private readonly adminSession = inject(AdminSessionService);
  readonly language = inject(LanguageService);

  readonly loading = signal(false);
  readonly message = signal('');
  readonly errorMessage = signal('');
  readonly ateliers = signal<AtelierResponse[]>([]);
  readonly centres = signal<CentreResponse[]>([]);
  readonly selectedAtelierId = signal<number | null>(null);
  readonly selectedReservations = signal<ReservationResponse[]>([]);

  readonly filteredAteliers = computed(() => {
    if (this.adminSession.isGlobalAdmin()) {
      return this.ateliers();
    }
    const centreNames = new Set(this.centres().map((site) => site.nom));
    return this.ateliers().filter((atelier) => centreNames.has(atelier.centreNom));
  });

  constructor() {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    forkJoin({
      ateliers: this.ateliersApi.getAll(),
      centres: this.centresApi.getAll()
    }).subscribe({
      next: ({ ateliers, centres }) => {
        this.ateliers.set(ateliers);
        this.centres.set(this.adminSession.isGlobalAdmin() ? centres : centres.filter((site) => site.id === this.adminSession.centreId()));
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.errorMessage.set(extractApiErrorMessage(error, this.language.t('admin.ateliers.loadError')));
      }
    });
  }

  showReservations(atelier: AtelierResponse): void {
    if (this.selectedAtelierId() === atelier.id) {
      this.selectedAtelierId.set(null);
      this.selectedReservations.set([]);
      return;
    }

    this.selectedAtelierId.set(atelier.id);
    this.reservationsApi.getByAtelier(atelier.id).subscribe({
      next: (reservations) => this.selectedReservations.set(reservations),
      error: (error) => {
        this.errorMessage.set(extractApiErrorMessage(error, this.language.t('admin.ateliers.reservationsError')));
      }
    });
  }

  convertToPublic(atelierId: number): void {
    this.message.set('');
    this.errorMessage.set('');

    this.ateliersApi.convertToPublic(atelierId).subscribe({
      next: () => {
        this.message.set(this.language.t('admin.ateliers.convertSuccess'));
        this.loadData();
      },
      error: (error) => {
        this.errorMessage.set(extractApiErrorMessage(error, this.language.t('admin.ateliers.convertError')));
      }
    });
  }

  typeBadgeClass(type: AtelierResponse['typeAtelier']): string {
    return type === 'PRIVE' ? 'ds-badge-info' : 'ds-badge-neutral';
  }

  typeLabel(type: AtelierResponse['typeAtelier']): string {
    return type === 'PRIVE' ? this.language.t('member.create.private') : this.language.t('member.create.public');
  }

  statusBadgeClass(statut: AtelierResponse['statut']): string {
    if (statut === 'PLANIFIE') {
      return 'ds-badge-success';
    }
    if (statut === 'COMPLET') {
      return 'ds-badge-warning';
    }
    return 'ds-badge-danger';
  }

  atelierStatusLabel(statut: AtelierResponse['statut']): string {
    if (statut === 'PLANIFIE') {
      return this.language.t('common.atelierPlanned');
    }
    if (statut === 'COMPLET') {
      return this.language.t('common.atelierFull');
    }
    return this.language.t('common.atelierCanceled');
  }

  reservationBadgeClass(statut: ReservationResponse['statut']): string {
    if (statut === 'CONFIRMEE') {
      return 'ds-badge-success';
    }
    if (statut === 'EN_ATTENTE') {
      return 'ds-badge-warning';
    }
    return 'ds-badge-danger';
  }

  reservationStatusLabel(statut: ReservationResponse['statut']): string {
    if (statut === 'CONFIRMEE') {
      return this.language.t('common.reservationConfirmed');
    }
    if (statut === 'EN_ATTENTE') {
      return this.language.t('common.reservationPending');
    }
    return this.language.t('common.reservationCanceled');
  }

  paymentBadgeClass(statut: string | undefined): string {
    if (statut === 'PAYE') {
      return 'ds-badge-success';
    }
    if (statut === 'EN_ATTENTE') {
      return 'ds-badge-warning';
    }
    if (statut === 'REMBOURSE') {
      return 'ds-badge-info';
    }
    return 'ds-badge-neutral';
  }

  paymentStatusLabel(statut: string | undefined): string {
    if (statut === 'PAYE') {
      return this.language.t('common.paymentPaid');
    }
    if (statut === 'EN_ATTENTE') {
      return this.language.t('common.paymentPending');
    }
    if (statut === 'REMBOURSE') {
      return this.language.t('common.paymentRefunded');
    }
    if (statut === 'ANNULE') {
      return this.language.t('common.paymentCanceled');
    }
    return this.language.t('common.none');
  }
}
