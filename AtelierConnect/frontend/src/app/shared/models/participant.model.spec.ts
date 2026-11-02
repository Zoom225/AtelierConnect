import { describe, expect, it } from 'vitest';
import { categorieParticipant } from './participant.model';
import { toParticipantCategory } from './enums.model';

describe('Catégories AtelierConnect', () => {
  it('associe les anciennes catégories aux nouvelles règles métier', () => {
    expect(toParticipantCategory('GLOBAL')).toBe('PARTENAIRE');
    expect(toParticipantCategory('SITE')).toBe('ABONNE');
    expect(toParticipantCategory('LIBRE')).toBe('VISITEUR');
  });

  it('accepte une réponse historique sans champ categorie', () => {
    expect(categorieParticipant({ typeMembre: 'SITE' })).toBe('ABONNE');
  });

  it('utilise en priorité la catégorie retournée par le backend', () => {
    expect(categorieParticipant({
      typeMembre: 'GLOBAL',
      categorie: 'PARTENAIRE'
    })).toBe('PARTENAIRE');
  });
});
