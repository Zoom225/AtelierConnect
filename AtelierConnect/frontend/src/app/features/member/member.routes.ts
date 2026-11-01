import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { LanguageService } from '../../core/i18n/language.service';
import { memberGuard } from '../../core/guards/member.guard';

export const MEMBER_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/member-home.page').then((m) => m.MemberHomePage),
    title: () => `AtelierConnect - ${inject(LanguageService).t('nav.memberArea')}`
  },
  {
    path: 'profile',
    canActivate: [memberGuard],
    loadComponent: () => import('./pages/member-profile.page').then((m) => m.MemberProfilePage),
    title: () => `AtelierConnect - ${inject(LanguageService).t('nav.profile')}`
  },
  {
    path: 'ateliers',
    canActivate: [memberGuard],
    loadComponent: () => import('./pages/member-public-ateliers.page').then((m) => m.MemberPublicAteliersPage),
    title: () => `AtelierConnect - ${inject(LanguageService).t('nav.ateliers')}`
  },
  {
    path: 'ateliers/nouveau',
    canActivate: [memberGuard],
    loadComponent: () => import('./pages/member-create-atelier.page').then((m) => m.MemberCreateAtelierPage),
    title: () => `AtelierConnect - ${inject(LanguageService).t('nav.create')}`
  },
  {
    path: 'matches',
    canActivate: [memberGuard],
    loadComponent: () => import('./pages/member-public-ateliers.page').then((m) => m.MemberPublicAteliersPage),
    title: () => `AtelierConnect - ${inject(LanguageService).t('nav.ateliers')}`
  },
  {
    path: 'matches/new',
    canActivate: [memberGuard],
    loadComponent: () => import('./pages/member-create-atelier.page').then((m) => m.MemberCreateAtelierPage),
    title: () => `AtelierConnect - ${inject(LanguageService).t('nav.create')}`
  },
  {
    path: 'reservations',
    canActivate: [memberGuard],
    loadComponent: () => import('./pages/member-reservations.page').then((m) => m.MemberReservationsPage),
    title: () => `AtelierConnect - ${inject(LanguageService).t('nav.reservations')}`
  },
  {
    path: 'payments',
    canActivate: [memberGuard],
    loadComponent: () => import('./pages/member-payments.page').then((m) => m.MemberPaymentsPage),
    title: () => `AtelierConnect - ${inject(LanguageService).t('nav.payments')}`
  }
];
