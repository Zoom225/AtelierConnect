import { expect, test, type APIRequestContext } from '@playwright/test';

type ParticipantLogin = { id: number; matricule: string; token: string; categorie: string };
type Atelier = {
  id: number;
  typeAtelier: string;
  nbParticipantsActuels: number;
  accordOuverturePublique?: boolean | null;
  dateAccordOuverturePublique?: string | null;
};
type Reservation = { id: number; atelierId: number; statut: string; paiement?: { statut: string } };
type Salle = { id: number; nom: string; centreId: number };

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

async function loginParticipantInBrowser(page: import('@playwright/test').Page, matricule: string) {
  await page.goto('/member');
  await page.locator('input[formControlName="matricule"]').fill(matricule);
  await page.locator('input[formControlName="password"]').fill(demoPassword!);
  await page.locator('form button[type="submit"]').click();
  await expect(page.locator('input[formControlName="password"]')).toHaveCount(0);
}

test('participant suspendu : message immédiat, créations publique et privée bloquées dans Angular et l’API', async ({ page }) => {
  const participant = await login(page.request, 'L10002');
  const headers = bearer(participant.token);
  const penalty = await page.request.get(`/api/participants/${participant.id}/penalty`, { headers });
  expect(penalty.status()).toBe(200);
  expect(await penalty.json()).toBe(true);
  const before = await (await page.request.get(`/api/ateliers/organisateur/${participant.id}`, { headers })).json();
  const salles = await (await page.request.get('/api/salles')).json() as Salle[];
  expect(salles.length).toBeGreaterThan(0);

  await loginParticipantInBrowser(page, 'L10002');
  await expect(page).toHaveURL(/\/member\/profile$/, { timeout: 20_000 });
  await expect(page.getByRole('alert')).toContainText('suspension active', { timeout: 20_000 });
  await expect(page.getByRole('alert')).toContainText('publics et privés');
  await expect(page.getByTestId('member-debt')).toContainText('Dette à régler');
  await expect(page.getByTestId('member-debt')).toContainText(/90,00\s*€/);
  const shortcuts = page.locator('a[aria-disabled="true"]');
  await expect(shortcuts).toHaveCount(2);
  await expect(shortcuts.nth(0)).not.toHaveAttribute('href');
  await expect(shortcuts.nth(1)).not.toHaveAttribute('href');

  let creationRequests = 0;
  page.on('request', request => {
    if (request.method() === 'POST' && new URL(request.url()).pathname === '/api/ateliers') creationRequests++;
  });
  for (const type of ['PUBLIC', 'PRIVE']) {
    // Même en ouvrant directement le formulaire, la suspension reste bloquante.
    await page.goto(`/member/ateliers/nouveau?type=${type}`);
    await expect(page.getByRole('alert')).toContainText('suspension active', { timeout: 20_000 });
    await expect(page.getByRole('alert')).toContainText('date de fin incluse');
    await expect(page.locator('button[type="submit"]')).toBeDisabled();
    await expect(page.locator('input[formControlName="date"]')).toBeDisabled();
    // Le contrôle serveur reste obligatoire si le navigateur est contourné.
    const refused = await page.request.post('/api/ateliers', { headers, data: {
      salleId: salles[0].id, atelierDate: dateDansSixJours(), atelierType: type
    } });
    expect(refused.status()).toBe(400);
    // Ce compte de démonstration cumule dette et suspension : le backend
    // contrôle d'abord le solde impayé, les deux motifs restent bloquants.
    expect((await refused.json()).message).toBe("Vous avez un solde impayé et ne pouvez pas créer d'atelier.");
  }
  expect(creationRequests).toBe(0);
  const after = await (await page.request.get(`/api/ateliers/organisateur/${participant.id}`, { headers })).json();
  expect(after).toEqual(before);
});

