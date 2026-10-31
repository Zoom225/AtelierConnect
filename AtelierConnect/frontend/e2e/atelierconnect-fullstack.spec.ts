import { expect, test, type APIRequestContext } from '@playwright/test';

type ParticipantLogin = { id: number; matricule: string; token: string; categorie: string };
type Atelier = {
  id: number;
  typeMatch: string;
  nbJoueursActuels: number;
  accordOuverturePublique?: boolean | null;
  dateAccordOuverturePublique?: string | null;
};
type Reservation = { id: number; matchId: number; statut: string; paiement?: { statut: string } };
type Salle = { id: number; nom: string; siteId: number };

const demoPassword = process.env.APP_DEMO_MEMBER_PASSWORD;
const adminPassword = process.env.APP_DEMO_ADMIN_PASSWORD;

async function login(api: APIRequestContext, matricule: string): Promise<ParticipantLogin> {
  if (!demoPassword) throw new Error('APP_DEMO_MEMBER_PASSWORD manquant pour les tests CI');
  const response = await api.post('/api/participants/login', {
    data: { matricule, password: demoPassword }
  });
  expect(response.status(), 'Authentification du participant de test').toBe(200);
  const participant = await response.json() as ParticipantLogin;
  expect(participant.token).toBeTruthy();
  return participant;
}

function bearer(token: string): Record<string, string> {
  return { Authorization: `Bearer ${token}` };
}

function dateDansSixJours(): string {
  const date = new Date();
  date.setUTCDate(date.getUTCDate() + 6);
  return `${date.toISOString().slice(0, 10)}T09:00:00`;
}

