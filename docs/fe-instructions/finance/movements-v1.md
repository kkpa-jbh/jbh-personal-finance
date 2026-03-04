# Frontend Component: Movements

We need to create or update the component(s) for **Movements** — financial transactions (deposits, withdrawals, balance snapshots) linked to a product.

Check if a movements component already exists before creating anything new. If it's a new component, check the current navigation and suggest the best location to the user.

---

## API Specification

**Base Path**: `/jbh-api/finance/products/movements`
**Authentication**: JWT token required (handled by interceptors)

---

## Models

### Request — Add Movement

```typescript
interface CategoryTypeApiRequest {
  source: 'INCOME' | 'EXPENSE';
  alias: string; // Internal category alias, e.g. "FOOD", "SALARY"
}

interface CategoryApiRequest {
  categoryId: number;
  categoryType: CategoryTypeApiRequest;
}

interface AddMovementApiRequest {
  entryDate: string;               // ISO date string, e.g. "2026-03-04"
  totalAmount: number | null;      // BigDecimal — null when movementType is BALANCE_SNAPSHOT
  balanceSnapshot: number | null;  // BigDecimal — required when movementType is BALANCE_SNAPSHOT
  movementType: 'DEPOSIT' | 'WITHDRAWAL' | 'BALANCE_SNAPSHOT';
  categoryRequest: CategoryApiRequest | null; // Must be null for BALANCE_SNAPSHOT
  description: string | null;
}
```

### Response — Single Movement

```typescript
interface CategoryTypeApiResponse {
  source: 'INCOME' | 'EXPENSE';
  alias: string;
}

interface MovementApiResponse {
  id: { value: string };              // UUID wrapper
  productId: { value: string };       // UUID wrapper
  movementType: 'DEPOSIT' | 'WITHDRAWAL' | 'BALANCE_SNAPSHOT';
  category: CategoryTypeApiResponse | null; // null for BALANCE_SNAPSHOT
  movementAmount: number;             // BigDecimal
  movementDate: string;               // ISO date string (LocalDate)
  balanceSnapshot: number | null;     // BigDecimal | null
  metadata: Record<string, any>;      // MovementMetadata key-value map
  createdAt: string;                  // ISO datetime string (LocalDateTime)
  description: string | null;
  canBeRemoved: boolean;
}
```

### Response — Add Movement (POST)

The POST response wraps three objects together:

```typescript
interface ProductApiResponse {
  id: { value: string };
  name: string;
  type: string;                        // ProductType enum value
  userId: string;                      // UUID
  movementBalance: number;             // BigDecimal
  currentBalance: number;              // BigDecimal
  netProfitBalance: number;            // BigDecimal
  isActive: boolean;
  createdAt: string;                   // ISO datetime
  updatedAt: string;                   // ISO datetime
  netGrowthRate: number;               // BigDecimal
  metadata: Record<string, any>;
}

interface MonthlyBalanceApiResponse {
  id: number;
  productId: { value: string };
  year: number;
  month: number;
  period: string;                        // YearMonth, e.g. "2026-03"
  netGrowthRate: number;                 // BigDecimal
  totalDebits: number;                   // BigDecimal
  totalCredits: number;                  // BigDecimal
  movementBalance: number;               // BigDecimal
  openingBalance: number;                // BigDecimal
  closingBalance: number;                // BigDecimal
  monthlyNetProfit: number;              // BigDecimal
  totalMovements: number;
  gapPeriod: boolean;
  officialMonthlyReport: boolean;
  monthlyReportedProfit: number;         // BigDecimal
  incomeWithholdingTaxAmount: number;    // BigDecimal
  createdAt: string;                     // ISO datetime
  updatedAt: string;                     // ISO datetime
}

interface AddMovementApiResponse {
  productresponse: ProductApiResponse;
  movement: MovementApiResponse;
  monthlyBalance: MonthlyBalanceApiResponse;
}
```

---

## Endpoints

### POST — Add Movement to a Product

- **Endpoint**: `POST /jbh-api/finance/products/movements/{productId}`
- **Path Param**: `productId` — UUID of the product
- **Request Body**: `AddMovementApiRequest`
- **Response**: `AddMovementApiResponse`

### GET — Get Movements for a Product

- **Endpoint**: `GET /jbh-api/finance/products/movements/{productId}`
- **Path Param**: `productId` — UUID of the product
- **Response**: `MovementApiResponse[]`
- **Note**: The backend returns movements from the last 3 months (inclusive) by default — no date filter params are needed.

---

## Implementation Instructions

### Repository Layer

Create or update the movements repository adapter following the existing project patterns.

