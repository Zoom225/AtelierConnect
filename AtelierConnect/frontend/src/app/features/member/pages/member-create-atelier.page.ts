import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AteliersApiService } from '../../../core/api/ateliers-api.service';
import { ReservationsApiService } from '../../../core/api/reservations-api.service';
import { CentresApiService } from '../../../core/api/centres-api.service';
import { SallesApiService } from '../../../core/api/salles-api.service';
import { MemberSessionService } from '../../../core/auth/member-session.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { TypeParticipant, toParticipantCategory } from '../../../shared/models/enums.model';
import { ATELIER_TOTAL_PRICE_EUR, BOOKING_MAX_DAYS } from '../../../shared/models/atelier-rules';
import { AtelierResponse } from '../../../shared/models/atelier.model';
import { CentreResponse, SalleResponse } from '../../../shared/models/centre-salle.model';
import { extractApiErrorMessage } from '../../../shared/utils/api-error.util';

const formatDateForInput = (date: Date): string => {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const getMaxBookingDays = (memberType?: TypeParticipant | null): number => BOOKING_MAX_DAYS[memberType ?? 'VISITEUR'];

const createMaxBookingDateValidator = (getMaxDate: () => string): ValidatorFn => {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = `${control.value ?? ''}`.trim();
    if (!value) {
      return null;
    }

    const maxDate = getMaxDate();
    return value > maxDate ? { maxBookingDate: { maxDate, actualDate: value } } : null;
  };
};

