import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { ADMIN_SESSION_KEY, AdminSessionService } from './admin-session.service';

describe('AdminSessionService', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  it('devrait persister les informations d\'authentification admin', () => {
    TestBed.configureTestingModule({});
    const service = TestBed.inject(AdminSessionService);

    service.setSession({
      token: 'jwt-token',
      email: 'admin@example.com',
      nom: 'Admin',
      prenom: 'Global',
      role: 'GLOBAL',
      centreId: null
    });

    expect(service.isAuthenticated()).toBe(true);
    expect(service.token()).toBe('jwt-token');
    expect(service.isGlobalAdmin()).toBe(true);
  });

  it('devrait restaurer les informations d\'authentification admin depuis localStorage', () => {
    localStorage.setItem(
      ADMIN_SESSION_KEY,
      JSON.stringify({
        token: 'restored-token',
        email: 'site@example.com',
        nom: 'Admin',
        prenom: 'Centre',
        role: 'SITE',
        centreId: 2
      })
    );

    TestBed.configureTestingModule({});
    const service = TestBed.inject(AdminSessionService);

    expect(service.isAuthenticated()).toBe(true);
    expect(service.token()).toBe('restored-token');
    expect(service.isCentreAdmin()).toBe(true);
    expect(service.centreId()).toBe(2);
  });

  it('devrait effacer le contenu invalide de localStorage', () => {
    localStorage.setItem(ADMIN_SESSION_KEY, 'not-json');

    TestBed.configureTestingModule({});
    const service = TestBed.inject(AdminSessionService);

    expect(service.isAuthenticated()).toBe(false);
    expect(localStorage.getItem(ADMIN_SESSION_KEY)).toBeNull();
  });

  it('devrait effacer les informations d\'authentification admin', () => {
    TestBed.configureTestingModule({});
    const service = TestBed.inject(AdminSessionService);

    service.setSession({
      token: 'jwt-token',
      email: 'site@example.com',
      nom: 'Admin',
      prenom: 'Centre',
      role: 'SITE',
      centreId: 2
    });

    service.clearSession();

    expect(service.isAuthenticated()).toBe(false);
    expect(service.session()).toBeNull();
  });
});
