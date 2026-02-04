# Frontend Component: Add Movement to Product

We need to create or update the component for adding movements to products.

If it's a new component, check what might be the best place to put it on the menu based on the context.
If you don't feel sure, ask the user by providing suggestions.

These are the details about the API(s) to call. Follow the current standards of the project and follow the best practices.

Check if the component already exists to update it.

## API Specification

**Base Path**: `/jbh-api/finance/products`
**Authentication**: JWT token required in Authorization header

### Operations to Implement

#### POST - Add Movement to Product

- **Endpoint**: `POST /jbh-api/finance/products/movements/{productId}`
- **Path Parameter**: `productId` (UUID) - The product ID to add the movement to
- **Request Body**:

```typescript
interface AddMovementRequest {
  entryDate: string;        // LocalDate format: "YYYY-MM-DD"
  totalAmount: number;      // BigDecimal - Required for DEPOSIT/WITHDRAWAL
  balanceSnapshot: number;  // BigDecimal - Required for BALANCE_SNAPSHOT
  movementType: MovementType;
  categoryName: string;     // Category enum name from categories API
}

type MovementType = 'DEPOSIT' | 'WITHDRAWAL' | 'BALANCE_SNAPSHOT';
```

- **Response**: `AddBasicMovementDTO`

```typescript
interface AddBasicMovementDTO {
  account: ProductDTO;
  movement: MovementDTO;
  monthlyBalance: MonthlyBalanceDTO;
}

interface MovementDTO {
  id: AccountMovementId;
  accountId: ProductId;
  movementType: MovementType;
  category: MovementCategoryDTO;
  movementAmount: number;    // BigDecimal
  movementDate: string;      // LocalDate: "YYYY-MM-DD"
  balanceSnapshot: number;   // BigDecimal
  metadata: AccountMovementMetadata;
  createdAt: string;         // ISO DateTime (LocalDateTime)
  description: string;
}

interface AccountMovementId {
  value: number; // Long
}

interface ProductId {
  value: string; // UUID
}

interface MovementCategoryDTO {
  source: CategorySource;
  typeName: string;
  translationsKey: string; // JSON: {"en":"English","es":"Spanish"}
}

type CategorySource = 'INCOME' | 'EXPENSE';

interface MonthlyBalanceDTO {
  id: number;                      // Long
  accountId: ProductId;
  year: number;
  month: number;
  period: string;                  // YearMonth format: "YYYY-MM"
  netGrowthRate: number;           // BigDecimal
  totalDebits: number;             // BigDecimal
  totalCredits: number;            // BigDecimal
  movementBalance: number;         // BigDecimal
  openingBalance: number;          // BigDecimal
  closingBalance: number;          // BigDecimal
  monthlyNetProfit: number;        // BigDecimal
  totalMovements: number;
  gapPeriod: boolean;
  officialMonthlyReport: boolean;
  monthlyReportedProfit: number;   // BigDecimal
  incomeWithholdingTaxAmount: number; // BigDecimal
  createdAt: string;               // ISO DateTime
  updatedAt: string;               // ISO DateTime
}

interface ProductDTO {
  id: ProductId;
  name: string;
  type: ProductType;
  userId: string;
  movementBalance: number;    // BigDecimal
  currentBalance: number;     // BigDecimal
  netProfitBalance: number;   // BigDecimal
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  netGrowthRate: number;      // BigDecimal
  metadata: ProductMetadata;
}
```

---

## Related APIs - Categories

The movement categories must be fetched from these catalog APIs:

#### GET - Income Categories (for DEPOSIT movements)

- **Endpoint**: `GET /jbh-api/finance/categories/incomes`
- **Response**: Array of `CategoryDTO`

```typescript
interface CategoryDTO {
  name: string;           // Enum value: "SALARY", "DIVIDENDS", etc.
  translationKey: string; // JSON: {"en":"...","es":"..."}
  source: CategorySource; // "INCOME"
}
```

#### GET - Expense Categories (for WITHDRAWAL movements)

- **Endpoint**: `GET /jbh-api/finance/categories/expenses`
- **Response**: Array of `CategoryDTO`

```typescript
interface CategoryDTO {
  name: string;           // Enum value: "PERSONAL", "TRANSFER", etc.
  translationKey: string; // JSON: {"en":"...","es":"..."}
  source: CategorySource; // "EXPENSE"
}
```

---

## Project Standards to Follow

### Authentication

- Send JWT token as it's currently working right now. (with interceptors)
- Handle responses as it's currently working right now (with interceptors)

### Form Implementation

- Follow existing form component patterns in this project
- Use the project's validation library for client-side validation
- Display validation errors inline below each field
- Show loading state during form submission
- Display success message after successful operation
- Display error message if operation fails
- Use decimal/Date/currency directives or pipes where they apply. (Based on the Response Type object)

### Directives or Pipes