test('parcours complet : identité, atelier privé, invitation, paiement et consentement persistant', async ({ page }) => {
  test.setTimeout(120_000);

  // Les vérifications travaillent uniquement sur la base PostgreSQL de CI, jamais sur la production.
  const publicResponse = await page.request.get('/api/ateliers/public');
  expect(publicResponse.status()).toBe(200);
  expect(Array.isArray(await publicResponse.json())).toBe(true);

  const protectedResponse = await page.request.get('/api/ateliers');
  expect([401, 403]).toContain(protectedResponse.status());

  const organisateur = await login(page.request, 'G1002');
  expect(organisateur.categorie).toBe('PARTENAIRE');
  const invite = await login(page.request, 'S10001');

  const sallesResponse = await page.request.get('/api/salles');
  expect(sallesResponse.status()).toBe(200);
  const salles = await sallesResponse.json() as Salle[];
  const salle = salles.find(s => s.nom === 'Salle Création A');
  expect(salle, 'Une salle du centre de Bruxelles doit être initialisée').toBeDefined();

  const creation = await page.request.post('/api/ateliers', {
    headers: bearer(organisateur.token),
    data: { salleId: salle!.id, atelierDate: dateDansSixJours(), atelierType: 'PRIVE' }
  });
  expect(creation.status(), await creation.text()).toBe(201);
  const atelier = await creation.json() as Atelier;
  expect(atelier.typeMatch).toBe('PRIVE');
  expect(atelier.accordOuverturePublique).not.toBe(true);

  // Un participant différent ne peut pas gérer les ateliers de l'organisateur.
  const autreOrganisateur = await page.request.get(
    `/api/ateliers/organisateur/${organisateur.id}`, { headers: bearer(invite.token) }
  );
  expect([400, 403]).toContain(autreOrganisateur.status());

  const invitation = await page.request.post('/api/reservations/private-invites', {
    headers: bearer(organisateur.token),
    data: { matchId: atelier.id, inviteeMatricules: ['S10001'] }
  });
  expect(invitation.status(), await invitation.text()).toBe(201);
  const reservationsInvite = await invitation.json() as Reservation[];
  expect(reservationsInvite).toHaveLength(1);

  const nonInvite = await login(page.request, 'L10001');
  const accessRefuse = await page.request.get(`/api/ateliers/${atelier.id}`, {
    headers: bearer(nonInvite.token)
  });
  expect(accessRefuse.status()).toBe(403);

  const reservationsOrganisateur = await page.request.get(
    `/api/reservations/participant/${organisateur.id}`,
    { headers: bearer(organisateur.token) }
  );
  expect(reservationsOrganisateur.status()).toBe(200);
  const liste = await reservationsOrganisateur.json() as Reservation[];
  const reservationOrganisateur = liste.find(r => r.matchId === atelier.id);
  expect(reservationOrganisateur).toBeDefined();
  expect(reservationOrganisateur!.statut).toBe('EN_ATTENTE');

  const paymentDenied = await page.request.post(
    `/api/paiements/reservation/${reservationOrganisateur!.id}/pay`,
    { headers: bearer(invite.token) }
  );
  expect([400, 403]).toContain(paymentDenied.status());

  const paiementOrganisateur = await page.request.post(
    `/api/paiements/reservation/${reservationOrganisateur!.id}/pay`,
    { headers: bearer(organisateur.token) }
  );
  expect(paiementOrganisateur.status(), await paiementOrganisateur.text()).toBe(200);

  const paiementInvite = await page.request.post(
    `/api/paiements/reservation/${reservationsInvite[0].id}/pay`,
    { headers: bearer(invite.token) }
  );
  expect(paiementInvite.status(), await paiementInvite.text()).toBe(200);

  const confirmation = await page.request.get(`/api/ateliers/${atelier.id}`, {
    headers: bearer(organisateur.token)
  });
  expect(confirmation.status()).toBe(200);
  expect((await confirmation.json() as Atelier).nbJoueursActuels).toBe(2);

  // L'invité n'est pas l'organisateur : il ne peut pas donner son consentement.
  const accordInterdit = await page.request.patch(
    `/api/ateliers/${atelier.id}/consent-open-public`,
    { headers: bearer(invite.token), data: {} }
  );
  expect([400, 403]).toContain(accordInterdit.status());

  const accord = await page.request.patch(
    `/api/ateliers/${atelier.id}/consent-open-public`,
    { headers: bearer(organisateur.token), data: {} }
  );
  expect(accord.status()).toBe(204);

  const detail = await page.request.get(`/api/ateliers/${atelier.id}`, {
    headers: bearer(organisateur.token)
  });
  expect(detail.status()).toBe(200);
  const apresAccord = await detail.json() as Atelier;
  expect(apresAccord.accordOuverturePublique).toBe(true);
  expect(apresAccord.dateAccordOuverturePublique).toBeTruthy();

  // Une conversion anticipée à J-6 reste interdite, même avec accord.
  if (!adminPassword) throw new Error('APP_DEMO_ADMIN_PASSWORD manquant');
  const adminResponse = await page.request.post('/api/auth/login', {
    data: { email: 'admin@atelierconnect.example', password: adminPassword }
  });
  expect(adminResponse.status(), await adminResponse.text()).toBe(200);
  const administrateur = await adminResponse.json() as { token: string };
  expect(administrateur.token).toBeTruthy();
  const conversionTropTot = await page.request.patch(
    `/api/ateliers/${atelier.id}/convert-public`,
    { headers: bearer(administrateur.token), data: {} }
  );
  expect([400, 409]).toContain(conversionTropTot.status());

  // Véritable navigateur Chromium, connexion Angular et preuve visuelle du statut après actualisation.
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'AtelierConnect' })).toBeVisible();
  await page.goto('/member');
  await page.locator('input[formControlName="matricule"]').fill('G1002');
  await page.locator('input[formControlName="password"]').fill(demoPassword);
  await page.locator('form button[type="submit"]').click();
  await expect(page).toHaveURL(/\/member\/profile/, { timeout: 20_000 });

  await page.goto('/member/reservations');
  const choisirAtelier = page.locator('button').filter({ hasText: `#${atelier.id} -` }).first();
  await expect(choisirAtelier).toBeVisible();
  await choisirAtelier.click();

  const consentButton = page.getByRole('button', { name: /accord pour une éventuelle ouverture au public est enregistré/i });
  await expect(consentButton).toBeDisabled();

  await page.reload();
  const choisirApresActualisation = page.locator('button').filter({ hasText: `#${atelier.id} -` }).first();
  await expect(choisirApresActualisation).toBeVisible();
  await choisirApresActualisation.click();
  await expect(consentButton).toBeDisabled();
});
