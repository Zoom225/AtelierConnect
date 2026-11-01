import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AteliersApiService } from '../../../core/api/ateliers-api.service';
import { MemberSessionService } from '../../../core/auth/member-session.service';
import { LanguageService } from '../../../core/i18n/language.service';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatProgressSpinnerModule
  ],
  template: `
    <section class="home-shell">
      <div class="home-hero">
        <div class="home-hero-copy">
          <p class="eyebrow">{{ copy().eyebrow }}</p>
          <h1>{{ copy().title }}</h1>
          <p class="lead">{{ copy().lead }}</p>

          <div class="hero-actions">
            <a mat-flat-button color="primary" routerLink="/member">
              {{ memberSession.isAuthenticated() ? copy().memberCtaConnected : copy().memberCta }}
            </a>
            <a mat-stroked-button routerLink="/admin/login">{{ copy().adminCta }}</a>
            <button mat-stroked-button type="button" (click)="checkApi()" [disabled]="loading()">
              {{ copy().testApi }}
            </button>
          </div>
        </div>

        <div class="workshop-panel" [attr.aria-label]="copy().atelierPreviewLabel">
          <div class="workshop-layout">
            <span></span>
            <span></span>
          </div>
          <div class="workshop-card live">
            <p>{{ copy().atelierPublic }}</p>
            <strong>{{ count() ?? '--' }}</strong>
            <span>{{ copy().atelierAvailable }}</span>
          </div>
          <div class="workshop-card">
            <p>{{ copy().reservation }}</p>
            <strong>{{ copy().simpleReservation }}</strong>
            <span>{{ copy().reservationBody }}</span>
          </div>
        </div>
      </div>

      @if (loading() || message() || error()) {
        <mat-card class="card-soft feedback-card">
          <mat-card-content>
            @if (loading()) {
              <div class="loading-line">
                <mat-spinner diameter="28"></mat-spinner>
                <span>{{ copy().loadingApi }}</span>
              </div>
            }

            @if (message()) {
              <p class="status-success">{{ message() }}</p>
            }
            @if (error()) {
              <p class="status-error">{{ error() }}</p>
            }
          </mat-card-content>
        </mat-card>
      }

      <div class="feature-grid">
        <mat-card class="feature-card">
          <mat-card-content>
            <span class="feature-code">01</span>
            <h2>{{ copy().feature1Title }}</h2>
            <p>{{ copy().feature1Body }}</p>
          </mat-card-content>
        </mat-card>
        <mat-card class="feature-card">
          <mat-card-content>
            <span class="feature-code">02</span>
            <h2>{{ copy().feature2Title }}</h2>
            <p>{{ copy().feature2Body }}</p>
          </mat-card-content>
        </mat-card>
        <mat-card class="feature-card">
          <mat-card-content>
            <span class="feature-code">03</span>
            <h2>{{ copy().feature3Title }}</h2>
            <p>{{ copy().feature3Body }}</p>
          </mat-card-content>
        </mat-card>
        <mat-card class="feature-card">
          <mat-card-content>
            <span class="feature-code">04</span>
            <h2>{{ copy().feature4Title }}</h2>
            <p>{{ copy().feature4Body }}</p>
          </mat-card-content>
        </mat-card>
      </div>
    </section>
  `,
  styles: [`
    .home-shell {
      width: min(1180px, calc(100% - 2rem));
      margin: 0 auto;
      padding: 3rem 0 4rem;
    }

    .home-hero {
      display: grid;
      grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
      gap: 2rem;
      align-items: stretch;
      min-height: 460px;
    }

    .home-hero-copy {
      display: flex;
      flex-direction: column;
      justify-content: center;
      padding: clamp(2rem, 4vw, 4rem);
      border: 1px solid rgba(15, 23, 42, 0.08);
      border-radius: 8px;
      background:
        linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(248, 250, 252, 0.88)),
        radial-gradient(circle at 8% 0%, rgba(16, 185, 129, 0.18), transparent 36%);
      box-shadow: 0 24px 60px rgba(15, 23, 42, 0.08);
    }

    .eyebrow {
      margin: 0 0 0.8rem;
      color: #047857;
      font-size: 0.78rem;
      font-weight: 900;
      letter-spacing: 0.12em;
      text-transform: uppercase;
    }

    h1 {
      margin: 0;
      color: #0f172a;
      font-size: clamp(3rem, 8vw, 6rem);
      line-height: 0.95;
      font-weight: 900;
      letter-spacing: 0;
    }

    .lead {
      max-width: 640px;
      margin: 1.3rem 0 0;
      color: #475569;
      font-size: 1.08rem;
      line-height: 1.75;
    }

    .hero-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 0.8rem;
      margin-top: 2rem;
    }

    .workshop-panel {
      position: relative;
      overflow: hidden;
      min-height: 420px;
      border-radius: 8px;
      border: 1px solid rgba(4, 120, 87, 0.18);
      background:
        linear-gradient(90deg, rgba(255, 255, 255, 0.54) 1px, transparent 1px),
        linear-gradient(180deg, rgba(255, 255, 255, 0.54) 1px, transparent 1px),
        linear-gradient(135deg, #0f766e, #22c55e 55%, #84cc16);
      background-size: 52px 52px, 52px 52px, auto;
      box-shadow: 0 24px 60px rgba(4, 120, 87, 0.16);
    }

    .workshop-layout {
      position: absolute;
      inset: 42px;
      border: 2px solid rgba(255, 255, 255, 0.48);
      border-radius: 28px;
      transform: rotate(-6deg);
      background:
        radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.2) 0 27%, transparent 28%),
        linear-gradient(135deg, rgba(255, 255, 255, 0.16), transparent 65%);
    }

    .workshop-layout::before {
      content: '';
      position: absolute;
      inset: 24% 22%;
      border: 16px solid rgba(255, 255, 255, 0.17);
      border-radius: 22px;
      box-shadow: 0 0 0 5px rgba(255, 255, 255, 0.07);
    }

    .workshop-layout span {
      position: absolute;
      width: 34px;
      height: 34px;
      border-radius: 10px;
      border: 2px solid rgba(255, 255, 255, 0.68);
      background: rgba(15, 118, 110, 0.56);
    }

    .workshop-layout span:first-child {
      top: 12%;
      left: 22%;
    }

    .workshop-layout span:last-child {
      right: 22%;
      bottom: 12%;
    }

    .workshop-card {
      position: absolute;
      right: 1.2rem;
      bottom: 1.2rem;
      width: min(240px, calc(100% - 2.4rem));
      padding: 1rem;
      border-radius: 8px;
      background: rgba(255, 255, 255, 0.92);
      box-shadow: 0 16px 36px rgba(15, 23, 42, 0.18);
      backdrop-filter: blur(12px);
    }

    .workshop-card.live {
      top: 1.2rem;
      bottom: auto;
      left: 1.2rem;
      right: auto;
    }

    .workshop-card p {
      margin: 0 0 0.35rem;
      color: #64748b;
      font-size: 0.78rem;
      font-weight: 800;
      text-transform: uppercase;
    }

    .workshop-card strong {
      display: block;
      color: #0f172a;
      font-size: 2rem;
      line-height: 1;
    }

    .workshop-card span {
      display: block;
      margin-top: 0.35rem;
      color: #475569;
      font-size: 0.9rem;
    }

    .feedback-card {
      margin-top: 1.5rem;
    }

    .loading-line {
      display: flex;
      align-items: center;
      gap: 0.8rem;
      color: #475569;
      font-weight: 700;
    }

    .feature-grid {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 1rem;
      margin-top: 1.5rem;
    }

    .feature-card {
      min-height: 190px;
      border-radius: 8px !important;
    }

    .feature-card h2 {
      margin: 1rem 0 0.5rem;
      color: #0f172a;
      font-size: 1.05rem;
      font-weight: 850;
    }

    .feature-card p {
      margin: 0;
      color: #64748b;
      line-height: 1.6;
    }

    .feature-code {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 2.3rem;
      height: 2.3rem;
      border-radius: 8px;
      background: #ecfdf5;
      color: #047857;
      font-weight: 900;
    }

    @media (max-width: 920px) {
      .home-hero,
      .feature-grid {
        grid-template-columns: 1fr;
      }

      .workshop-panel {
        min-height: 360px;
      }
    }
  `]
})
export class LandingPage {
  private readonly ateliersApi = inject(AteliersApiService);
  readonly memberSession = inject(MemberSessionService);
  private readonly language = inject(LanguageService);

