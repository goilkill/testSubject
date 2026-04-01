const { test, expect } = require('@playwright/test');

const runId = `${Date.now()}_${Math.floor(Math.random() * 1e6)}`;
const productName = `pw${runId} Слоёный творожок`;
let productId;

test.beforeAll(async ({ request }) => {
  const pr = await request.post('/api/products', {
    data: {
      name: productName,
      calories: 120,
      proteins: 6,
      fats: 7,
      carbohydrates: 2,
      composition: 'ui seed',
      category: 'SWEETS',
      cookingStatus: 'READY_TO_EAT',
      photos: [],
      flags: [],
    },
  });
  expect(pr.ok(), await pr.text()).toBeTruthy();
  productId = (await pr.json()).id;
});

test.afterAll(async ({ request }) => {
  if (productId) await request.delete(`/api/products/${productId}`);
});

test.describe('Продукты: поиск по названию', () => {
  test.beforeEach(async ({ page }) => {
    await Promise.all([
      page.waitForResponse(
        (r) => r.url().includes('/api/products') && r.request().method() === 'GET' && r.ok()
      ),
      page.goto('/products.html'),
    ]);
  });

  const searchCases = [
    { title: 'ЭП: подстрока в верхнем регистре', query: 'ТВОРОЖОК', expectEmpty: false },
    { title: 'ЭП: пустой запрос', query: '', expectEmpty: false },
    { title: 'ЭП: несуществующий продукт', query: `___no_prod_${runId}___`, expectEmpty: true },
  ];

  for (const c of searchCases) {
    test(c.title, async ({ page }) => {
      if (c.query === '') {
        await page.fill('#search', 'tmp');
        await Promise.all([
          page.waitForResponse(
            (r) => r.url().includes('/api/products') && r.request().method() === 'GET' && r.ok()
          ),
          page.fill('#search', ''),
        ]);
      } else {
        await Promise.all([
          page.waitForResponse(
            (r) => r.url().includes('/api/products') && r.request().method() === 'GET' && r.ok()
          ),
          page.fill('#search', c.query),
        ]);
      }
      if (c.expectEmpty) {
        await expect(page.locator('.empty')).toContainText('Продукты не найдены');
      } else {
        await expect(page.locator('.item-name', { hasText: productName })).toBeVisible();
      }
    });
  }
});

test.describe('Продукты: валидация названия при создании', () => {
  test.beforeEach(async ({ page }) => {
    await Promise.all([
      page.waitForResponse(
        (r) => r.url().includes('/api/products') && r.request().method() === 'GET' && r.ok()
      ),
      page.goto('/products.html'),
    ]);
    await page.getByRole('button', { name: '+ Добавить продукт' }).click();
    await expect(page.locator('#modal')).toBeVisible();
  });

  test('имя из 1 символа — ошибка', async ({ page }) => {
    await page.fill('#fName', 'A');
    await page.selectOption('#fCategory', 'VEGETABLES');
    await page.selectOption('#fCooking', 'READY_TO_EAT');
    await page.fill('#fCalories', '10');
    await page.fill('#fProteins', '1');
    await page.fill('#fFats', '1');
    await page.fill('#fCarbs', '1');
    await page.locator('#modal .modal-actions button.btn-green', { hasText: 'Сохранить' }).click();
    await expect(page.locator('#formError')).toBeVisible();
  });

  test('имя из 2 символов — успешное создание', async ({ page }) => {
    const finalName = `Z${String(runId).slice(-1)}`;
    expect(finalName.length).toBe(2);

    await page.fill('#fName', finalName);
    await page.selectOption('#fCategory', 'VEGETABLES');
    await page.selectOption('#fCooking', 'READY_TO_EAT');
    await page.fill('#fCalories', '10');
    await page.fill('#fProteins', '1');
    await page.fill('#fFats', '1');
    await page.fill('#fCarbs', '1');
    await page.locator('#modal .modal-actions button.btn-green', { hasText: 'Сохранить' }).click();
    await expect(page.locator('#modal')).toBeHidden();

    await Promise.all([
      page.waitForResponse(
        (r) => r.url().includes('/api/products') && r.request().method() === 'GET' && r.ok()
      ),
      page.fill('#search', finalName),
    ]);
    await expect(page.locator('.item-name', { hasText: finalName })).toBeVisible();

    const listRes = await page.request.get(
      `/api/products?name=${encodeURIComponent(finalName)}`
    );
    const arr = await listRes.json();
    const created = arr.find((x) => x.name === finalName);
    expect(created).toBeTruthy();
    await page.request.delete(`/api/products/${created.id}`);
  });
});
