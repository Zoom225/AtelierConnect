import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AteliersApiService } from '../../../core/api/ateliers-api.service';
import { ReservationsApiService } from '../../../core/api/reservations-api.service';
import { CentresApiService } from '../../../core/api/centres-api.service';
import { MemberSessionService } from '../../../core/auth/member-session.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { AtelierResponse } from '../../../shared/models/atelier.model';
import { ATELIER_CAPACITY } from '../../../shared/models/atelier-rules';
import { CentreResponse } from '../../../shared/models/centre-salle.model';
import { extractApiErrorMessage } from '../../../shared/utils/api-error.util';

@Component({
  selector: 'app-member-public-ateliers-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule
  ],
  template: `
    <section class="page-shell">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 class="title-gradient ds-section-title">{{ language.t('member.public.title') }}</h1>
          <p class="ds-subtitle">{{ language.t('member.public.subtitle') }}</p>
        </div>
        <div class="toolbar-actions">
          <a mat-stroked-button routerLink="/member/profile">{{ language.t('member.public.myProfile') }}</a>
          <a mat-flat-button color="primary" routerLink="/member/ateliers/new">{{ language.t('member.public.create') }}</a>
        </div>
      </div>

      <mat-card class="card-soft">
        <mat-card-content class="grid gap-4 pt-4 md:grid-cols-3">
          <mat-form-field appearance="outline">
            <mat-label>{{ language.t('member.public.filter') }}</mat-label>
            <mat-select [value]="selectedCentreId()" (valueChange)="selectedCentreId.set($event)">
              <mat-option [value]="0">{{ language.t('common.allCentres') }}</mat-option>
              @for (site of centres(); track site.id) {
                <mat-option [value]="site.id">{{ site.nom }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="md:col-span-2">
            <mat-label>{{ language.t('member.public.search') }}</mat-label>
            <input
              matInput
              [value]="search()"
              (input)="search.set(($any($event.target)).value)"
              [placeholder]="language.t('member.public.searchPlaceholder')"
            />
          </mat-form-field>
        </mat-card-content>
      </mat-card>

      @if (loading()) {
        <div class="py-4">
          <mat-spinner diameter="32"></mat-spinner>
        </div>
      }

      @if (message()) {
        <p class="status-success">{{ message() }}</p>
      }

      @if (errorMessage()) {
        <p class="status-error">{{ errorMessage() }}</p>
      }

      <a routerLink="/member/ateliers/new" class="card-soft block rounded-2xl border border-sky-200 bg-gradient-to-r from-sky-50 to-indigo-50 p-5 no-underline transition hover:-translate-y-0.5 hover:shadow-md">
        <p class="text-sm font-medium uppercase tracking-wide text-sky-700">{{ language.t('member.public.noAtelierPrompt') }}</p>
        <div class="mt-2 flex flex-wrap items-center justify-between gap-3">
          <div>
            <p class="text-xl font-semibold text-slate-900">{{ language.t('member.public.create') }}</p>
            <p class="text-sm text-slate-600">{{ language.t('member.public.createBody') }}</p>
          </div>
          <span class="rounded-full bg-sky-600 px-4 py-2 text-sm font-semibold text-white">{{ language.t('member.public.create') }}</span>
        </div>
      </a>

      <div class="grid gap-4 lg:grid-cols-2">
        @for (atelier of filteredAteliers(); track atelier.id) {
          <mat-card class="card-soft">
            <mat-card-header>
              <mat-card-title>
                {{ atelier.salleNom }} - {{ atelier.centreNom }}
                @if (isOrganizer(atelier)) {
                  <span class="ml-2 ds-badge ds-badge-info">{{ language.t('member.public.myAtelier') }}</span>
                }
                @if (!isOrganizer(atelier) && atelier.nbParticipantsActuels >= ATELIER_CAPACITY) {
                  <span class="ml-2 ds-badge ds-badge-danger">{{ language.t('member.public.completion') }}</span>
                } @else if (!isOrganizer(atelier) && !canJoin(atelier)) {
                  <span class="ml-2 ds-badge ds-badge-warning">{{ language.t('member.public.none') }}</span>
                } @else if (!isOrganizer(atelier)) {
                  <span class="ml-2 ds-badge ds-badge-success">{{ language.t('member.public.available') }}</span>
                }
              </mat-card-title>
              <mat-card-subtitle>
                {{ atelier.date }} · {{ atelier.heureDebut }} - {{ atelier.heureFin }}
              </mat-card-subtitle>
            </mat-card-header>
            <mat-card-content class="ds-data-list">
              <div class="ds-data-row"><span class="ds-data-key">{{ language.t('member.public.organizer') }}</span><span class="ds-data-value">{{ atelier.organisateurNom }}</span></div>
              <div class="ds-data-row"><span class="ds-data-key">{{ language.t('member.public.status') }}</span><span class="ds-data-value"><span class="ds-badge" [class]="statusBadgeClass(atelier.statut)">{{ atelierStatusLabel(atelier.statut) }}</span></span></div>
              <div class="ds-data-row"><span class="ds-data-key">{{ language.t('member.public.type') }}</span><span class="ds-data-value"><span class="ds-badge" [class]="typeBadgeClass(atelier.typeAtelier)">{{ typeLabel(atelier.typeAtelier) }}</span></span></div>
              <div class="ds-data-row"><span class="ds-data-key">{{ language.t('member.public.players') }}</span><span class="ds-data-value">{{ atelier.nbParticipantsActuels }}/{{ ATELIER_CAPACITY }}</span></div>
              <div class="ds-data-row"><span class="ds-data-key">{{ language.t('member.public.price') }}</span><span class="ds-data-value">{{ atelier.prixParParticipant }} EUR</span></div>
            </mat-card-content>
            <mat-card-actions>
              <button
                mat-flat-button
                color="primary"
                type="button"
                (click)="joinAtelier(atelier)"
                [disabled]="joiningAtelierId() === atelier.id || !canJoin(atelier)"
              >
                {{ joiningAtelierId() === atelier.id ? language.t('member.public.registering') : language.t('member.public.join') }}
              </button>

              @if (isOrganizer(atelier)) {
                <button mat-stroked-button type="button" (click)="startEdit(atelier)" [disabled]="!canModify(atelier) || actionAtelierId() === atelier.id">
                  {{ language.t('member.public.modify') }}
                </button>
                <button mat-stroked-button color="warn" type="button" (click)="cancelOwnAtelier(atelier)" [disabled]="actionAtelierId() === atelier.id">
                  {{ language.t('member.public.delete') }}
                </button>
              }
            </mat-card-actions>

            @if (isOrganizer(atelier) && !canModify(atelier)) {
              <p class="px-4 pb-3 text-xs text-amber-700">{{ language.t('member.public.editHint') }}</p>
            }

            @if (editingAtelierId() === atelier.id) {
              <mat-card-content class="grid gap-3 border-t border-slate-100 pt-3 md:grid-cols-4">
                <mat-form-field appearance="outline">
                  <mat-label>{{ language.t('member.public.date') }}</mat-label>
                  <input matInput type="date" [formControl]="editForm.controls.date" />
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>{{ language.t('member.public.time') }}</mat-label>
                  <input matInput type="time" [formControl]="editForm.controls.heureDebut" />
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>{{ language.t('member.public.typeLabel') }}</mat-label>
                  <mat-select [formControl]="editForm.controls.typeAtelier">
                    <mat-option value="PUBLIC">{{ language.t('member.create.public') }}</mat-option>
                    <mat-option value="PRIVE">{{ language.t('member.create.private') }}</mat-option>
                  </mat-select>
                </mat-form-field>

                <div class="flex items-end gap-2">
                  <button mat-flat-button color="primary" type="button" (click)="saveEdit(atelier)" [disabled]="editForm.invalid || actionAtelierId() === atelier.id">
                    {{ language.t('common.save') }}
                  </button>
                  <button mat-stroked-button type="button" (click)="cancelEdit()" [disabled]="actionAtelierId() === atelier.id">
                    {{ language.t('common.back') }}
                  </button>
                </div>
              </mat-card-content>
            }
          </mat-card>
        } @empty {
          @if (!loading()) {
            <mat-card>
              <mat-card-content class="py-6 text-slate-600">
                {{ language.t('member.public.noAteliers') }}
              </mat-card-content>
            </mat-card>
          }
        }
      </div>
    </section>
  `
})
export class MemberPublicAteliersPage {
  readonly ATELIER_CAPACITY = ATELIER_CAPACITY;
  private readonly ateliersApi = inject(AteliersApiService);
  private readonly centresApi = inject(CentresApiService);
  private readonly reservationsApi = inject(ReservationsApiService);
  private readonly memberSession = inject(MemberSessionService);
  readonly language = inject(LanguageService);