  readonly loading = signal(false);
  readonly count = signal<number | null>(null);
  readonly errorCode = signal<'api' | ''>('');
  readonly message = computed(() => {
    if (this.count() === null) {
      return '';
    }

    return this.language.t('landing.apiSuccess', { count: this.count() ?? 0 });
  });
  readonly error = computed(() => {
    if (!this.errorCode()) {
      return '';
    }

    return this.language.t('landing.apiError');
  });
  readonly copy = computed(() => ({
    eyebrow: this.language.t('landing.eyebrow'),
    title: this.language.t('landing.title'),
    lead: this.language.t('landing.lead'),
    memberCta: this.language.t('landing.memberCta'),
    memberCtaConnected: this.language.t('landing.memberCtaConnected'),
    adminCta: this.language.t('landing.adminCta'),
    testApi: this.language.t('landing.testApi'),
    loadingApi: this.language.t('landing.loadingApi'),
    atelierPreviewLabel: this.language.t('landing.atelierPreviewLabel'),
    atelierPublic: this.language.t('landing.atelierPublic'),
    atelierAvailable: this.language.t('landing.atelierAvailable'),
    reservation: this.language.t('landing.reservation'),
    reservationBody: this.language.t('landing.reservationBody'),
    simpleReservation: this.language.t('landing.simpleReservation'),
    feature1Title: this.language.t('landing.feature1.title'),
    feature1Body: this.language.t('landing.feature1.body'),
    feature2Title: this.language.t('landing.feature2.title'),
    feature2Body: this.language.t('landing.feature2.body'),
    feature3Title: this.language.t('landing.feature3.title'),
    feature3Body: this.language.t('landing.feature3.body'),
    feature4Title: this.language.t('landing.feature4.title'),
    feature4Body: this.language.t('landing.feature4.body')
  }));

  checkApi(): void {
    this.loading.set(true);
    this.errorCode.set('');

    this.ateliersApi.getPublic().subscribe({
      next: (ateliers) => {
        this.count.set(ateliers.length);
        this.loading.set(false);
      },
      error: () => {
        this.count.set(null);
        this.errorCode.set('api');
        this.loading.set(false);
      }
    });
  }
}
