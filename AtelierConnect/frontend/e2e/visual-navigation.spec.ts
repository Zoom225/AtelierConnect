import { expect, test } from '@playwright/test';

test('navigation mobile : le menu ouvre les liens, se ferme après navigation et avec Échap', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/');
  const toggle = page.getByRole('button', { name: 'Ouvrir ou fermer le menu' });
  const navigation = page.getByRole('navigation', { name: 'Navigation principale' });
  await expect(toggle).toHaveAttribute('aria-expanded', 'false');
  await toggle.click();
  await expect(navigation).toBeVisible();
  await navigation.getByRole('link', { name: 'Espace participant' }).click();
  await expect(page).toHaveURL(/\/member$/);
  await expect(toggle).toHaveAttribute('aria-expanded', 'false');
  await toggle.click();
  await navigation.getByRole('link', { name: 'Accueil' }).focus();
  await page.keyboard.press('Escape');
  await expect(toggle).toHaveAttribute('aria-expanded', 'false');
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});

test('accueil : les trois photographies locales sont disponibles et décrites', async ({ page }) => {
  await page.goto('/');
  for (const name of ['atelier-hero.jpg', 'atelier-watercolor.jpg', 'atelier-studio.jpg']) {
    const photograph = page.locator(`img[src="/images/${name}"]`).first();
    await photograph.scrollIntoViewIfNeeded();
    await expect(photograph).toHaveAttribute('alt', /\S+/);
    await expect
      .poll(() =>
        photograph.evaluate((img: HTMLImageElement) => img.complete && img.naturalWidth > 0),
      )
      .toBe(true);
  }
});