```typescript
interface MovementRepository {
  addMovement(productId: string, request: AddMovementApiRequest): Promise<AddMovementApiResponse>;
  getMovementsByProduct(productId: string): Promise<MovementApiResponse[]>;
}
```

- Check `core/domain/` and `core/infrastructure/` for existing models:
  - `MovementApiResponse`, `AddMovementApiResponse`, `AddMovementApiRequest`
  - `CategoryTypeApiResponse`, `CategoryApiRequest`
  - `ProductApiResponse`, `MonthlyBalanceApiResponse`
- Reuse any existing models. If not found, create them following the `ApiResponse` / `ApiRequest` naming convention.

### Category Integration

- When the user selects a category for a movement, the frontend must send:
  - `categoryRequest.categoryId` — the `id` field from the `CategoryApiResponse`
  - `categoryRequest.categoryType.source` — the `source` field (`INCOME` or `EXPENSE`)
  - `categoryRequest.categoryType.alias` — the `alias` field

- **Load categories** using the Categories API before opening the Add Movement form:
  - For **DEPOSIT** movements: load from `GET /jbh-api/finance/categories/incomes`
  - For **WITHDRAWAL** movements: load from `GET /jbh-api/finance/categories/expenses`
  - For **BALANCE_SNAPSHOT** movements: do **not** show a category selector — `categoryRequest` must be `null`

### Business Rules

- `BALANCE_SNAPSHOT` movements:
  - Must NOT include a `categoryRequest` (send `null`)
  - Use `balanceSnapshot` field to carry the value; `totalAmount` should be `null`
- `DEPOSIT` movements: `totalAmount` must be a positive value
- `WITHDRAWAL` movements: `totalAmount` must be a negative value
- `entryDate` must be a valid ISO date string (`YYYY-MM-DD`)

### List View

- Display movements for the selected product (last 3 months)
- Show: date, movement type, category alias/displayName (if available), amount, description
- Apply `jbhCurrency` pipe to all amount fields: `movementAmount`, `balanceSnapshot`
- Apply `jbhDate` pipe to `movementDate` and `createdAt`
- Use `canBeRemoved` to conditionally show a remove/delete action per movement row

### Add Movement Form

- Fields:
  - `entryDate` — date picker
  - `movementType` — selector: `DEPOSIT`, `WITHDRAWAL`, `BALANCE_SNAPSHOT`
  - `totalAmount` — decimal input (show when type is `DEPOSIT` or `WITHDRAWAL`) — use `jbhDecimalFormat` directive on the `<input>`
  - `balanceSnapshot` — decimal input (show only when type is `BALANCE_SNAPSHOT`) — use `jbhDecimalFormat` directive on the `<input>`
  - `categoryRequest` — category selector (hide when type is `BALANCE_SNAPSHOT`); dynamically load income/expense categories based on selected `movementType`
  - `description` — optional text input

- After a successful POST, update the displayed product balance and movement list with data from `AddMovementApiResponse`

### Pipes and Directives

```
<!-- BigDecimal display values -->
<span>{{ movement.movementAmount | jbhCurrency }}</span>
<span>{{ movement.balanceSnapshot | jbhCurrency }}</span>

<!-- Date display -->
<span>{{ movement.movementDate | jbhDate }}</span>
<span>{{ movement.createdAt | jbhDate }}</span>

<!-- Form inputs (BigDecimal) -->
<input [jbhDecimalFormat]="2" formControlName="totalAmount" />
<input [jbhDecimalFormat]="2" formControlName="balanceSnapshot" />
```

### Multi-Platform

- Must work on Desktop, Android, and iOS.
- Use responsive Ionic layout with proper breakpoints.
- Consider mobile keyboard behavior for decimal and date inputs.
- Register any Ionic icons used in the component.

### Translations

- Support `EN` and `ES` only.
- Add all static labels (headings, movement type labels, empty states, form labels, buttons) to both `en.json` and `es.json`.
- For category display names, use `displayName['en']` or `displayName['es']` from the category response (fall back to `alias`).

---

## Additional Notes

- The movements list is scoped to the last 3 months — inform the user if this is visible in the UI.
- `canBeRemoved` controls delete eligibility per movement — only show delete when this is `true`.
- Do not implement state management.
- Do not forget to register Ionic icons used by the HTML.
- Error handling is already implemented globally — do not add custom error handling.
- Do not suggest adding unit tests.
- The application only supports EN and ES. Use proper grammar in translations.

---

Feel free to ask any questions or flag anything that needs to be confirmed before proceeding — for example, how the category selector should be presented (modal, dropdown, segmented list), or whether the movement form should be a modal or a separate page.
