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
import { CentresApiService } from '../../../core/api/centres-api.service';
import { SallesApiService } from '../../../core/api/salles-api.service';
import { AdminSessionService } from '../../../core/auth/admin-session.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { CentreResponse, SalleRequest, SalleResponse } from '../../../shared/models/centre-salle.model';
import { extractApiErrorMessage } from '../../../shared/utils/api-error.util';

@Component({
  selector: 'app-admin-salles-page',
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
    <!-- En-tête salles -->
    <div class="adm-ter-header">
      <div class="adm-ter-header-inner">
        <div class="adm-ter-title-block">
          <span class="adm-ter-icon">📍</span>
          <div>
            <h1 class="adm-ter-title">{{ language.t('admin.salles.title') }}</h1>
            <p class="adm-ter-sub">{{ language.t('admin.salles.subtitle') }}</p>
          </div>
        </div>
        <a routerLink="/admin" class="adm-ter-back-btn">{{ language.t('common.back') }}</a>
      </div>
    </div>

    <section class="page-shell">
      <!-- Formulaire -->
      <div class="adm-ter-form-card">
        <div class="adm-ter-form-header">
          <span>{{ editingId() ? '✏️' : '➕' }}</span>
          <h2 class="adm-ter-form-title">{{ editingId() ? language.t('common.edit') : language.t('common.create') }}</h2>
        </div>
        <form [formGroup]="form" class="grid gap-4 pt-4 md:grid-cols-2" (ngSubmit)="save()">
          <mat-form-field appearance="outline">
            <mat-label>{{ language.t('admin.salles.name') }}</mat-label>
            <input matInput formControlName="nom" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>{{ language.t('admin.salles.site') }}</mat-label>
            <mat-select formControlName="centreId" [disabled]="adminSession.isCentreAdmin()">
              @for (site of centres(); track site.id) {
                <mat-option [value]="site.id">🏟️ {{ site.nom }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          @if (message()) {
            <div class="status-success md:col-span-2">✅ {{ message() }}</div>
          }
          @if (visibleErrorMessage()) {
            <div class="status-error md:col-span-2">❌ {{ errorMessage() }}</div>
          }

          <div class="flex flex-wrap items-center gap-3 md:col-span-2">
            <button class="adm-ter-btn-primary" type="submit" [disabled]="loading() || form.invalid">
              {{ editingId() ? language.t('common.save') : language.t('common.create') }}
            </button>
            <button class="adm-ter-btn-secondary" type="button" (click)="resetForm()">{{ language.t('common.reset') }}</button>
            @if (loading()) { <mat-spinner diameter="24"></mat-spinner> }
          </div>
        </form>
      </div>

      <!-- Liste des salles -->
      <div class="grid gap-5 md:grid-cols-2 lg:grid-cols-3">
        @for (salle of filteredSalles(); track salle.id) {
          <div class="adm-ter-card">
            <div class="adm-ter-card-header">
              <span class="adm-ter-card-num">#{{ salle.id }}</span>
              <div class="adm-ter-card-name">📍 {{ salle.nom }}</div>
              <div class="adm-ter-card-site">🏟️ {{ salle.centreNom }}</div>
            </div>
            <div class="adm-ter-card-actions">
              <button class="adm-ter-action-edit" type="button" (click)="edit(salle)">✏️ {{ language.t('common.edit') }}</button>
              <button class="adm-ter-action-delete" type="button" (click)="remove(salle.id)">🗑️ {{ language.t('common.delete') }}</button>
            </div>
          </div>
        } @empty {
          <div class="adm-ter-empty md:col-span-2 lg:col-span-3">
            <span>📍</span>
            <p>{{ language.t('admin.salles.empty') }}</p>
          </div>
        }
      </div>
    </section>

    <style>
      .adm-ter-header {
        background: linear-gradient(135deg, #134e4a 0%, #0f766e 50%, #0d9488 100%);
        padding: 1.5rem 2rem;
        box-shadow: 0 4px 16px rgba(15,118,110,0.3);
      }
      .adm-ter-header-inner {
        display: flex; align-items: center; justify-content: space-between;
        max-width: 1200px; margin: 0 auto; gap: 1rem; flex-wrap: wrap;
      }
      .adm-ter-title-block { display: flex; align-items: center; gap: 1rem; }
      .adm-ter-icon { font-size: 2.5rem; }
      .adm-ter-title { font-size: 1.6rem; font-weight: 800; color: #fff; margin: 0; }
      .adm-ter-sub { color: rgba(255,255,255,0.75); font-size: 0.85rem; margin: 0; }
      .adm-ter-back-btn {
        background: rgba(255,255,255,0.15); color: #fff; border: 2px solid rgba(255,255,255,0.35);
        border-radius: 9999px; padding: 0.45rem 1.1rem; font-weight: 700;
        font-size: 0.85rem; text-decoration: none; transition: background 0.15s; white-space: nowrap;
      }
      .adm-ter-back-btn:hover { background: rgba(255,255,255,0.25); }

      .adm-ter-form-card {
        background: #fff; border-radius: 1.25rem;
        box-shadow: 0 4px 20px rgba(15,118,110,0.1);
        padding: 1.5rem; border-left: 5px solid #0d9488;
      }
      .adm-ter-form-header { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.5rem; font-size: 1.5rem; }
      .adm-ter-form-title { font-size: 1.2rem; font-weight: 700; color: #134e4a; margin: 0; }

      .adm-ter-btn-primary {
        background: linear-gradient(135deg, #0f766e, #0d9488); color: #fff; border: none;
        border-radius: 0.6rem; padding: 0.65rem 1.5rem; font-weight: 700; cursor: pointer;
        box-shadow: 0 3px 10px rgba(15,118,110,0.3); transition: transform 0.15s;
      }
      .adm-ter-btn-primary:hover:not(:disabled) { transform: translateY(-1px); }
      .adm-ter-btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
      .adm-ter-btn-secondary {
        background: #f0fdfa; color: #0f766e; border: 1.5px solid #99f6e4;
        border-radius: 0.6rem; padding: 0.65rem 1.25rem; font-weight: 600; cursor: pointer;
        transition: background 0.15s;
      }
      .adm-ter-btn-secondary:hover { background: #ccfbf1; }

      .adm-ter-card {
        background: #fff; border-radius: 1.25rem;
        box-shadow: 0 4px 16px rgba(0,0,0,0.07); overflow: hidden;
        transition: transform 0.15s, box-shadow 0.15s;
        border-top: 4px solid #0d9488;
      }
      .adm-ter-card:hover { transform: translateY(-3px); box-shadow: 0 10px 28px rgba(0,0,0,0.12); }
      .adm-ter-card-header {
        padding: 1rem 1.25rem 0.75rem;
        background: linear-gradient(135deg, #f0fdfa, #ccfbf1);
      }
      .adm-ter-card-num {
        display: inline-block; font-size: 0.72rem; font-weight: 800; color: #0f766e;
        background: #99f6e4; border-radius: 9999px; padding: 0.15rem 0.6rem; margin-bottom: 0.4rem;
      }
      .adm-ter-card-name { font-size: 1.05rem; font-weight: 700; color: #134e4a; }
      .adm-ter-card-site { font-size: 0.85rem; color: #0f766e; margin-top: 0.2rem; }

      .adm-ter-card-actions {
        display: flex; gap: 0.75rem; padding: 0.75rem 1.25rem;
        border-top: 1px solid #f0fdfa; flex-wrap: wrap;
      }
      .adm-ter-action-edit {
        flex: 1; min-width: 100px; background: #eff6ff; color: #1d4ed8;
        border: 1.5px solid #bfdbfe; border-radius: 0.5rem; padding: 0.45rem 0.75rem;
        font-size: 0.82rem; font-weight: 700; cursor: pointer; transition: background 0.15s;
      }
      .adm-ter-action-edit:hover { background: #dbeafe; }
      .adm-ter-action-delete {
        flex: 1; min-width: 100px; background: #fef2f2; color: #dc2626;
        border: 1.5px solid #fecaca; border-radius: 0.5rem; padding: 0.45rem 0.75rem;
        font-size: 0.82rem; font-weight: 700; cursor: pointer; transition: background 0.15s;
      }
      .adm-ter-action-delete:hover { background: #fee2e2; }

      .adm-ter-empty {
        text-align: center; padding: 3rem; color: #94a3b8;
        display: flex; flex-direction: column; align-items: center; gap: 0.5rem; font-size: 1.1rem;
      }
      .adm-ter-empty span { font-size: 3rem; }
    </style>
  `
})
export class AdminSallesPage {
  private readonly sallesApi = inject(SallesApiService);
  private readonly centresApi = inject(CentresApiService);
  readonly adminSession = inject(AdminSessionService);
  readonly language = inject(LanguageService);
  private static readonly HIDDEN_GENERIC_ERROR = 'Une erreur inattendue est survenue';

  readonly loading = signal(false);
  readonly message = signal('');
  readonly errorMessage = signal('');
  readonly editingId = signal<number | null>(null);
  readonly salles = signal<SalleResponse[]>([]);
  readonly centres = signal<CentreResponse[]>([]);
  readonly visibleErrorMessage = computed(() => {
    const message = this.errorMessage().trim();
    return message === AdminSallesPage.HIDDEN_GENERIC_ERROR ? '' : message;
  });

  readonly filteredSalles = computed(() => {
    if (this.adminSession.isGlobalAdmin()) {
      return this.salles();
    }
    return this.salles().filter((salle) => salle.centreId === this.adminSession.centreId());
  });

  readonly form = new FormGroup({
    nom: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    centreId: new FormControl<number | null>(null, { validators: [Validators.required] })
  });

  constructor() {
    this.loadData();
  }

  private setPageError(message: string): void {
    this.errorMessage.set(
      message.trim() === AdminSallesPage.HIDDEN_GENERIC_ERROR ? '' : message
    );
  }

  loadData(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    const centreId = this.adminSession.centreId();
    const sallesRequest$ =
      this.adminSession.isCentreAdmin() && centreId
        ? this.sallesApi.getByCentre(centreId)
        : this.sallesApi.getAll();
    const centresRequest$ =
      this.adminSession.isCentreAdmin() && centreId
        ? this.centresApi.getById(centreId)
        : this.centresApi.getAll();

    forkJoin({
      salles: sallesRequest$,
      centres: centresRequest$
    }).subscribe({
      next: ({ salles, centres }) => {
        this.salles.set(salles);
        const availableCentres = Array.isArray(centres) ? centres : [centres];
        const filteredCentres = this.adminSession.isGlobalAdmin()
          ? availableCentres
          : availableCentres.filter((site) => site.id === centreId);
        this.centres.set(filteredCentres);
        if (this.adminSession.isCentreAdmin() && centreId) {
          this.form.controls.centreId.setValue(centreId);
        }
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.setPageError(extractApiErrorMessage(error, this.language.t('admin.salles.loadError')));
      }
    });
  }

  edit(salle: SalleResponse): void {
    this.editingId.set(salle.id);
    this.form.patchValue({ nom: salle.nom, centreId: salle.centreId });
  }

  resetForm(): void {
    this.editingId.set(null);
    this.message.set('');
    this.errorMessage.set('');
    this.form.reset({ nom: '', centreId: this.adminSession.isCentreAdmin() ? this.adminSession.centreId() : null });
  }

  save(): void {
    if (this.form.invalid || this.loading()) {
      return;
    }

    this.loading.set(true);
    this.message.set('');
    this.errorMessage.set('');
    const payload = this.form.getRawValue() as SalleRequest;
    if (this.adminSession.isCentreAdmin()) {
      payload.centreId = this.adminSession.centreId()!;
    }

    const request$ = this.editingId() ? this.sallesApi.update(this.editingId()!, payload) : this.sallesApi.create(payload);
    request$.subscribe({
      next: () => {
        const wasEditing = this.editingId() !== null;
        this.loading.set(false);
        this.resetForm();
        this.message.set(wasEditing ? this.language.t('admin.salles.updateSuccess') : this.language.t('admin.salles.createSuccess'));
        this.loadData();
      },
      error: (error) => {
        this.loading.set(false);
        this.setPageError(extractApiErrorMessage(error, this.language.t('admin.salles.saveError')));
      }
    });
  }

  remove(id: number): void {
    this.message.set('');
    this.errorMessage.set('');

    if (!confirm(this.language.t('admin.salles.deleteConfirm'))) {
      return;
    }

    this.sallesApi.delete(id).subscribe({
      next: () => {
        this.message.set(this.language.t('admin.salles.deleteSuccess'));
        this.loadData();
      },
      error: (error) => {
        this.setPageError(extractApiErrorMessage(error, this.language.t('admin.salles.deleteError')));
      }
    });
  }
}