@Component({
  selector: 'app-member-create-atelier-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  template: `
    <section class="page-shell max-w-6xl">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 class="title-gradient ds-section-title">{{ language.t('member.create.title') }}</h1>
          <p class="ds-subtitle">{{ language.t('member.create.subtitle') }}</p>
          <span class="ds-badge"
                [class]="form.controls.typeAtelier.value === 'PRIVE' ? 'ds-badge-info' : 'ds-badge-success'">
            {{ language.t('member.create.mode') }} : {{ form.controls.typeAtelier.value }}
          </span>
        </div>
        <div class="toolbar-actions">
          <a mat-stroked-button routerLink="/member/ateliers">{{ language.t('member.public.title') }}</a>
        </div>
      </div>

      <mat-card class="card-soft panel-gradient">
        <mat-card-content>
          <form [formGroup]="form" class="grid gap-4 pt-4 md:grid-cols-2" (ngSubmit)="submit()">
            <mat-form-field appearance="outline">
              <mat-label>{{ language.t('member.create.site') }}</mat-label>
              <mat-select formControlName="centreId" (valueChange)="onCentreChange($event)" [disabled]="isCentreMember()">
                @for (site of centres(); track site.id) {
                  <mat-option [value]="site.id">{{ site.nom }}</mat-option>
                }
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>{{ language.t('member.create.salle') }}</mat-label>
              <mat-select formControlName="salleId" [disabled]="!salles().length">
                @for (salle of salles(); track salle.id) {
                  <mat-option [value]="salle.id">{{ salle.nom }} - {{ ATELIER_TOTAL_PRICE_EUR }} EUR</mat-option>
                }
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>{{ language.t('member.create.date') }}</mat-label>
              <input matInput type="date" formControlName="date" [min]="todayDate()" [max]="maxBookingDate()" />
              <mat-hint>{{ language.t('member.create.dateHint') }} {{ todayDate() }} - {{ maxBookingDate() }}</mat-hint>
              @if (form.controls.date.hasError('maxBookingDate') && (form.controls.date.dirty || form.controls.date.touched)) {
                <mat-error>{{ bookingDelayErrorMessage() }}</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>{{ language.t('member.create.time') }}</mat-label>
              <input matInput type="time" formControlName="heureDebut" />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>{{ language.t('member.create.atelierType') }}</mat-label>
              <mat-select formControlName="typeAtelier">
                <mat-option value="PUBLIC">{{ language.t('member.create.public') }}</mat-option>
                <mat-option value="PRIVE">{{ language.t('member.create.private') }}</mat-option>
              </mat-select>
            </mat-form-field>

            <div class="rounded-lg border border-slate-200 bg-slate-50 p-4 text-sm text-slate-700 md:col-span-2">
              <p class="font-semibold text-slate-800">{{ language.t('member.create.rappel') }}</p>
              <ul class="ml-4 list-disc space-y-1">
                <li>{{ language.t('member.create.ruleGlobal') }}</li>
                <li>{{ language.t('member.create.ruleCentre') }}</li>
                <li>{{ language.t('member.create.ruleLibre') }}</li>
              </ul>
              <p class="mt-2 text-slate-500">{{ language.t('member.create.bookingLimit') }} {{ maxBookingDate() }}</p>
            </div>

            @if (errorMessage()) {
              <p class="status-error md:col-span-2">{{ errorMessage() }}</p>
            } @else if (!centres().length && !loading()) {
              <p class="status-info md:col-span-2">{{ language.t('member.create.noCentre') }}</p>
            }

            @if (form.controls.centreId.value && !salles().length && !loading()) {
              <p class="status-info md:col-span-2">{{ language.t('member.create.noSalle') }}</p>
            }

            @if (form.controls.typeAtelier.value === 'PRIVE') {
              <div class="md:col-span-2">
                <p class="mb-2 text-sm font-medium text-slate-800">{{ language.t('member.create.privatePlayers') }}</p>
                <div class="grid gap-3 md:grid-cols-3">
                  <mat-form-field appearance="outline">
                    <mat-label>{{ language.t('member.create.player2') }}</mat-label>
                    <input matInput formControlName="player1" />
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>{{ language.t('member.create.player3') }}</mat-label>
                    <input matInput formControlName="player2" />
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>{{ language.t('member.create.player4') }}</mat-label>
                    <input matInput formControlName="player3" />
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>{{ language.t('member.create.player5') }}</mat-label>
                    <input matInput formControlName="player4" />
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>{{ language.t('member.create.player6') }}</mat-label>
                    <input matInput formControlName="player5" />
                  </mat-form-field>
                </div>
              </div>
            }

            @if (message()) {
              <p class="status-success md:col-span-2">{{ message() }}</p>
            }

            <div class="toolbar-actions justify-start md:col-span-2">
              <button mat-flat-button color="primary" type="submit" [disabled]="loading() || form.invalid || !salles().length">
                {{ language.t('member.create.submit') }}
              </button>
              <a mat-stroked-button routerLink="/member/profile">{{ language.t('member.create.back') }}</a>
              @if (loading()) {
                <mat-spinner diameter="24"></mat-spinner>
              }
            </div>
          </form>
        </mat-card-content>
      </mat-card>
    </section>
  `
})
export class MemberCreateAtelierPage {
  readonly ATELIER_TOTAL_PRICE_EUR = ATELIER_TOTAL_PRICE_EUR;
  private readonly centresApi = inject(CentresApiService);
  private readonly sallesApi = inject(SallesApiService);
  private readonly ateliersApi = inject(AteliersApiService);
  private readonly reservationsApi = inject(ReservationsApiService);
  private readonly memberSession = inject(MemberSessionService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly language = inject(LanguageService);
  private static readonly HIDDEN_GENERIC_ERROR = 'Une erreur inattendue est survenue';

  readonly loading = signal(false);
  readonly message = signal('');
  readonly errorMessage = signal('');
  readonly centres = signal<CentreResponse[]>([]);
  readonly salles = signal<SalleResponse[]>([]);
  readonly member = computed(() => this.memberSession.member());
  readonly isCentreMember = computed(() => this.member()?.categorie === 'ABONNE');
  readonly todayDate = computed(() => {
    const today = new Date();
    today.setHours(12, 0, 0, 0);
    return formatDateForInput(today);
  });
  readonly maxBookingDate = computed(() => {
    const today = new Date();
    today.setHours(12, 0, 0, 0);
    today.setDate(today.getDate() + getMaxBookingDays(this.member()?.categorie));
    return formatDateForInput(today);
  });
  readonly bookingDelayErrorMessage = computed(() => {
    return this.language.t('member.create.bookingLimitError', {
      date: this.maxBookingDate()
    });
  });

  readonly bookingDelayProfileErrorMessage = computed(() => {
    const memberType = this.member()?.categorie ?? 'VISITEUR';
    return this.language.t('member.create.bookingLimitProfileError', {
      type: this.member()?.categorie ?? toParticipantCategory(memberType),
      days: getMaxBookingDays(memberType),
      date: this.maxBookingDate()
    });
  });

  readonly form = new FormGroup({
    centreId: new FormControl<number | null>(null, { validators: [Validators.required] }),
    salleId: new FormControl<number | null>(null, { validators: [Validators.required] }),
    date: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, createMaxBookingDateValidator(() => this.maxBookingDate())]
    }),
    heureDebut: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    typeAtelier: new FormControl<'PUBLIC' | 'PRIVE'>('PUBLIC', {
      nonNullable: true,
      validators: [Validators.required]
    }),
    player1: new FormControl('', { nonNullable: true }),
    player2: new FormControl('', { nonNullable: true }),
    player3: new FormControl('', { nonNullable: true }),
    player4: new FormControl('', { nonNullable: true }),
    player5: new FormControl('', { nonNullable: true })
  });

  constructor() {
    if (!this.memberSession.isAuthenticated()) {
      this.router.navigateByUrl('/member');
      return;
    }

    this.applyPreferredTypeFromQuery();
    this.applyDefaultStartTime();
    this.applyDateBounds();
    this.loadCentres();
  }

  private setPageError(message: string): void {
    this.errorMessage.set(
      message.trim() === MemberCreateAtelierPage.HIDDEN_GENERIC_ERROR ? '' : message
    );
  }

  private applyDefaultStartTime(): void {
    if (!this.form.controls.heureDebut.value) {
      this.form.controls.heureDebut.setValue('09:00');
    }
  }

  private applyPreferredTypeFromQuery(): void {
    const preferredType = this.route.snapshot.queryParamMap.get('type')?.toUpperCase();
    if (preferredType === 'PUBLIC' || preferredType === 'PRIVE') {
      this.form.controls.typeAtelier.setValue(preferredType);
    }
  }

  loadCentres(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.centresApi.getAll().subscribe({
      next: (centres) => {
        this.centres.set(centres);
        const member = this.member();
        if (member?.categorie === 'ABONNE' && member.centreId) {
          this.form.controls.centreId.setValue(member.centreId);
          this.onCentreChange(member.centreId);
          return;
        }

        if (centres.length) {
          const defaultCentreId = centres[0].id;
          this.form.controls.centreId.setValue(defaultCentreId);
          this.onCentreChange(defaultCentreId);
        }
        this.loading.set(false);
      },
      error: (error) => {
        this.setPageError(extractApiErrorMessage(error, this.language.t('member.create.noCentreLoaded')));
        this.loading.set(false);
      }
    });
  }

  onCentreChange(centreId: number | null): void {
    this.form.controls.salleId.setValue(null);
    this.salles.set([]);

    if (!centreId) {
      return;
    }

    this.loading.set(true);
    this.sallesApi.getByCentre(centreId).subscribe({
      next: (salles) => {
        this.salles.set(salles);
        if (salles.length) {
          this.form.controls.salleId.setValue(salles[0].id);
        }
        this.loading.set(false);
      },
      error: (error) => {
        const status = error?.status;
        if (status === 404) {
          this.setPageError(this.language.t('member.create.noCentreLoaded'));
        } else if (status === 500) {
          this.setPageError(this.language.t('member.create.noSalleLoaded'));
        } else {
          this.setPageError(extractApiErrorMessage(error, this.language.t('member.create.noSalleLoaded')));
        }
        this.salles.set([]);
        this.loading.set(false);
      }
    });
  }

  submit(): void {
    this.form.markAllAsTouched();

    if (this.form.invalid || this.loading()) {
      if (this.form.controls.date.hasError('maxBookingDate')) {
        this.setPageError(this.bookingDelayErrorMessage());
      }
      return;
    }

    const { salleId, date, heureDebut, typeAtelier } = this.form.getRawValue();

    if (!salleId) {
      this.setPageError(this.language.t('member.create.noSalle'));
      return;
    }

    if (typeAtelier !== 'PUBLIC' && typeAtelier !== 'PRIVE') {
      this.setPageError(this.language.t('member.create.invalidType'));
      return;
    }

    this.loading.set(true);
    this.message.set('');
    this.errorMessage.set('');

    const atelierDate = `${date}T${heureDebut}:00`;

    const payload: import('../../../shared/models/atelier.model').CreateAtelierRequest = {
      salleId,
      atelierDate,
      atelierType: typeAtelier
    };

    this.ateliersApi.create(payload).subscribe({
      next: (createdAtelier) => {
        this.handleInvites(createdAtelier);
      },
      error: (error) => {
        this.loading.set(false);
        this.setPageError(this.toFriendlyCreationErrorMessage(error));
      }
    });
  }

  private applyDateBounds(): void {
    const dateControl = this.form.controls.date;
    const minDate = this.todayDate();
    const maxDate = this.maxBookingDate();
    const selectedDate = dateControl.getRawValue();

    if (!selectedDate || selectedDate < minDate) {
      dateControl.setValue(minDate);
    } else if (selectedDate > maxDate) {
      dateControl.setValue(maxDate);
    }

    dateControl.updateValueAndValidity({ emitEvent: false });
  }

  private toFriendlyCreationErrorMessage(error: unknown): string {
    const apiMessage = extractApiErrorMessage(error, this.language.t('member.create.createError'));
    const status = (error as any)?.status || (error as any)?.error?.status;

    const errorMappings: Record<string, string> = {
      'outstanding balance': this.language.t('member.create.outstandingBalance'),
      'active penalty': this.language.t('member.create.activePenalty'),
      'ne peut pas reserver plus de': this.bookingDelayProfileErrorMessage(),
      'ce créneau est déjà réservé': this.language.t('member.create.slotTaken'),
      'site est fermé': this.language.t('member.create.centreClosed'),
      'en dehors des heures d\'ouverture': this.language.t('member.create.outOfHours'),
      'ne peut créer un atelier que sur son propre site': this.language.t('member.create.centreOnly')
    };

    const normalizedMessage = apiMessage
      .toLowerCase()
      .normalize('NFD')
      .replace(/\p{Diacritic}/gu, '')
      .replace(/ã©/g, 'e')
      .replace(/ã¨/g, 'e')
      .replace(/ãª/g, 'e')
      .replace(/ã /g, 'a');
    for (const [key, message] of Object.entries(errorMappings)) {
      const normalizedKey = key
        .toLowerCase()
        .normalize('NFD')
        .replace(/\p{Diacritic}/gu, '');
      if (normalizedMessage.includes(normalizedKey)) {
        return message;
      }
    }

    if (status === 500) {
      return this.language.t('member.create.internalError');
    }

    return apiMessage;
  }

  private handleInvites(createdAtelier: AtelierResponse): void {
    const invitees = [
      this.form.controls.player1.getRawValue(),
      this.form.controls.player2.getRawValue(),
      this.form.controls.player3.getRawValue(),
      this.form.controls.player4.getRawValue(),
      this.form.controls.player5.getRawValue()
    ]
      .map((value) => value.trim().toUpperCase())
      .filter(Boolean);

    if (this.form.controls.typeAtelier.getRawValue() !== 'PRIVE' || !invitees.length) {
      this.loading.set(false);
      this.navigateToReservations(createdAtelier.id, this.language.t('member.create.success'));
      return;
    }

    this.reservationsApi.createPrivateInvites({
      atelierId: createdAtelier.id,
      inviteeMatricules: invitees
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.navigateToReservations(createdAtelier.id, this.language.t('member.create.successPrivate'));
      },
      error: (error) => {
        this.loading.set(false);
        this.setPageError(extractApiErrorMessage(error, this.language.t('member.create.addPlayersError')));
        this.navigateToReservations(createdAtelier.id, this.language.t('member.create.partialSuccess'));
      }
    });
  }

  private navigateToReservations(createdAtelierId: number, successMessage: string): void {
    this.message.set(successMessage);
    setTimeout(
      () =>
        this.router.navigate(['/member/reservations'], {
          state: {
            createdAtelierId,
            successMessage
          }
        }),
      1000
    );
  }
}
