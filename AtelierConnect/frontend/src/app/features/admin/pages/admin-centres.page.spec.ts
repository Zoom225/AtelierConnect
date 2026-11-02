import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CentresApiService } from '../../../core/api/centres-api.service';
import { AdminSessionService } from '../../../core/auth/admin-session.service';
import { AdminCentresPage } from './admin-centres.page';

describe('AdminCentresPage — centres AtelierConnect', () => {
  let centresApiMock: {
    getAll: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    delete: ReturnType<typeof vi.fn>;
  };
  let adminSessionMock: {
    isGlobalAdmin: ReturnType<typeof vi.fn>;
    isCentreAdmin: ReturnType<typeof vi.fn>;
    centreId: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    centresApiMock = {
      getAll: vi.fn().mockReturnValue(of([])),
      create: vi.fn().mockReturnValue(of({ id: 1 })),
      update: vi.fn().mockReturnValue(of({ id: 1 })),
      delete: vi.fn().mockReturnValue(of(void 0))
    };
    adminSessionMock = {
      isGlobalAdmin: vi.fn().mockReturnValue(true),
      isCentreAdmin: vi.fn().mockReturnValue(false),
      centreId: vi.fn().mockReturnValue(null)
    };
    await TestBed.configureTestingModule({
      imports: [AdminCentresPage],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: CentresApiService, useValue: centresApiMock },
        { provide: AdminSessionService, useValue: adminSessionMock }
      ]
    }).compileComponents();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    TestBed.resetTestingModule();
  });

  it('propose 120 minutes et 20 minutes de pause pour un nouveau centre', () => {
    const component = TestBed.createComponent(AdminCentresPage).componentInstance;
    expect(component.form.controls.dureeAtelierMinutes.value).toBe(120);
    expect(component.form.controls.dureeEntreAtelierMinutes.value).toBe(20);
    expect(component.form.controls.ouvertVisiteurs.value).toBe(false);
  });

  it('transmet le choix explicite d’ouverture aux visiteurs au backend', () => {
    const component = TestBed.createComponent(AdminCentresPage).componentInstance;
    component.form.patchValue({
      nom: 'Centre AtelierConnect Bruxelles',
      adresse: 'Bruxelles',
      ouvertVisiteurs: true
    });

    component.save();

    expect(centresApiMock.create).toHaveBeenCalledWith(expect.objectContaining({
      nom: 'Centre AtelierConnect Bruxelles',
      dureeAtelierMinutes: 120,
      dureeEntreAtelierMinutes: 20,
      ouvertVisiteurs: true
    }));
  });
});