  readonly loading = signal(false);
  readonly joiningAtelierId = signal<number | null>(null);
  readonly actionAtelierId = signal<number | null>(null);
  readonly editingAtelierId = signal<number | null>(null);
  readonly message = signal('');
  readonly errorMessage = signal('');
  readonly ateliers = signal<AtelierResponse[]>([]);
  readonly centres = signal<CentreResponse[]>([]);
  readonly selectedCentreId = signal<number>(0);
  readonly search = signal('');
  readonly editForm = new FormGroup({
    date: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    heureDebut: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    typeAtelier: new FormControl<'PUBLIC' | 'PRIVE'>('PUBLIC', { nonNullable: true, validators: [Validators.required] })
  });

  readonly filteredAteliers = computed(() => {
    const centreId = this.selectedCentreId();
    const search = this.search().trim().toLowerCase();

    return this.ateliers()
      .filter((atelier) => {
        const ateliersCentre = !centreId || this.centres().find((site) => site.id === centreId)?.nom === atelier.centreNom;
        const haystack = `${atelier.salleNom} ${atelier.centreNom} ${atelier.organisateurNom}`.toLowerCase();
        const ateliersSearch = !search || haystack.includes(search);
        return ateliersCentre && ateliersSearch;
      })
      .sort((a, b) => {
        const scoreA = this.isOrganizer(a) ? 0 : 1;
        const scoreB = this.isOrganizer(b) ? 0 : 1;
        return scoreA - scoreB;
      });
  });

