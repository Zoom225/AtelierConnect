import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router, RouterModule } from '@angular/router';
import { AdminSessionService } from './core/auth/admin-session.service';
import { MemberSessionService } from './core/auth/member-session.service';
import { LanguageService } from './core/i18n/language.service';
import { LanguageSwitcherComponent } from './shared/components/language-switcher.component';

@Component({
  selector: 'app-root',
  imports: [
    CommonModule,
    RouterModule,
    MatToolbarModule,
    MatButtonModule,
    LanguageSwitcherComponent,
  ],
  template: `
    <a href="#main-content" class="skip-link">{{ designCopy().skip }}</a>
    <header class="app-header">
      <div class="app-header-inner">
        <a routerLink="/" class="app-logo">
          <span class="app-logo-badge">A</span>
          <span class="app-logo-wordmark"
            ><span class="app-logo-text">AtelierConnect</span
            ><span class="app-logo-tagline">{{ designCopy().tagline }}</span></span
          >
        </a>
        <button
          type="button"
          class="menu-toggle"
          aria-controls="primary-nav"
          [attr.aria-expanded]="menuOpen()"
          [attr.aria-label]="designCopy().menu"
          (click)="menuOpen.set(!menuOpen())"
        >
          <span></span><span></span><span></span>
        </button>
        <nav
          id="primary-nav"
          class="app-nav"
          [class.is-open]="menuOpen()"
          [attr.aria-label]="designCopy().navigation"
          (click)="menuOpen.set(false)"
          (keydown.escape)="menuOpen.set(false)"
        >
          <a
            routerLink="/"
            routerLinkActive="nav-active"
            [routerLinkActiveOptions]="{ exact: true }"
            class="nav-link"
          >
            {{ navText().home }}
          </a>

          @if (!adminSession.isAuthenticated()) {
            <a routerLink="/admin/login" class="nav-link">{{ navText().admin }}</a>
          }

          @if (!memberSession.isAuthenticated()) {
            <a routerLink="/member" class="nav-btn-outline">{{ navText().member }}</a>
          }

          @if (memberSession.isAuthenticated()) {
            <div class="nav-divider"></div>
            <a routerLink="/member/profile" routerLinkActive="nav-active" class="nav-link">{{
              navText().profile
            }}</a>
            <a routerLink="/member/ateliers" routerLinkActive="nav-active" class="nav-link">{{
              navText().ateliers
            }}</a>
            <a routerLink="/member/reservations" routerLinkActive="nav-active" class="nav-link">{{
              navText().reservations
            }}</a>
            <a routerLink="/member/payments" routerLinkActive="nav-active" class="nav-link">{{
              navText().payments
            }}</a>
            <div class="nav-divider"></div>
            <a routerLink="/member/ateliers/nouveau" class="nav-btn-create">{{
              navText().create
            }}</a>
            <a
              routerLink="/member/ateliers/nouveau"
              [queryParams]="{ type: 'PUBLIC' }"
              class="nav-btn-green"
              >{{ navText().public }}</a
            >
            <a
              routerLink="/member/ateliers/nouveau"
              [queryParams]="{ type: 'PRIVE' }"
              class="nav-btn-purple"
              >{{ navText().private }}</a
            >
            <div class="nav-divider"></div>
            <button class="nav-btn-logout" type="button" (click)="logoutMember()">
              {{ navText().logout }}
            </button>
          }

          @if (adminSession.isAuthenticated()) {
            <div class="nav-divider"></div>
            <a
              routerLink="/admin"
              routerLinkActive="nav-active"
              [routerLinkActiveOptions]="{ exact: true }"
              class="nav-link"
              >{{ navText().dashboard }}</a
            >
            <a routerLink="/admin/participants" routerLinkActive="nav-active" class="nav-link">{{
              navText().members
            }}</a>
            <a routerLink="/admin/ateliers" routerLinkActive="nav-active" class="nav-link">{{
              navText().ateliers
            }}</a>
            <a routerLink="/admin/centres" routerLinkActive="nav-active" class="nav-link">{{
              navText().centres
            }}</a>
            <a routerLink="/admin/salles" routerLinkActive="nav-active" class="nav-link">{{
              navText().courts
            }}</a>
            <a routerLink="/admin/fermetures" routerLinkActive="nav-active" class="nav-link">{{
              navText().closures
            }}</a>
            <div class="nav-divider"></div>
            <button class="nav-btn-logout" type="button" (click)="logoutAdmin()">
              {{ navText().logout }}
            </button>
          }
        </nav>

        <app-language-switcher class="app-lang-switcher"></app-language-switcher>
      </div>
    </header>

    <main id="main-content" class="app-main-shell" tabindex="-1">
      <router-outlet></router-outlet>
    </main>
    <footer class="app-footer">
      <div class="footer-content">
        <a routerLink="/" class="footer-brand">AtelierConnect<span>✳</span></a>
        <p>{{ designCopy().footer }}</p>
        <div class="footer-links">
          <a routerLink="/member">{{ navText().member }}</a
          ><a routerLink="/admin/login">{{ navText().admin }}</a>
        </div>
      </div>
      <div class="footer-bottom">
        <span>© {{ year }} AtelierConnect</span><span>{{ designCopy().signature }}</span>
      </div>
    </footer>
  `,
  styles: [],
})
export class App {
  readonly menuOpen = signal(false);
  readonly year = new Date().getFullYear();
  readonly adminSession = inject(AdminSessionService);
  readonly memberSession = inject(MemberSessionService);
  private readonly language = inject(LanguageService);
  private readonly router = inject(Router);
  readonly designCopy = computed(() =>
    this.language.locale() === 'fr'
      ? {
          skip: 'Aller au contenu',
          tagline: 'Réservation d’ateliers créatifs',
          menu: 'Ouvrir ou fermer le menu',
          navigation: 'Navigation principale',
          footer: 'Un lieu, des idées et le plaisir de faire ensemble.',
          signature: 'Créer des choses. Tisser des liens.',
        }
      : {
          skip: 'Skip to content',
          tagline: 'Creative workshop booking',
          menu: 'Open or close menu',
          navigation: 'Main navigation',
          footer: 'A place, a few ideas, and the joy of making together.',
          signature: 'Make things. Make connections.',
        },
  );
  readonly navText = computed(() => ({
    home: this.language.t('nav.home'),
    admin: this.language.t('nav.admin'),
    member: this.language.t('nav.member'),
    profile: this.language.t('nav.profile'),
    ateliers: this.language.t('nav.ateliers'),
    dashboard: this.language.t('nav.dashboard'),
    members: this.language.t('nav.members'),
    centres: this.language.t('nav.centres'),
    courts: this.language.t('nav.courts'),
    closures: this.language.t('nav.closures'),
    reservations: this.language.t('nav.reservations'),
    payments: this.language.t('nav.payments'),
    create: this.language.t('nav.create'),
    public: this.language.t('nav.public'),
    private: this.language.t('nav.private'),
    logout: this.language.t('nav.logout'),
  }));

  logoutAdmin(): void {
    this.adminSession.clearSession();
    this.router.navigateByUrl('/admin/login');
  }

  logoutMember(): void {
    this.memberSession.clearMember();
    this.router.navigateByUrl('/member');
  }
}
