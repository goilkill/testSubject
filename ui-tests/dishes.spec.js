const { test, expect } = require('@playwright/test');

const runId = `${Date.now()}_${Math.floor(Math.random() * 1e6)}`;
const productName = `pw${runId} продукт для блюда`;
const dishName = `pw${runId} Борщ Особый`;

let productId;
let dishId;

test.beforeAll(async ({ request }) => {
  const pr = await request.post('/api/products', {
    data: {
      name: productName,
      calories: 200,
      proteins: 10,
      fats: 5,
      carbohydrates: 20,
      composition: 'ui seed',
      category: 'GRAINS',
      cookingStatus: 'READY_TO_EAT',
      photos: [],
      flags: [],
    },
  });
  expect(pr.ok(), await pr.text()).toBeTruthy();
  const p = await pr.json();
  productId = p.id;

  const dr = await request.post('/api/dishes', {
    data: {
      name: dishName,
      category: 'SOUP',
      portionSize: 250,
      ingredients: [{ productId, quantity: 100 }],
      flags: [],
      photos: [],
    },
  });
  expect(dr.ok(), await dr.text()).toBeTruthy();
  const d = await dr.json();
  dishId = d.id;
});

test.afterAll(async ({ request }) => {
  if (dishId) await request.delete(`/api/dishes/${dishId}`);
  if (productId) await request.delete(`/api/products/${productId}`);
});

test.describe('Блюда: поиск по названию', () => {
  test.beforeEach(async ({ page }) => {
    await Promise.all([
      page.waitForResponse(
        (r) => r.url().includes('/api/dishes') && r.request().method() === 'GET' && r.ok()
      ),
      page.goto('/dishes.html'),
    ]);
  });

  const searchCases = [
    { title: 'ЭР: подстрока в смешанном регистре', query: 'бОрЩ', expectEmpty: false },
    { title: 'ЭР: подстрока в верхнем регистре', query: 'БОРЩ', expectEmpty: false },
    { title: 'ЭР: пустой запрос (весь список)', query: '', expectEmpty: false },
    { title: 'ЭР: несуществующая подстрока', query: `___no_dish_${runId}___`, expectEmpty: true },
  ];

  for (const c of searchCases) {
    test(c.title, async ({ page }) => {
      if (c.query === '') {
        await page.fill('#search', 'tmp');
        await Promise.all([
          page.waitForResponse(
            (r) => r.url().includes('/api/dishes') && r.request().method() === 'GET' && r.ok()
          ),
          page.fill('#search', ''),
        ]);
      } else {
        await Promise.all([
          page.waitForResponse(
            (r) => r.url().includes('/api/dishes') && r.request().method() === 'GET' && r.ok()
          ),
          page.fill('#search', c.query),
        ]);
      }
      if (c.expectEmpty) {
        await expect(page.locator('.empty')).toContainText('Блюда не найдены');
      } else {
        await expect(page.locator('.item-name', { hasText: dishName })).toBeVisible();
      }
    });
  }
});

test.describe('Блюда: модальное окно создания', () => {
  test.beforeEach(async ({ page }) => {
    await Promise.all([
      page.waitForResponse(
        (r) => r.url().includes('/api/dishes') && r.request().method() === 'GET' && r.ok()
      ),
      page.goto('/dishes.html'),
    ]);
    await page.getByRole('button', { name: '+ Добавить блюдо' }).click();
    await page.waitForSelector('#productSelect option', { state: 'attached' });
  });

  test('ЭР: сохранение без ингредиентов — ошибка в форме', async ({ page }) => {
    await page.fill('#fName', `pw${runId} без состава`);
    await page.selectOption('#fCategory', 'SOUP');
    await page.fill('#fPortionSize', '250');
    await page.locator('#modal .modal-actions button.btn-blue', { hasText: 'Сохранить' }).click();
    await expect(page.locator('#formError')).toBeVisible();
    await expect(page.locator('#formError')).toContainText('Добавьте хотя бы один ингредиент');
  });

  const qtyCases = [
    { title: 'количество ингредиента = 0', value: '0' },
    { title: 'количество ингредиента < 0', value: '-1' },
  ];

  for (const c of qtyCases) {
    test(c.title, async ({ page }) => {
      await page.fill('#ingredientQty', c.value);
      page.once('dialog', async (dialog) => {
        expect(dialog.message()).toMatch(/грамм/i);
        await dialog.dismiss();
      });
      await page.locator('.ingredient-add button.btn-green').click();
    });
  }
});
