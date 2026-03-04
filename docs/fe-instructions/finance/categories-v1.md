# Frontend Component: Categories

We need to create or update the component(s) for **Categories**.

Categories are a read-only catalog used across the app (e.g., to tag movements with income or expense types). Check where these are currently surfaced in the navigation — they may be embedded within a settings screen, a catalog section, or reused as a selector in forms. If unsure, ask the user with suggestions.

Check if a category-related component already exists before creating anything new.

---

## API Specification

**Base Path**: `/jbh-api/finance/categories`
**Authentication**: JWT token required (handled by interceptors)
**HTTP Methods**: `GET` only — this is a read-only catalog.

---

### Response Model

All three endpoints return the same shape:

```typescript
interface CategoryApiResponse {
  id: number;
  source: 'INCOME' | 'EXPENSE';
  alias: string;           // Internal short key, e.g. "FOOD", "SALARY"
  active: boolean;
  displayName: Record<string, string>; // Locale map, e.g. { en: 'Food', es: 'Comida' }
}
```

---

### Endpoints

#### 1. GET Categories by Source (generic)

- **Endpoint**: `GET /jbh-api/finance/categories?source={source}`
- **Query Param**: `source` — required, values: `INCOME` or `EXPENSE`
- **Response**: `CategoryApiResponse[]`

#### 2. GET All Expense Categories

- **Endpoint**: `GET /jbh-api/finance/categories/expenses`
- **Response**: `CategoryApiResponse[]`

#### 3. GET All Income Categories

- **Endpoint**: `GET /jbh-api/finance/categories/incomes`
- **Response**: `CategoryApiResponse[]`

---

## Implementation Instructions

### Repository Layer

Create or update the category repository adapter following the existing repository patterns.

```typescript
interface CategoryRepository {
  findBySource(source: 'INCOME' | 'EXPENSE'): Promise<CategoryApiResponse[]>;
  findAllExpenses(): Promise<CategoryApiResponse[]>;
  findAllIncomes(): Promise<CategoryApiResponse[]>;
}
```

- Check `core/domain/` and `core/infrastructure/` for an existing `CategoryApiResponse` model. Reuse it if it exists.
- If not found, create it with the name `CategoryApiResponse` following the project naming standard.

### Display Logic

- Use `displayName['en']` or `displayName['es']` depending on the active app locale.
- Fall back to `alias` if the locale key is missing in `displayName`.
- Only show categories where `active === true`.
- Use `source` to group or filter between `INCOME` and `EXPENSE` categories.

### Component Behavior

- Show income and expense categories as two separate sections or tabs.
- Show an empty state when no categories are returned.
- No create, edit, or delete actions — this is read-only.

### Pipes and Directives

- No currency, decimal, or date formatting is needed for this API.
- Do **not** apply directives to display elements. Only use pipes (`|`) in templates.

### Multi-Platform

- Must work on Desktop, Android, and iOS.
- Use responsive Ionic layout with proper breakpoints.
- Register any Ionic icons used in the component.

### Translations

- Support `EN` and `ES` only.
- The `displayName` map already provides translated labels — use them for user-facing text.
- Add any static UI labels (headings, tabs, empty state) to both `en.json` and `es.json`.

---

## Additional Notes

- Only `active === true` categories should be shown in selectors and forms throughout the app.
- `alias` is an internal identifier — prefer `displayName` for all user-facing labels.
- Do not implement state management.
- Do not forget to register the Ionic icons used by the HTML.
- Error handling is already implemented globally — do not add custom error handling.
- Do not suggest adding unit tests.
- The application only supports EN and ES. Use proper grammar in translations.

---

Feel free to ask any questions or flag anything that needs to be confirmed before proceeding — for example, where in the navigation this component should live, or whether categories are displayed as a standalone page vs. an inline selector inside movement or product forms.
