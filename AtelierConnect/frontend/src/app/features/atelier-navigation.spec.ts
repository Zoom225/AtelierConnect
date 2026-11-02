import { describe, expect, it } from 'vitest';
import { ADMIN_ROUTES } from './admin/admin.routes';
import { MEMBER_ROUTES } from './member/member.routes';

describe('Routes AtelierConnect et compatibilité des anciens liens', () => {
  it('expose les ateliers participants et conserve les anciennes URL protégées', () => {
    for (const path of ['ateliers', 'ateliers/nouveau', 'matches', 'matches/new']) {
      const route = MEMBER_ROUTES.find((item) => item.path === path);
      expect(route, `Route /member/${path} absente`).toBeDefined();
      expect(route!.canActivate?.length).toBeGreaterThan(0);
      expect(route!.loadComponent).toBeTypeOf('function');
    }
  });

  it('expose les chemins administratifs AtelierConnect sans rompre les anciens liens', () => {
    const paths = ['participants', 'ateliers', 'centres', 'salles',
      'members', 'matches', 'sites', 'terrains'];
    for (const path of paths) {
      const route = ADMIN_ROUTES.find((item) => item.path === path);
      expect(route, `Route /admin/${path} absente`).toBeDefined();
      expect(route!.canActivate?.length).toBeGreaterThan(0);
      expect(route!.loadComponent).toBeTypeOf('function');
    }
  });
});