  constructor() {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.message.set('');
    this.errorMessage.set('');

    forkJoin({
      ateliers: this.ateliersApi.getPublic(),
      centres: this.centresApi.getAll()
    }).subscribe({
      next: ({ ateliers, centres }) => {
        this.ateliers.set(ateliers);
        this.centres.set(centres);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.errorMessage.set(extractApiErrorMessage(error, this.language.t('member.public.loadError')));
      }
    });
  }

  joinAtelier(atelier: AtelierResponse): void {
    const memberId = this.memberSession.memberId();
    if (!memberId) {
      this.errorMessage.set(this.language.t('member.public.noMember'));
      return;
    }

    if (!this.canJoin(atelier)) {
      this.errorMessage.set(this.language.t('member.public.joinBlocked'));
      return;
    }

    this.joiningAtelierId.set(atelier.id);
    this.message.set('');
    this.errorMessage.set('');

    this.reservationsApi
      .create({
        atelierId: atelier.id,
        participantId: memberId,
        requesterId: memberId
      })
      .subscribe({
        next: () => {
          this.joiningAtelierId.set(null);
          this.message.set(this.language.t('member.public.joinSuccess'));
        },
        error: (error) => {
          this.joiningAtelierId.set(null);
          this.errorMessage.set(extractApiErrorMessage(error, this.language.t('member.public.joinError')));
        }
      });
  }

  isOrganizer(atelier: AtelierResponse): boolean {
    return atelier.organisateurId === this.memberSession.memberId();
  }

  canJoin(atelier: AtelierResponse): boolean {
    if (this.isOrganizer(atelier)) {
      return false;
    }

    return atelier.statut === 'PLANIFIE' && atelier.nbParticipantsActuels < ATELIER_CAPACITY;
  }

  canModify(atelier: AtelierResponse): boolean {
    return this.isOrganizer(atelier) && this.startsInMoreThan24Hours(atelier);
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

  typeBadgeClass(type: AtelierResponse['typeAtelier']): string {
    return type === 'PRIVE' ? 'ds-badge-info' : 'ds-badge-neutral';
  }

  typeLabel(type: AtelierResponse['typeAtelier']): string {
    return type === 'PRIVE' ? this.language.t('member.create.private') : this.language.t('member.create.public');
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

  startEdit(atelier: AtelierResponse): void {
    if (!this.canModify(atelier)) {
      this.errorMessage.set(this.language.t('member.public.modifyTooLate'));
      return;
    }

    this.editingAtelierId.set(atelier.id);
    this.editForm.patchValue({
      date: atelier.date,
      heureDebut: atelier.heureDebut.slice(0, 5),
      typeAtelier: atelier.typeAtelier
    });
  }

  cancelEdit(): void {
    this.editingAtelierId.set(null);
  }

  saveEdit(atelier: AtelierResponse): void {
    const memberId = this.memberSession.memberId();
    if (!memberId || this.editForm.invalid) {
      return;
    }

    this.actionAtelierId.set(atelier.id);
    this.message.set('');
    this.errorMessage.set('');

    this.ateliersApi
      .update(atelier.id, {
        salleId: atelier.salleId,
        organisateurId: memberId,
        date: this.editForm.controls.date.getRawValue(),
        heureDebut: this.editForm.controls.heureDebut.getRawValue(),
        typeAtelier: this.editForm.controls.typeAtelier.getRawValue()
      })
      .subscribe({
        next: () => {
          this.actionAtelierId.set(null);
          this.editingAtelierId.set(null);
          this.message.set(this.language.t('member.public.updateSuccess'));
          this.loadData();
        },
        error: (error) => {
          this.actionAtelierId.set(null);
          this.errorMessage.set(extractApiErrorMessage(error, this.language.t('member.public.updateError')));
        }
      });
  }

  cancelOwnAtelier(atelier: AtelierResponse): void {
    const memberId = this.memberSession.memberId();
    if (!memberId) {
      return;
    }

    if (!confirm(this.language.t('member.public.deleteConfirm', { id: atelier.id }))) {
      return;
    }

    this.actionAtelierId.set(atelier.id);
    this.message.set('');
    this.errorMessage.set('');

    this.ateliersApi.cancel(atelier.id, memberId).subscribe({
      next: () => {
        this.actionAtelierId.set(null);
        this.editingAtelierId.set(null);
        this.message.set(this.language.t('member.public.deleteSuccess'));
        this.loadData();
      },
      error: (error) => {
        this.actionAtelierId.set(null);
        this.errorMessage.set(extractApiErrorMessage(error, this.language.t('member.public.deleteError')));
      }
    });
  }

  private startsInMoreThan24Hours(atelier: AtelierResponse): boolean {
    const startDateTime = new Date(`${atelier.date}T${atelier.heureDebut}`);
    return startDateTime.getTime() - Date.now() >= 24 * 60 * 60 * 1000;
  }
}
