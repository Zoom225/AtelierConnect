import { expect, test } from '@playwright/test';

test('accueil FR/EN et mobile : vocabulaire, langue persistante et absence de débordement', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'AtelierConnect', exact: true })).toBeVisible();
  await expect(page.locator('html')).toHaveAttribute('lang', 'fr');
  await expect(page.locator('body')).not.toContainText(/\b(padel|joueurs?|terrains?|courts?|matches)\b/i);
  const desktop = test.info().outputPath('accueil-fr-desktop.png');
  await page.screenshot({ path: desktop, fullPage: true });
  await test.info().attach('Accueil FR desktop', { path: desktop, contentType: 'image/png' });
  await page.locator('.lang-trigger').click();
  await page.locator('.lang-option').nth(1).click();
  await expect(page.locator('html')).toHaveAttribute('lang', 'en');
  await page.reload();
  await expect(page.locator('html')).toHaveAttribute('lang', 'en');
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.getByRole('heading', { name: 'AtelierConnect', exact: true })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await expect(page.locator('body')).not.toContainText(/\b(padel|joueurs?|terrains?|courts?|matches)\b/i);
  const mobile = test.info().outputPath('accueil-en-mobile.png');
  await page.screenshot({ path: mobile, fullPage: true });
  await test.info().attach('Accueil EN mobile', { path: mobile, contentType: 'image/png' });
});

test('formulaire participant utilisable au clavier sans afficher de secret', async ({ page }) => {
  await page.goto('/member');
  await expect(page.locator('input[formControlName="matricule"]')).toBeVisible();
  let found = false;
  for (let step = 0; step < 30; step++) {
    await page.keyboard.press('Tab');
    if (await page.locator('input[formControlName="matricule"]').evaluate(input => input === document.activeElement)) {
      found = true; break;
    }
  }
  expect(found).toBe(true);
  await page.keyboard.press('Tab');
  await expect(page.locator('input[formControlName="password"]')).toBeFocused();
  const screenshot = test.info().outputPath('connexion-clavier.png');
  await page.screenshot({ path: screenshot, fullPage: true });
  await test.info().attach('Connexion clavier sans identifiants', { path: screenshot, contentType: 'image/png' });
});
