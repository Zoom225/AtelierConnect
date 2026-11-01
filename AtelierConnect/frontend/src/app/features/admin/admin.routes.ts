import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { LanguageService } from '../../core/i18n/language.service';
import { adminGuard } from '../../core/guards/admin.guard';

export const ADMIN_ROUTES: Routes = [
  {
	path: 'login',
	loadComponent: () => import('./pages/admin-login.page').then((m) => m.AdminLoginPage),
	title: () => `AtelierConnect - ${inject(LanguageService).t('nav.adminLogin')}`
  },
  {
	path: '',
	canActivate: [adminGuard],
	data: { roles: ['GLOBAL', 'SITE'] },
	loadComponent: () => import('./pages/admin-home.page').then((m) => m.AdminHomePage),
	title: () => `AtelierConnect - ${inject(LanguageService).t('nav.dashboard')}`
  },
  {
	path: 'participants',
	canActivate: [adminGuard],
	data: { roles: ['GLOBAL', 'SITE'] },
	loadComponent: () => import('./pages/admin-participants.page').then((m) => m.AdminParticipantsPage),
	title: () => `AtelierConnect - ${inject(LanguageService).t('nav.members')}`
  },
  {
	path: 'ateliers',
	canActivate: [adminGuard],
	data: { roles: ['GLOBAL', 'SITE'] },
	loadComponent: () => import('./pages/admin-ateliers.page').then((m) => m.AdminAteliersPage),
	title: () => `AtelierConnect - ${inject(LanguageService).t('nav.ateliers')}`
  },
  {
	path: 'centres',
	canActivate: [adminGuard],
	data: { roles: ['GLOBAL', 'SITE'] },
	loadComponent: () => import('./pages/admin-centres.page').then((m) => m.AdminCentresPage),
	title: () => `AtelierConnect - ${inject(LanguageService).t('nav.centres')}`
  },
  {
	path: 'salles',
	canActivate: [adminGuard],
	data: { roles: ['GLOBAL', 'SITE'] },
	loadComponent: () => import('./pages/admin-salles.page').then((m) => m.AdminSallesPage),
	title: () => `AtelierConnect - ${inject(LanguageService).t('nav.courts')}`
  },
  {
	path: 'members',
	canActivate: [adminGuard],
	data: { roles: ['GLOBAL', 'SITE'] },
	loadComponent: () => import('./pages/admin-participants.page').then((m) => m.AdminParticipantsPage),
	title: () => `AtelierConnect - ${inject(LanguageService).t('nav.members')}`
  },
  {
	path: 'matches',
	canActivate: [adminGuard],
	data: { roles: ['GLOBAL', 'SITE'] },
	loadComponent: () => import('./pages/admin-ateliers.page').then((m) => m.AdminAteliersPage),
	title: () => `AtelierConnect - ${inject(LanguageService).t('nav.ateliers')}`
  },
  {
	path: 'sites',
	canActivate: [adminGuard],
	data: { roles: ['GLOBAL', 'SITE'] },
	loadComponent: () => import('./pages/admin-centres.page').then((m) => m.AdminCentresPage),
	title: () => `AtelierConnect - ${inject(LanguageService).t('nav.centres')}`
  },
  {
	path: 'terrains',
	canActivate: [adminGuard],
	data: { roles: ['GLOBAL', 'SITE'] },
	loadComponent: () => import('./pages/admin-salles.page').then((m) => m.AdminSallesPage),
	title: () => `AtelierConnect - ${inject(LanguageService).t('nav.courts')}`
  },
  {
	path: 'fermetures',
	canActivate: [adminGuard],
	data: { roles: ['GLOBAL', 'SITE'] },
	loadComponent: () => import('./pages/admin-jours-fermeture.page').then((m) => m.AdminJoursFermeturePage),
	title: () => `AtelierConnect - ${inject(LanguageService).t('nav.closures')}`
  }
];