test('règlement autonome : dette soldée une seule fois, suspension maintenue', async ({ page }) => {
  const participant = await login(page.request, 'L10002');
  await loginParticipantInBrowser(page, 'L10002');
  await expect(page).toHaveURL(/\/member\/profile$/, { timeout: 20_000 });
  await page.goto('/member/payments');
  const panel = page.getByTestId('debt-payment-panel');
  await expect(panel).toContainText(/90,00\s*€/, { timeout: 20_000 });
  const completed = page.waitForResponse(response => new URL(response.url()).pathname === '/api/paiements/dette/pay');
  await page.getByTestId('pay-debt').click();
  const response = await completed;
  expect(response.status()).toBe(200);
  const requestData = response.request().postDataJSON();
  const result = await response.json();
  expect(result.solde).toBe(0);
  expect(result.suspensionActive).toBe(true);
  await expect(page.getByTestId('pay-debt')).toHaveCount(0, { timeout: 20_000 });
  await expect(panel).toContainText('Aucune dette à régler');
  const replay = await page.request.post('/api/paiements/dette/pay', {
    headers: bearer(participant.token), data: requestData
  });
  expect(replay.status()).toBe(200);
  const history = await replay.json();
  expect(history.solde).toBe(0);
  expect(history.mouvements.filter((movement: any) => movement.nature === 'REGLEMENT_DETTE')).toHaveLength(1);
  await page.goto('/member/ateliers/nouveau?type=PUBLIC');
  await expect(page.getByRole('alert')).toContainText('suspension active', { timeout: 20_000 });
  await expect(page.locator('button[type="submit"]')).toBeDisabled();
});

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
  expect(atelier.typeAtelier).toBe('PRIVE');
  expect(atelier.accordOuverturePublique).not.toBe(true);

  // Un participant différent ne peut pas gérer les ateliers de l'organisateur.
  const autreOrganisateur = await page.request.get(
    `/api/ateliers/organisateur/${organisateur.id}`, { headers: bearer(invite.token) }
  );
  expect([400, 403]).toContain(autreOrganisateur.status());

  const invitation = await page.request.post('/api/reservations/private-invites', {
    headers: bearer(organisateur.token),
    data: { atelierId: atelier.id, inviteeMatricules: ['S10001'] }
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
  const reservationOrganisateur = liste.find(r => r.atelierId === atelier.id);
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
  expect((await confirmation.json() as Atelier).nbParticipantsActuels).toBe(2);

  // Une annulation après paiement doit libérer une place et enregistrer
  // un remboursement dans le système (sans prestataire bancaire).
  const annulationInvite = await page.request.patch(
    `/api/reservations/${reservationsInvite[0].id}/cancel`,
    { headers: bearer(invite.token), data: {} }
  );
  expect(annulationInvite.status(), await annulationInvite.text()).toBe(204);

  const reservationsApresAnnulation = await page.request.get(
    `/api/reservations/participant/${invite.id}`,
    { headers: bearer(invite.token) }
  );
  expect(reservationsApresAnnulation.status()).toBe(200);
  const annulee = (await reservationsApresAnnulation.json() as Reservation[])
    .find(r => r.id === reservationsInvite[0].id);
  expect(annulee?.statut).toBe('ANNULEE');
  expect(annulee?.paiement?.statut).toBe('REMBOURSE');

  const detailApresAnnulation = await page.request.get(
    `/api/ateliers/${atelier.id}`,
    { headers: bearer(organisateur.token) }
  );
  expect(detailApresAnnulation.status()).toBe(200);
  expect((await detailApresAnnulation.json() as Atelier).nbParticipantsActuels).toBe(1);

  const secondeAnnulation = await page.request.patch(
    `/api/reservations/${reservationsInvite[0].id}/cancel`,
    { headers: bearer(invite.token), data: {} }
  );
  expect([400, 409]).toContain(secondeAnnulation.status());

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


test('administrateur global : créer, modifier et supprimer un centre dans Angular', async ({ page }) => {
  test.setTimeout(120_000);
  if (!adminPassword) throw new Error('APP_DEMO_ADMIN_PASSWORD manquant pour les tests CI');

  await page.goto('/admin/login');
  await page.locator('input[formControlName="email"]').fill('admin@atelierconnect.example');
  await page.locator('input[formControlName="password"]').fill(adminPassword);
  await page.locator('form button[type="submit"]').click();
  await expect(page).toHaveURL(/\/admin\/?$/, { timeout: 20_000 });

  await page.goto('/admin/centres');
  await expect(page.locator('h1.adm-site-title')).toBeVisible();

  const nom = 'Centre test E2E ' + Date.now();
  await page.locator('input[formControlName="nom"]').fill(nom);
  await page.locator('input[formControlName="adresse"]').fill('10 rue du Test, Bruxelles');
  await page.locator('input[formControlName="ouvertVisiteurs"]').check();
  await page.locator('form button[type="submit"]').click();

  const fiche = page.locator('.adm-site-card').filter({ hasText: nom });
  await expect(fiche).toBeVisible({ timeout: 15_000 });

  // Confirmer la persistance réelle côté Spring/PostgreSQL, et pas seulement le DOM.
  const reponseCreation = await page.request.get('/api/centres');
  expect(reponseCreation.status()).toBe(200);
  const centres = await reponseCreation.json() as Array<{
    id: number;
    nom: string;
    adresse: string;
    ouvertVisiteurs: boolean;
    dureeAtelierMinutes: number;
    dureeEntreAtelierMinutes: number;
  }>;
  const centre = centres.find(item => item.nom === nom);
  expect(centre).toBeDefined();
  expect(centre!.ouvertVisiteurs).toBe(true);
  expect(centre!.dureeAtelierMinutes).toBe(120);
  expect(centre!.dureeEntreAtelierMinutes).toBe(20);

  await fiche.locator('button.adm-site-action-edit').click();
  await expect(page.locator('input[formControlName="nom"]')).toHaveValue(nom);
  await page.locator('input[formControlName="adresse"]').fill('12 rue du Test, Bruxelles');
  await page.locator('form button[type="submit"]').click();
  await expect(fiche).toContainText('12 rue du Test, Bruxelles', { timeout: 15_000 });

  // Suppression réservée au centre créé par ce test sur une base CI jetable.
  page.once('dialog', dialog => dialog.accept());
  await fiche.locator('button.adm-site-action-delete').click();
  await expect(fiche).toHaveCount(0, { timeout: 15_000 });
  const reponseSuppression = await page.request.get('/api/centres/' + centre!.id);
  expect(reponseSuppression.status()).toBe(404);
});

test('création Angular, paiement et annulation visibles et persistants', async ({ page }) => {
  await loginParticipantInBrowser(page, 'G1002');
  await page.goto('/member/ateliers/nouveau');
  await page.locator('mat-select[formControlName="centreId"]').click();
  await page.getByRole('option').filter({ hasText: 'Bruxelles' }).click();
  // La première salle est sélectionnée après la réponse HTTP du centre.
  // Attendre cette sélection évite d'ouvrir un menu encore en chargement.
  await expect(page.locator('mat-select[formControlName="salleId"]'))
    .toContainText('Salle Création A', { timeout: 20_000 });
  const date = new Date();
  date.setDate(date.getDate() + 5);
  await page.locator('input[formControlName="date"]').fill(date.toISOString().slice(0, 10));
  await page.locator('input[formControlName="heureDebut"]').fill('12:00');
  const creationResponse = page.waitForResponse(response =>
    response.url().endsWith('/api/ateliers') && response.request().method() === 'POST');
  await page.locator('form button[type="submit"]').click();
  const creation = await creationResponse;
  expect(creation.status(), await creation.text()).toBe(201);
  const atelier = await creation.json() as Atelier;
  expect(atelier.typeAtelier).toBe('PUBLIC');
  expect(atelier.nbParticipantsActuels).toBe(0);
  await expect(page).toHaveURL(/\/member\/reservations/);

  const organiser = await login(page.request, 'G1002');
  const reservations = await page.request.get(`/api/reservations/participant/${organiser.id}`,
    { headers: bearer(organiser.token) });
  const reservation = (await reservations.json() as Reservation[]).find(item => item.atelierId === atelier.id)!;
  expect(reservation.statut).toBe('EN_ATTENTE');
  const card = page.locator('mat-card').filter({
    has: page.locator('mat-card-title').filter({ hasText: new RegExp(`#${reservation.id}$`) })
  });
  const paid = page.waitForResponse(response => response.url().endsWith(`/reservation/${reservation.id}/pay`));
  await card.getByRole('button', { name: /Payer/ }).click();
  expect((await paid).status()).toBe(200);
  await expect(card.getByRole('button', { name: /Payer/ })).toBeDisabled();
  const cancelled = page.waitForResponse(response => response.url().endsWith(`/reservations/${reservation.id}/cancel`));
  await card.getByRole('button', { name: /Annuler/ }).click();
  expect((await cancelled).status()).toBe(204);
  await page.reload();
  await expect(card.getByRole('button', { name: /Annuler/ })).toBeDisabled();
  const after = await page.request.get(`/api/reservations/${reservation.id}`, { headers: bearer(organiser.token) });
  const persisted = await after.json() as Reservation;
  expect(persisted.statut).toBe('ANNULEE');
  expect(persisted.paiement?.statut).toBe('REMBOURSE');

  const visitor = await login(page.request, 'L10001');
  const joined = await page.request.post('/api/reservations', { headers: bearer(visitor.token),
    data: { atelierId: atelier.id, participantId: visitor.id, requesterId: visitor.id } });
  expect(joined.status(), await joined.text()).toBe(201);
  const duplicate = await page.request.post('/api/reservations', { headers: bearer(visitor.token),
    data: { atelierId: atelier.id, participantId: visitor.id, requesterId: visitor.id } });
  expect(duplicate.status()).toBe(400);
  expect((await duplicate.json()).message).toContain('déjà inscrit');
});

test('salles administrables, Swagger et refus HTTP réels', async ({ page }) => {
  await page.goto('/admin/login');
  await page.locator('input[formControlName="email"]').fill('admin@atelierconnect.example');
  await page.locator('input[formControlName="password"]').fill(adminPassword!);
  await page.locator('form button[type="submit"]').click();
  await expect(page).toHaveURL(/\/admin\/?$/, { timeout: 20_000 });
  await page.goto('/admin/salles');
  // Les cartes sont rendues après le chargement conjoint des salles et centres.
  await expect(page.locator('.adm-ter-card').first()).toBeVisible({ timeout: 20_000 });
  const name = 'Salle validation ' + Date.now();
  await page.locator('input[formControlName="nom"]').fill(name);
  await page.locator('mat-select[formControlName="centreId"]').click();
  await page.getByRole('option').filter({ hasText: 'Bruxelles' }).click();
  const creationResponse = page.waitForResponse(response => new URL(response.url()).pathname === '/api/salles' && response.request().method() === 'POST');
  await page.locator('form button[type="submit"]').click();
  expect((await creationResponse).status()).toBe(201);
  const card = page.locator('.adm-ter-card').filter({ hasText: name });
  await expect(card).toBeVisible({ timeout: 20_000 });
  await card.locator('button.adm-ter-action-edit').click();
  await page.locator('input[formControlName="nom"]').fill(name + ' modifiée');
  const updateResponse = page.waitForResponse(response => new URL(response.url()).pathname.startsWith('/api/salles/') && response.request().method() === 'PUT');
  await page.locator('form button[type="submit"]').click();
  expect((await updateResponse).status()).toBe(200);
  await expect(card).toContainText(name + ' modifiée', { timeout: 20_000 });
  const salles = await (await page.request.get('/api/salles')).json() as Salle[];
  const salle = salles.find(item => item.nom === name + ' modifiée')!;
  expect(salle).toBeDefined();
  page.once('dialog', dialog => dialog.accept());
  const deletionResponse = page.waitForResponse(response => new URL(response.url()).pathname === `/api/salles/${salle.id}` && response.request().method() === 'DELETE');
  await card.locator('button.adm-ter-action-delete').click();
  expect((await deletionResponse).status()).toBe(204);
  await expect(card).toHaveCount(0, { timeout: 20_000 });
  expect((await page.request.get(`/api/salles/${salle.id}`)).status()).toBe(404);

  expect((await page.request.get('/swagger-ui.html')).status()).toBe(200);
  expect((await page.request.get('/v3/api-docs')).status()).toBe(200);
  expect((await page.request.post('/api/salles', { data: { nom: 'Interdit', centreId: salle.centreId } })).status()).toBe(403);
  const invalidLogin = await page.request.post('/api/participants/login',
    { data: { matricule: 'INEXISTANT', password: 'incorrect' } });
  expect(invalidLogin.status()).toBe(400);
  expect((await invalidLogin.json()).message).toBe('Identifiants invalides.');
  const preflight = await page.request.fetch('/api/reservations', { method: 'OPTIONS', headers: {
    Origin: 'http://localhost:4200', 'Access-Control-Request-Method': 'POST',
    'Access-Control-Request-Headers': 'authorization,content-type'
  } });
  expect(preflight.status()).toBe(200);
  expect(preflight.headers()['access-control-allow-origin']).toBeTruthy();
});
