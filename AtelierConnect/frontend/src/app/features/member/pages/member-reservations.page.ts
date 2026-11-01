import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { AteliersApiService } from '../../../core/api/ateliers-api.service';
import { PaiementsApiService } from '../../../core/api/paiements-api.service';
import { ReservationsApiService } from '../../../core/api/reservations-api.service';
import { MemberSessionService } from '../../../core/auth/member-session.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { AtelierResponse } from '../../../shared/models/atelier.model';
import { ATELIER_CAPACITY } from '../../../shared/models/atelier-rules';
import { ReservationResponse } from '../../../shared/models/reservation.model';
import { extractApiErrorMessage } from '../../../shared/utils/api-error.util';

@Component({
  selector: 'app-member-reservations-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule
  ],
  template: `
    <section class="page-shell">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 class="title-gradient ds-section-title">{{ language.t('member.reservations.title') }}</h1>
          <p class="ds-subtitle">{{ language.t('member.reservations.subtitle') }}</p>
        </div>
        <div class="toolbar-actions">
          <a mat-flat-button color="primary" routerLink="/member/ateliers/new" [queryParams]="{ type: 'PUBLIC' }">{{ language.t('member.reservations.createPublic') }}</a>
          <a mat-flat-button color="accent" routerLink="/member/ateliers/new" [queryParams]="{ type: 'PRIVE' }">{{ language.t('member.reservations.createPrivate') }}</a>
          <a mat-stroked-button routerLink="/member/ateliers">{{ language.t('member.reservations.ateliers') }}</a>
          <a mat-stroked-button routerLink="/member/payments">{{ language.t('member.payments.title') }}</a>
        </div>
      </div>

      @if (loading()) {
        <mat-spinner diameter="32"></mat-spinner>
      }

      @if (message()) {
        <p class="status-success">{{ message() }}</p>
      }

      @if (errorMessage()) {
        <p class="status-error">{{ errorMessage() }}</p>
      }

      <mat-card class="card-soft panel-gradient">
        <mat-card-header>
          <mat-card-title>{{ language.t('member.reservations.organizedTitle') }}</mat-card-title>
          <mat-card-subtitle>{{ language.t('member.reservations.organizedSub') }}</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content class="grid gap-4 md:grid-cols-3">
          @if (managedAteliers().length) {
            <div class="md:col-span-3 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
              @for (atelier of managedAteliers(); track atelier.id) {
                <button
                  type="button"
                  class="text-left rounded-lg border p-4 transition"
                  [class.border-teal-400]="managedAtelierId() === atelier.id"
                  [class.bg-teal-50]="managedAtelierId() === atelier.id"
                  [class.border-slate-200]="managedAtelierId() !== atelier.id"
                  [class.bg-white]="managedAtelierId() !== atelier.id"
                  (click)="onManagedAtelierChange(atelier.id)"
                >
                  <div class="flex items-start justify-between gap-3">
                    <div>
                      <p class="font-semibold text-slate-900">#{{ atelier.id }} - {{ atelier.salleNom }}</p>
                      <p class="mt-1 text-sm text-slate-600">{{ atelier.centreNom }}</p>
                    </div>
                    <div class="flex flex-col items-end gap-1">
                      <span class="ds-badge" [class]="typeBadgeClass(atelier.typeAtelier)">{{ atelierTypeLabel(atelier.typeAtelier) }}</span>
                      <span class="ds-badge" [class]="atelierStatusBadgeClass(atelier.statut)">{{ atelierStatusLabel(atelier.statut) }}</span>
                    </div>
                  </div>
                  <div class="mt-3 text-sm text-slate-700">
                    {{ atelier.date }} - {{ atelier.heureDebut.slice(0, 5) }} / {{ atelier.heureFin.slice(0, 5) }}
                  </div>
                  <div class="mt-1 text-xs text-slate-500">
                    {{ atelier.nbParticipantsActuels }}/{{ ATELIER_CAPACITY }} participants
                  </div>
                </button>
              }
            </div>
          }

          <mat-form-field appearance="outline">
            <mat-label>{{ language.t('member.reservations.select') }}</mat-label>
            <mat-select [value]="managedAtelierId()" (valueChange)="onManagedAtelierChange($event)">
              <mat-option [value]="null">{{ language.t('member.reservations.chooseAtelier') }}</mat-option>
              @for (atelier of managedAteliers(); track atelier.id) {
                <mat-option [value]="atelier.id">
                  #{{ atelier.id }} – {{ atelier.date }} {{ atelier.heureDebut }} [{{ atelierTypeLabel(atelier.typeAtelier) }}]
                </mat-option>
              }
            </mat-select>
          </mat-form-field>

          @if (selectedManagedAtelier()?.typeAtelier === 'PRIVE') {
            <mat-form-field appearance="outline">
              <mat-label>{{ language.t('member.reservations.inviteMatricule') }}</mat-label>
              <input matInput [formControl]="inviteMatricule" [placeholder]="language.t('member.reservations.inviteMatricule')" />
            </mat-form-field>

            <div class="flex items-end gap-2">
              <button
                mat-flat-button
                color="primary"
                type="button"
                (click)="addPlayer()"
                [disabled]="!selectedManagedAtelier() || inviteMatricule.invalid || actionId() !== null"
              >
                {{ language.t('member.reservations.addPlayer') }}
              </button>
            </div>
          }

          @if (!managedAteliers().length) {
              <div class="md:col-span-3 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">
              ✅ {{ language.t('member.reservations.noOrganized') }}
            </div>
          }

          @if (selectedManagedAtelier()) {
            <div class="md:col-span-3 rounded-lg border border-slate-200 bg-white p-4">
                <p class="mb-3 text-sm font-semibold text-slate-800">
                ✏️ {{ language.t('member.reservations.edit') }} #{{ selectedManagedAtelier()!.id }}
                <span class="ml-2 ds-badge" [class]="typeBadgeClass(selectedManagedAtelier()!.typeAtelier)">
                  {{ atelierTypeLabel(selectedManagedAtelier()!.typeAtelier) }}
                </span>
              </p>
              <form [formGroup]="managedAtelierForm" class="grid gap-3 md:grid-cols-4" (ngSubmit)="updateManagedAtelier()">
                <mat-form-field appearance="outline">
                  <mat-label>{{ language.t('member.public.date') }}</mat-label>
                  <input matInput type="date" formControlName="date" />
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>{{ language.t('member.public.time') }}</mat-label>
                  <input matInput type="time" formControlName="heureDebut" />
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>{{ language.t('member.public.typeLabel') }}</mat-label>
                  <mat-select formControlName="typeAtelier">
                    <mat-option value="PRIVE">{{ language.t('member.create.private') }}</mat-option>
                    <mat-option value="PUBLIC">{{ language.t('member.create.public') }}</mat-option>
                  </mat-select>
                </mat-form-field>

                <div class="flex items-end gap-2">
                    <button mat-flat-button color="primary" type="submit" [disabled]="managedAtelierForm.invalid || actionId() !== null">
                    💾 {{ language.t('common.save') }}
                  </button>
                  <button mat-stroked-button color="warn" type="button" (click)="requestDeleteManagedAtelier()" [disabled]="actionId() !== null">
                    🗑️ {{ language.t('common.delete') }}
                  </button>
                </div>
              </form>

              <div class="mt-3 rounded-lg border border-sky-200 bg-sky-50 p-3">
                <p class="mb-2 text-sm text-sky-900">
                  {{ language.t('member.reservations.consentInfo') }}
                </p>
                <button mat-stroked-button type="button"
                        (click)="consentToPublicOpening()"
                        [disabled]="actionId() !== null || selectedManagedAtelier()!.accordOuverturePublique === true">
                  {{ selectedManagedAtelier()!.accordOuverturePublique === true
                    ? language.t('member.reservations.consentSuccess')
                    : language.t('member.reservations.consentAction') }}
                </button>
              </div>

              @if (pendingDeleteAtelierId() === selectedManagedAtelier()!.id) {
                <div class="mt-3 rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-900">
                  <p class="font-semibold">{{ language.t('member.reservations.confirmation') }}</p>
                  <p class="mt-1">{{ language.t('member.reservations.emptyBody') }}</p>
                  <div class="mt-3 flex gap-2">
                    <button mat-flat-button color="warn" type="button" (click)="deleteManagedAtelier()" [disabled]="actionId() !== null">{{ language.t('common.confirm') }}</button>
                    <button mat-stroked-button type="button" (click)="cancelDeleteManagedAtelier()" [disabled]="actionId() !== null">{{ language.t('common.cancel') }}</button>
                  </div>
                </div>
              }
            </div>
          }

          <div class="md:col-span-3">
            @if (managedReservations().length) {
              <p class="mb-2 text-sm font-semibold text-slate-700">{{ language.t('member.reservations.players') }}</p>
              <div class="grid gap-2 md:grid-cols-2">
                @for (res of managedReservations(); track res.id) {
                  <div class="rounded-lg border border-slate-200 bg-slate-50 p-3 flex items-center justify-between">
                    <div>
                      <p class="font-medium text-slate-800">{{ res.participantNom }}</p>
                      <p class="mt-1 flex flex-wrap gap-1 text-xs">
                        <span class="ds-badge" [class]="reservationBadgeClass(res.statut)">{{ language.t('member.reservations.reservationLabel') }}: {{ reservationStatusLabel(res.statut) }}</span>
                        <span class="ds-badge" [class]="paymentBadgeClass(res.paiement?.statut)">{{ language.t('member.reservations.paymentLabel') }}: {{ paymentStatusLabel(res.paiement?.statut) }}</span>
                      </p>
                    </div>
                    @if (selectedManagedAtelier()?.typeAtelier === 'PRIVE') {
                      <button
                        mat-stroked-button
                        color="warn"
                        type="button"
                        (click)="removePlayer(res)"
                        [disabled]="actionId() === res.id || res.statut === 'ANNULEE' || res.participantId === memberId()"
                      >
                        {{ language.t('member.reservations.removePlayer') }}
                      </button>
                    }
                  </div>
                }
              </div>
            } @else if (selectedManagedAtelier()) {
                <p class="text-sm text-slate-500 italic">{{ language.t('member.reservations.noPlayer') }}</p>
            }
          </div>
        </mat-card-content>
      </mat-card>

      <div class="grid gap-4 lg:grid-cols-2">
        @for (reservation of reservations(); track reservation.id) {
          <mat-card class="card-soft">
            <mat-card-header>
              <mat-card-title>📋 {{ language.t('member.reservations.reservationTitle') }} #{{ reservation.id }}</mat-card-title>
              <mat-card-subtitle>{{ reservation.atelierDateTime }}</mat-card-subtitle>
            </mat-card-header>
            <mat-card-content class="ds-data-list">
              <div class="ds-data-row"><span class="ds-data-key">{{ language.t('member.reservations.player') }}</span><span class="ds-data-value">{{ reservation.participantNom }}</span></div>
              <div class="ds-data-row"><span class="ds-data-key">{{ language.t('member.reservations.atelier') }}</span><span class="ds-data-value">#{{ reservation.atelierId }}</span></div>
              <div class="ds-data-row"><span class="ds-data-key">{{ language.t('member.reservations.reservation') }}</span><span class="ds-data-value"><span class="ds-badge" [class]="reservationBadgeClass(reservation.statut)">{{ reservationStatusLabel(reservation.statut) }}</span></span></div>
              <div class="ds-data-row"><span class="ds-data-key">{{ language.t('member.reservations.payment') }}</span><span class="ds-data-value"><span class="ds-badge" [class]="paymentBadgeClass(reservation.paiement?.statut)">{{ paymentStatusLabel(reservation.paiement?.statut) }}</span></span></div>
              <div class="ds-data-row"><span class="ds-data-key">{{ language.t('member.reservations.amount') }}</span><span class="ds-data-value">{{ reservation.paiement?.montant ?? 0 }} €</span></div>
            </mat-card-content>
            <mat-card-actions>
              <button
                mat-flat-button
                color="primary"
                type="button"
                (click)="pay(reservation)"
                [disabled]="reservation.statut === 'ANNULEE' || reservation.paiement?.statut !== 'EN_ATTENTE' || actionId() === reservation.id"
              >
                💳 {{ language.t('member.reservations.pay') }}
              </button>
              <button
                mat-stroked-button
                color="warn"
                type="button"
                (click)="cancel(reservation)"
                [disabled]="reservation.statut === 'ANNULEE' || actionId() === reservation.id"
              >
                ❌ {{ language.t('member.reservations.cancel') }}
              </button>
            </mat-card-actions>
          </mat-card>
        } @empty {
          @if (!loading()) {
            <mat-card>
              <mat-card-content class="py-6 text-slate-600">
                <p>{{ language.t('member.payments.none') }}</p>
              <p class="mt-2 text-sm text-slate-400">{{ language.t('member.reservations.emptyBody') }}</p>
              </mat-card-content>
            </mat-card>
          }
        }
      </div>
    </section>
  `
})
export class MemberReservationsPage {
  readonly ATELIER_CAPACITY = ATELIER_CAPACITY;
  private readonly ateliersApi = inject(AteliersApiService);
  private readonly reservationsApi = inject(ReservationsApiService);
  private readonly paiementsApi = inject(PaiementsApiService);
  private readonly memberSession = inject(MemberSessionService);
  private readonly router = inject(Router);
  readonly language = inject(LanguageService);
  private static readonly HIDDEN_GENERIC_ERROR = 'Une erreur inattendue est survenue';
  private pendingCreatedAtelierId: number | null = null;
  private pendingSuccessMessage = '';