- Always apply the directive `jbhDecimalFormat` for BigDecimal objects in the API's response.
- Always apply the `pipe` `jbhDate` for `Date` objects present in the API's response. Check the API response example to know the date format.
- Always apply the currency pipe `jbhCurrency` for currency values in the API's response.

### Multi-Platform Requirements

- Design must work on Desktop, Android, and iOS with ionic/capacitor, etc.
- Use responsive breakpoints
- Consider mobile keyboard behavior for forms

### Component Structure

#### Project Structure Overview

```diagram
src/app/
├── core/domain/          # Models, repositories, use-cases
├── core/infrastructure/  # API, persistence, implementations
├── features/             # Lazy-loaded feature modules (pages,components)
├── shared/               # Reusable components, pipes, directives,utilities
└── theme/                # custom-components.scss (single source)
```

### API Integration

- Create/Update the repository adapter implementing the current standard of the project.
- Keep the existing structure of the repository layer.

---

## Additional Notes

### Business Rules

1. **Product Type Restrictions**: Only certain product types allow adding movements:
   - SAVINGS
   - CREDIT_CARD
   - INVESTMENT
   - CDT

   Products of type LOAN and REAL_ESTATE_INVESTMENT do not allow manual movement additions.

2. **Movement Type Logic**:
   - `DEPOSIT`: Requires `totalAmount` (positive value), uses Income categories
   - `WITHDRAWAL`: Requires `totalAmount` (positive value - API handles sign), uses Expense categories
   - `BALANCE_SNAPSHOT`: Requires `balanceSnapshot`, no category needed

3. **Date Validation**: Entry date cannot be in the future

4. **Category Selection**:
   - When `movementType` is `DEPOSIT`, show only Income categories
   - When `movementType` is `WITHDRAWAL`, show only Expense categories
   - When `movementType` is `BALANCE_SNAPSHOT`, hide category field

### Form Field Visibility Matrix

| Field           | DEPOSIT | WITHDRAWAL | BALANCE_SNAPSHOT |
|-----------------|---------|------------|------------------|
| entryDate       | ✅      | ✅         | ✅               |
| totalAmount     | ✅      | ✅         | ❌               |
| balanceSnapshot | ❌      | ❌         | ✅               |
| categoryName    | ✅      | ✅         | ❌               |

### Validation Rules

| Field           | Rule                                | Error (EN)                         | Error (ES)                                    |
|-----------------|-------------------------------------|------------------------------------|-----------------------------------------------|
| entryDate       | Required                            | Entry date is required             | La fecha de entrada es requerida              |
| entryDate       | Must be <= today                    | Entry date cannot be in the future | La fecha de entrada no puede ser en el futuro |
| movementType    | Required                            | Movement type is required          | El tipo de movimiento es requerido            |
| totalAmount     | Required for DEPOSIT/WITHDRAWAL     | Amount is required                 | El monto es requerido                         |
| totalAmount     | Must be > 0                         | Amount must be greater than zero   | El monto debe ser mayor que cero              |
| balanceSnapshot | Required for BALANCE_SNAPSHOT       | Balance snapshot is required       | El saldo es requerido                         |
| balanceSnapshot | Must be >= 0                        | Balance cannot be negative         | El saldo no puede ser negativo                |
| categoryName    | Required for DEPOSIT/WITHDRAWAL     | Category is required               | La categoría es requerida                     |

### UI Labels

| Element         | English          | Spanish              |
|-----------------|------------------|----------------------|
| Page Title      | Add Movement     | Agregar Movimiento   |
| Entry Date      | Entry Date       | Fecha de Entrada     |
| Amount          | Amount           | Monto                |
| Balance         | Current Balance  | Saldo Actual         |
| Category        | Category         | Categoría            |
| Movement Type   | Movement Type    | Tipo de Movimiento   |
| Submit Button   | Add Movement     | Agregar Movimiento   |
| Cancel Button   | Cancel           | Cancelar             |

### Movement Type Options

| Value            | Label (EN)       | Label (ES)           | Color   |
|------------------|------------------|----------------------|---------|
| DEPOSIT          | Deposit          | Depósito             | Green   |
| WITHDRAWAL       | Withdrawal       | Retiro               | Red     |
| BALANCE_SNAPSHOT | Balance Update   | Actualizar Saldo     | Blue    |

### Response Handling

After successful submission, the response contains:
- `account`: Updated product with recalculated balances
- `movement`: The created movement details
- `monthlyBalance`: Updated monthly balance for the movement's period

Use this data to refresh local state or navigate back with updated information.

---

- Do not implement state management.
- The error handling is already implemented, Do not mention it.
- Do not suggest adding unit tests. The project does not support that.
- The application only supports EN and ES languages. Be aware about the translations with good grammar.

---

Feel free to ask any questions about the implementation, or if something needs to be confirmed before proceeding.
