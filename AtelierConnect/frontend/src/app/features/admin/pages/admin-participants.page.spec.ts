import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ParticipantsApiService } from '../../../core/api/participants-api.service';
import { CentresApiService } from '../../../core/api/centres-api.service';
import { AdminSessionService } from '../../../core/auth/admin-session.service';
import { ParticipantResponse } from '../../../shared/models/participant.model';
import { CentreResponse } from '../../../shared/models/centre-salle.model';
import { AdminParticipantsPage } from './admin-participants.page';

describe('AdminParticipantsPage', () => {
  const members: ParticipantResponse[] = [
	{ id: 1, matricule: 'G1001', nom: 'Martin', prenom: 'Lucas', email: 'lucas@email.com', categorie: 'PARTENAIRE', centreId: null, centreNom: null, solde: 0 },
	{ id: 2, matricule: 'S10001', nom: 'Bernard', prenom: 'Tom', email: 'tom@email.com', categorie: 'ABONNE', centreId: 1, centreNom: 'Centre collaboratif Lyon', solde: 5 },
	{ id: 3, matricule: 'S10002', nom: 'Leroy', prenom: 'Sarah', email: 'sarah@email.com', categorie: 'ABONNE', centreId: 2, centreNom: 'Centre collaboratif Paris', solde: 0 }
  ];

  const centres: CentreResponse[] = [
	{ id: 1, nom: 'Centre collaboratif Lyon', adresse: 'Lyon', heureOuverture: '08:00', heureFermeture: '22:00', dureeAtelierMinutes: 120, dureeEntreAtelierMinutes: 20, anneeCivile: 2026 },
	{ id: 2, nom: 'Centre collaboratif Paris', adresse: 'Paris', heureOuverture: '07:00', heureFermeture: '23:00', dureeAtelierMinutes: 120, dureeEntreAtelierMinutes: 20, anneeCivile: 2026 }
  ];

  let participantsApiMock: {
	getAll: ReturnType<typeof vi.fn>;
	create: ReturnType<typeof vi.fn>;
	update: ReturnType<typeof vi.fn>;
	delete: ReturnType<typeof vi.fn>;
	hasPenalty: ReturnType<typeof vi.fn>;
  };
  let centresApiMock: { getAll: ReturnType<typeof vi.fn> };
  let adminSessionMock: {
	isGlobalAdmin: ReturnType<typeof vi.fn>;
	isCentreAdmin: ReturnType<typeof vi.fn>;
	centreId: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
	participantsApiMock = {
	  getAll: vi.fn().mockReturnValue(of(members)),
	  create: vi.fn().mockReturnValue(of(members[0])),
	  update: vi.fn().mockReturnValue(of(members[1])),
	  delete: vi.fn().mockReturnValue(of(void 0)),
	  hasPenalty: vi.fn((id: number) => of(id === 2))
	};
	centresApiMock = {
	  getAll: vi.fn().mockReturnValue(of(centres))
	};
	adminSessionMock = {
	  isGlobalAdmin: vi.fn().mockReturnValue(false),
	  isCentreAdmin: vi.fn().mockReturnValue(true),
	  centreId: vi.fn().mockReturnValue(1)
	};

	await TestBed.configureTestingModule({
	  imports: [AdminParticipantsPage],
	  providers: [
		provideRouter([]),
		provideNoopAnimations(),
		{ provide: ParticipantsApiService, useValue: participantsApiMock },
		{ provide: CentresApiService, useValue: centresApiMock },
		{ provide: AdminSessionService, useValue: adminSessionMock }
	  ]
	}).compileComponents();
  });

  afterEach(() => {
	vi.restoreAllMocks();
	TestBed.resetTestingModule();
  });

  it('charge uniquement les participants et centres autorises pour un admin de site', () => {
	const fixture = TestBed.createComponent(AdminParticipantsPage);
	const component = fixture.componentInstance;

	expect(component.filteredMembers()).toHaveLength(2);
	expect(component.filteredMembers().map((m) => m.id)).toEqual([1, 2]);
	expect(component.centres()).toHaveLength(1);
	expect(component.centres()[0].id).toBe(1);
  });

  it('applique la recherche et les filtres par type', () => {
	adminSessionMock.isGlobalAdmin.mockReturnValue(true);
	adminSessionMock.isCentreAdmin.mockReturnValue(false);
	adminSessionMock.centreId.mockReturnValue(null);
	const fixture = TestBed.createComponent(AdminParticipantsPage);
	const component = fixture.componentInstance;

	component.searchQuery.set('sarah');
	expect(component.displayedMembers()).toHaveLength(1);
	expect(component.displayedMembers()[0].matricule).toBe('S10002');

	component.searchQuery.set('');
	component.typeFilter.set('ABONNE');
	expect(component.displayedMembers()).toHaveLength(2);
	expect(component.countByType('ABONNE')).toBe(2);
  });

  it('utilise la catégorie native et conserve les anciens codes pour les filtres', () => {
    const fixture = TestBed.createComponent(AdminParticipantsPage);
    const component = fixture.componentInstance;
    expect(component.categorieParticipant(members[1])).toBe('ABONNE');
    expect(component.categorieParticipant(members[0])).toBe('PARTENAIRE');
    component.typeFilter.set('ABONNE');
    expect(component.displayedMembers()).toHaveLength(1);
  });

  it('affiche le statut de penalite active d un participant', () => {
	const fixture = TestBed.createComponent(AdminParticipantsPage);
	const component = fixture.componentInstance;
	fixture.detectChanges();

	expect(participantsApiMock.hasPenalty).toHaveBeenCalledWith(2);
	expect(component.hasActivePenalty(2)).toBe(true);
	expect(fixture.nativeElement.textContent).toContain('Pénalité active');
  });

  it('passe en mode édition puis sauvegarde la mise à jour du participant', () => {
	const scrollSpy = vi.spyOn(window, 'scrollTo').mockImplementation(() => undefined);
	const fixture = TestBed.createComponent(AdminParticipantsPage);
	const component = fixture.componentInstance;

	component.edit(members[1]);

	expect(component.editingId()).toBe(2);
	expect(component.form.controls.matricule.value).toBe('S10001');
	expect(scrollSpy).toHaveBeenCalled();

	component.form.controls.nom.setValue('Bernard-Modifie');
	component.save();

	expect(participantsApiMock.update).toHaveBeenCalledWith(
	  2,
	  expect.objectContaining({ matricule: 'S10001', nom: 'Bernard-Modifie', categorie: 'ABONNE', centreId: 1 })
	);
	expect(component.message()).toContain('Participant mis à jour');
	expect(component.editingId()).toBeNull();
  });

  it('supprime un participant après confirmation', () => {
	vi.spyOn(window, 'confirm').mockReturnValue(true);
	const fixture = TestBed.createComponent(AdminParticipantsPage);
	const component = fixture.componentInstance;

	component.remove(2);

	expect(participantsApiMock.delete).toHaveBeenCalledWith(2);
	expect(component.message()).toContain('Participant supprimé');
  });
});