  readonly loading = signal(false);
  readonly actionId = signal<number | null>(null);
  readonly message = signal('');
  readonly errorMessage = signal('');
  readonly reservations = signal<ReservationResponse[]>([]);
  readonly organisedAteliers = signal<AtelierResponse[]>([]);
  // Ne retourne que les ateliers PRIVE pour la gestion des participants
  readonly managedAteliers = computed(() => this.organisedAteliers().filter((m) => m.typeAtelier === 'PRIVE'));
  readonly selectedManagedAtelier = computed(() => {
    const atelierId = this.managedAtelierId();
    if (!atelierId) {
      return null;
    }
    return this.managedAteliers().find((atelier) => atelier.id === atelierId) ?? null;
  });
  readonly managedReservations = signal<ReservationResponse[]>([]);
  readonly managedAtelierId = signal<number | null>(null);
  readonly pendingDeleteAtelierId = signal<number | null>(null);
  readonly managedAtelierForm = new FormGroup({
    date: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    heureDebut: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    typeAtelier: new FormControl<'PUBLIC' | 'PRIVE'>('PRIVE', { nonNullable: true, validators: [Validators.required] })
  });
  readonly inviteMatricule = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.pattern(/^(G\d{4}|S\d{5}|L\d{5})$/)]
  });
  readonly memberId = computed(() => this.memberSession.memberId());

  constructor() {
    const navigationState = (this.router.getCurrentNavigation()?.extras.state ?? window.history.state ?? {}) as {
      createdAtelierId?: number;
      successMessage?: string;
    };
    this.pendingCreatedAtelierId = typeof navigationState.createdAtelierId === 'number'
      ? navigationState.createdAtelierId
      : null;
    this.pendingSuccessMessage = typeof navigationState.successMessage === 'string'
      ? navigationState.successMessage.trim()
      : '';

    if (typeof navigationState.successMessage === 'string' && navigationState.successMessage.trim()) {
      this.message.set(navigationState.successMessage);
    }
    this.loadReservations();
    this.loadOrganisedAteliers();
  }

  private setPageError(message: string): void {
    this.errorMessage.set(
      message.trim() === MemberReservationsPage.HIDDEN_GENERIC_ERROR ? '' : message
    );
  }

  loadReservations(): void {
      const memberId = this.memberId();
      if (!memberId) {
      this.errorMessage.set(this.language.t('member.reservations.noMember'));
      return;
    }

    this.loading.set(true);
    if (!this.pendingSuccessMessage) {
      this.message.set('');
    }
    this.errorMessage.set('');
    this.reservationsApi.getByParticipant(memberId).subscribe({
      next: (reservations) => {
        this.reservations.set(reservations);
        if (this.pendingSuccessMessage) {
          this.message.set(this.pendingSuccessMessage);
          this.pendingSuccessMessage = '';
        }
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.setPageError(extractApiErrorMessage(error, this.language.t('member.reservations.loadError')));
      }
    });
  }

  loadOrganisedAteliers(): void {
    const memberId = this.memberId();
    if (!memberId) {
      return;
    }

    this.ateliersApi.getByOrganisateur(memberId).subscribe({
      next: (ateliers) => {
        this.organisedAteliers.set(ateliers.filter((atelier) => atelier.statut !== 'ANNULE'));
        this.selectCreatedAtelierIfNeeded();
        if (this.managedAtelierId() && !this.selectedManagedAtelier()) {
          this.managedAtelierId.set(null);
          this.managedReservations.set([]);
          this.managedAtelierForm.reset({ date: '', heureDebut: '', typeAtelier: 'PRIVE' });
        } else {
          this.syncManagedAtelierForm();
        }
      },
      error: () => {
        // On garde la page utilisable même si ce chargement échoue.
      }
    });
  }

  onManagedAtelierChange(atelierId: number | null): void {
    // On bloque la sélection d'un atelier PUBLIC
    const atelier = this.organisedAteliers().find((m) => m.id === atelierId) ?? null;
    if (atelier && atelier.typeAtelier !== 'PRIVE') {
      this.errorMessage.set(this.language.t('member.reservations.privateOnly'));
      this.managedAtelierId.set(null);
      this.managedReservations.set([]);
      this.pendingDeleteAtelierId.set(null);
      return;
    }

    this.managedAtelierId.set(atelierId);
    this.managedReservations.set([]);
    this.pendingDeleteAtelierId.set(null);

    if (!atelierId) {
      this.managedAtelierForm.reset({ date: '', heureDebut: '', typeAtelier: 'PRIVE' });
      return;
    }

    this.syncManagedAtelierForm();

    this.reservationsApi.getByAtelier(atelierId).subscribe({
      next: (reservations) => this.managedReservations.set(reservations),
      error: (error) => {
        this.setPageError(extractApiErrorMessage(error, this.language.t('member.reservations.playersLoadError')));
      }
    });
  }

  private selectCreatedAtelierIfNeeded(): void {
    if (!this.pendingCreatedAtelierId) {
      return;
    }

    const createdAtelier = this.managedAteliers().find((atelier) => atelier.id === this.pendingCreatedAtelierId);
    this.pendingCreatedAtelierId = null;
    if (createdAtelier) {
      this.onManagedAtelierChange(createdAtelier.id);
    }
  }

  updateManagedAtelier(): void {
    const selectedAtelier = this.selectedManagedAtelier();
    const requesterId = this.memberId();
    if (!selectedAtelier || !requesterId || this.managedAtelierForm.invalid || this.actionId() !== null) {
      return;
    }

    this.actionId.set(selectedAtelier.id);
    this.message.set('');
    this.errorMessage.set('');

    this.ateliersApi
      .update(selectedAtelier.id, {
        salleId: selectedAtelier.salleId,
        organisateurId: requesterId,
        date: this.managedAtelierForm.controls.date.getRawValue(),
        heureDebut: this.managedAtelierForm.controls.heureDebut.getRawValue(),
        typeAtelier: this.managedAtelierForm.controls.typeAtelier.getRawValue()
      })
      .subscribe({
        next: (updatedAtelier) => {
          this.actionId.set(null);
          this.message.set(this.language.t('member.reservations.updateSuccess'));
          this.organisedAteliers.update((ateliers) => ateliers.map((atelier) => (atelier.id === updatedAtelier.id ? updatedAtelier : atelier)));

          if (updatedAtelier.typeAtelier !== 'PRIVE') {
            this.managedAtelierId.set(null);
            this.managedReservations.set([]);
            this.managedAtelierForm.reset({ date: '', heureDebut: '', typeAtelier: 'PRIVE' });
          } else {
            this.syncManagedAtelierForm();
            this.onManagedAtelierChange(updatedAtelier.id);
          }
        },
        error: (error) => {
          this.actionId.set(null);
          this.setPageError(extractApiErrorMessage(error, this.language.t('member.reservations.updateError')));
        }
      });
  }

  consentToPublicOpening(): void {
    const atelier = this.selectedManagedAtelier();
    if (!atelier || atelier.typeAtelier !== 'PRIVE' || atelier.accordOuverturePublique === true || this.actionId() !== null) {
      return;
    }
    if (!confirm(this.language.t('member.reservations.consentConfirm'))) {
      return;
    }

    this.actionId.set(atelier.id);
    this.message.set('');
    this.errorMessage.set('');
    this.ateliersApi.consentToPublicOpening(atelier.id).subscribe({
      next: () => {
        this.actionId.set(null);
        // Mettre immédiatement à jour la réponse locale ; le rechargement
        // récupérera ensuite l'accord enregistré en base via le DTO.
        this.organisedAteliers.update(ateliers => ateliers.map(atelier =>
          atelier.id === atelier.id ? { ...atelier, accordOuverturePublique: true } : atelier));
        this.message.set(this.language.t('member.reservations.consentSuccess'));
      },
      error: (error) => {
        this.actionId.set(null);
        this.setPageError(extractApiErrorMessage(error,
          this.language.t('member.reservations.consentError')));
      }
    });
  }

  requestDeleteManagedAtelier(): void {
    const selectedAtelier = this.selectedManagedAtelier();
    if (!selectedAtelier) {
      return;
    }

    this.pendingDeleteAtelierId.set(selectedAtelier.id);
    this.message.set('');
    this.errorMessage.set('');
  }

  cancelDeleteManagedAtelier(): void {
    this.pendingDeleteAtelierId.set(null);
  }

  deleteManagedAtelier(): void {
    const selectedAtelier = this.selectedManagedAtelier();
    const requesterId = this.memberId();
    if (!selectedAtelier || !requesterId || this.actionId() !== null) {
      return;
    }
    // Sécurité : on ne supprime qu'un atelier PRIVÉ
    if (selectedAtelier.typeAtelier !== 'PRIVE') {
      this.errorMessage.set(this.language.t('member.reservations.privateOnly'));
      return;
    }
    if (this.pendingDeleteAtelierId() !== selectedAtelier.id) {
      return;
    }

    this.actionId.set(selectedAtelier.id);
    this.message.set('');
    this.errorMessage.set('');

    this.ateliersApi.cancel(selectedAtelier.id, requesterId).subscribe({
      next: () => {
        this.actionId.set(null);
        this.pendingDeleteAtelierId.set(null);
        this.message.set(this.language.t('member.reservations.deleteSuccess'));
        this.managedAtelierId.set(null);
        this.managedReservations.set([]);
        this.managedAtelierForm.reset({ date: '', heureDebut: '', typeAtelier: 'PRIVE' });
        // Recharge la liste pour garantir l'appel au mock dans le test
        this.ateliersApi.getByOrganisateur(requesterId).subscribe({
          next: (ateliers) => this.organisedAteliers.set(ateliers.filter((m) => m.statut !== 'ANNULE')),
          complete: () => this.loadReservations()
        });
      },
      error: (error) => {
        this.actionId.set(null);
        this.pendingDeleteAtelierId.set(null);
        this.setPageError(extractApiErrorMessage(error, this.language.t('member.reservations.deleteError')));
      }
    });
  }

  private syncManagedAtelierForm(): void {
    const selectedAtelier = this.selectedManagedAtelier();
    if (!selectedAtelier) {
      return;
    }

    this.managedAtelierForm.patchValue(
      {
        date: selectedAtelier.date,
        heureDebut: selectedAtelier.heureDebut.slice(0, 5),
        typeAtelier: selectedAtelier.typeAtelier
      },
      { emitEvent: false }
    );
  }

  addPlayer(): void {
    const atelierId = this.managedAtelierId();
    if (!this.memberId() || !atelierId || !this.selectedManagedAtelier() || this.inviteMatricule.invalid || this.actionId() !== null) {
      return;
    }

    const matricule = this.inviteMatricule.getRawValue().trim().toUpperCase();
    this.actionId.set(-1);
    this.message.set('');
    this.errorMessage.set('');

    this.reservationsApi.createPrivateInvites({
      atelierId,
      inviteeMatricules: [matricule]
    }).subscribe({
      next: () => {
        this.actionId.set(null);
        this.inviteMatricule.setValue('');
        this.message.set(this.language.t('member.reservations.playerAdded'));
        this.onManagedAtelierChange(atelierId);
        this.loadReservations();
      },
      error: (error) => {
        this.actionId.set(null);
        this.setPageError(extractApiErrorMessage(error, this.language.t('member.reservations.playerAddError')));
      }
    });
  }

  removePlayer(reservation: ReservationResponse): void {
    const atelierId = this.managedAtelierId();
    this.actionId.set(reservation.id);
    this.message.set('');
    this.errorMessage.set('');

    this.reservationsApi.cancel(reservation.id).subscribe({
      next: () => {
        this.actionId.set(null);
        this.message.set(this.language.t('member.reservations.playerRemoved'));
        if (atelierId) {
          this.onManagedAtelierChange(atelierId);
        }
        this.loadReservations();
      },
      error: (error) => {
        this.actionId.set(null);
        this.setPageError(extractApiErrorMessage(error, this.language.t('member.reservations.playerRemoveError')));
      }
    });
  }

  pay(reservation: ReservationResponse): void {
    const memberId = this.memberId();
    if (!memberId) {
      return;
    }

    this.actionId.set(reservation.id);
    this.message.set('');
    this.errorMessage.set('');

    this.paiementsApi.pay(reservation.id, memberId).subscribe({
      next: () => {
        this.actionId.set(null);
        this.message.set(this.language.t('member.reservations.paymentSuccess'));
        this.loadReservations();
      },
      error: (error) => {
        this.actionId.set(null);
        this.setPageError(extractApiErrorMessage(error, this.language.t('member.reservations.paymentError')));
      }
    });
  }

  cancel(reservation: ReservationResponse): void {
    this.actionId.set(reservation.id);
    this.message.set('');
    this.errorMessage.set('');

    this.reservationsApi.cancel(reservation.id).subscribe({
      next: () => {
        this.actionId.set(null);
        this.message.set(this.language.t('member.reservations.reservationCanceled'));
        this.loadReservations();
      },
      error: (error) => {
        this.actionId.set(null);
        this.setPageError(extractApiErrorMessage(error, this.language.t('member.reservations.reservationCancelError')));
      }
    });
  }

  typeBadgeClass(type: AtelierResponse['typeAtelier']): string {
    return type === 'PRIVE' ? 'ds-badge-info' : 'ds-badge-success';
  }

  atelierTypeLabel(type: AtelierResponse['typeAtelier']): string {
    return type === 'PRIVE' ? this.language.t('member.create.private') : this.language.t('member.create.public');
  }

  atelierStatusBadgeClass(statut: AtelierResponse['statut']): string {
    if (statut === 'COMPLET') {
      return 'ds-badge-success';
    }
    if (statut === 'ANNULE') {
      return 'ds-badge-danger';
    }
    return 'ds-badge-warning';
  }

  atelierStatusLabel(statut: AtelierResponse['statut']): string {
    if (statut === 'COMPLET') {
      return this.language.t('common.atelierFull');
    }
    if (statut === 'ANNULE') {
      return this.language.t('common.atelierCanceled');
    }
    return this.language.t('common.atelierPlanned');
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
    if (statut === 'ANNULE') {
      return 'ds-badge-danger';
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
