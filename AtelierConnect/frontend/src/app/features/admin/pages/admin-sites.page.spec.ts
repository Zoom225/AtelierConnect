import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { SitesApiService } from '../../../core/api/sites-api.service';
import { AdminSessionService } from '../../../core/auth/admin-session.service';
import { AdminSitesPage } from './admin-sites.page';

describe('AdminSitesPage — centres AtelierConnect', () => {
  let sitesApiMock: {
    getAll: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };
  let adminSessionMock: {
    isGlobalAdmin: ReturnType<typeof vi.fn>;
    isSiteAdmin: ReturnType<typeof vi.fn>;
    siteId: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    sitesApiMock = {
      getAll: vi.fn().mockReturnValue(of([])),
      create: vi.fn().mockReturnValue(of({ id: 1 })),
      update: vi.fn().mockReturnValue(of({ id: 1 })),
      delete: vi.fn().mockReturnValue(of(void 0))
    };
    adminSessionMock = {
      isGlobalAdmin: vi.fn().mockReturnValue(true),
      isSiteAdmin: vi.fn().mockReturnValue(false),
      siteId: vi.fn().mockReturnValue(null)
    };
    await TestBed.configureTestingModule({
      imports: [AdminSitesPage],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: SitesApiService, useValue: sitesApiMock },
        { provide: AdminSessionService, useValue: adminSessionMock }
      ]
    }).compileComponents();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    TestBed.resetTestingModule();
  });

  it('propose 120 minutes et 20 minutes de pause pour un nouveau centre', () => {
    const component = TestBed.createComponent(AdminSitesPage).componentInstance;
    expect(component.form.controls.dureeMatchMinutes.value).toBe(120);
    expect(component.form.controls.dureeEntreMatchMinutes.value).toBe(20);
    expect(component.form.controls.ouvertVisiteurs.value).toBe(false);
  });

  it('transmet le choix explicite d’ouverture aux visiteurs au backend', () => {
    const component = TestBed.createComponent(AdminSitesPage).componentInstance;
    component.form.patchValue({
      nom: 'Centre AtelierConnect Bruxelles',
      adresse: 'Bruxelles',
      ouvertVisiteurs: true
    });

    component.save();

    expect(sitesApiMock.create).toHaveBeenCalledWith(expect.objectContaining({
      nom: 'Centre AtelierConnect Bruxelles',
      dureeMatchMinutes: 120,
      dureeEntreMatchMinutes: 20,
      ouvertVisiteurs: true
    }));
  });
});
