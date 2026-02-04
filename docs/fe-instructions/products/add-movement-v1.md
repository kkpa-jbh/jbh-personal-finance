# Frontend Component: Product Movements

These are the details about the API to call. Follow the current standards of the project and follow the best practices.

## API Specification

**Base Path**: `/jbh-api/finance/products/movements`
**Authentication**: JWT token required in Authorization header

---

## Operation: POST - Add Movement to Product

- **Endpoint**: `POST /jbh-api/finance/products/movements/{productId}`
- **Path Parameter**: `productId` (UUID) - The product ID to add the movement to
- **Request Body**:

```typescript
interface AddMovementRequest {
  entryDate: string;       // LocalDate format: "YYYY-MM-DD" (e.g., "2024-01-15")
  totalAmount: number;     // BigDecimal - Required for DEPOSIT/WITHDRAWAL, null for BALANCE_SNAPSHOT
  balanceSnapshot: number; // BigDecimal - Required for BALANCE_SNAPSHOT, null otherwise
  movementType: MovementType;
  categoryName: string;    // Category enum name (see categories below)
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
  movementAmount: number; // BigDecimal
  movementDate: string;   // LocalDate format: "YYYY-MM-DD"
  balanceSnapshot: number; // BigDecimal
  metadata: AccountMovementMetadata;
  createdAt: string;      // ISO DateTime
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
  translationsKey: string; // JSON format: {"en":"English","es":"Spanish"}
}

type CategorySource = 'INCOME' | 'EXPENSE';

interface AccountMovementMetadata {
  data: Record<AccountMovementMetadataKey, unknown>;
}

type AccountMovementMetadataKey =
    | 'TARGET_INTERNAL_ACCOUNT_ID'
    | 'TARGET_INTERNAL_ACCOUNT_NAME'
    | 'INVESTMENT_INCOME_ACCOUNT'
    | 'FILE_IMPORTED_AT_TAG'
    | 'FILE_IMPORT_TAG';
```

---

## Movement Categories

### Income Categories (for DEPOSIT movements)

Call the API `jbh-api/finance/categories/incomes` the response will be something like this:

```java
[
    {
    "name":"TRANSFER",
    "translationKey":"{\n  \"en\": \"Transfer\",\n  \"es\": \"Transferencia\"\n}\n",
    "source":"INCOME"
    },
    {
    "name":"SALARY",
    "translationKey":"{\n  \"en\": \"Salary\",\n  \"es\": \"Salario\"\n}\n",
    "source":"INCOME"
    },
    {
    "name":"DIVIDENDS",
    "translationKey":"{\n  \"en\": \"Dividends\",\n  \"es\": \"Dividendos\"\n}\n",
    "source":"INCOME"
    },
    {
    "name":"FREELANCE",
    "translationKey":"{\n  \"en\": \"Freelance\",\n  \"es\": \"Freelance\"\n}\n",
    "source":"INCOME"
    },
    {
    "name":"INVESTMENT",
    "translationKey":"{\n  \"en\": \"Investment\",\n  \"es\": \"Inversión\"\n}\n",
    "source":"INCOME"
    },
    {
    "name":"RENTAL",
    "translationKey":"{\n  \"en\": \"Rental\",\n  \"es\": \"Renta\"\n}\n",
    "source":"INCOME"
    },
    {
    "name":"GIFT",
    "translationKey":"{\n  \"en\": \"Gift\",\n  \"es\": \"Regalo\"\n}\n",
    "source":"INCOME"
    },
    {
    "name":"OTHER",
    "translationKey":"{\n  \"en\": \"Other\",\n  \"es\": \"Otro\"\n}\n",
    "source":"INCOME"
    },
    {
    "name":"INITIAL_BALANCE",
    "translationKey":"{\n  \"en\": \"Initial Balance\",\n  \"es\": \"Saldo Inicial\"\n}\n",
    "source":"INCOME"
    },
    {
    "name":"DEPOSIT",
    "translationKey":"{\n  \"en\": \"Deposit\",\n  \"es\": \"Depósito\"\n}\n",
    "source":"INCOME"
    }
    ]
```

| Category Name   | Label (EN)      | Label (ES)    |
|-----------------|-----------------|---------------|
| TRANSFER        | Transfer        | Transferencia |
| SALARY          | Salary          | Salario       |
| DIVIDENDS       | Dividends       | Dividendos    |
| FREELANCE       | Freelance       | Freelance     |
| INVESTMENT      | Investment      | Inversión     |
| RENTAL          | Rental          | Renta         |
| GIFT            | Gift            | Regalo        |
| OTHER           | Other           | Otro          |
| INITIAL_BALANCE | Initial Balance | Saldo Inicial |
| DEPOSIT         | Deposit         | Depósito      |

### Expense Categories (for WITHDRAWAL movements)

Call the API `jbh-api/finance/categories/expenses`, the response will be something like this:

```java
[
    {
    "name":"RETEFUENTE",
    "translationKey":"{\n  \"en\": \"Withholding Tax\",\n  \"es\": \"Retención en la Fuente\"\n}\n",
    "source":"EXPENSE"
    },
    {
    "name":"SOCIAL_SECURITY",
    "translationKey":"{\n  \"en\": \"Social Security\",\n  \"es\": \"Seguridad Social\"\n}\n",
    "source":"EXPENSE"
    },
    {
    "name":"PUBLIC_SERVICES",
    "translationKey":"{\n  \"en\": \"Public Services\",\n  \"es\": \"Servicios Públicos\"\n}\n",
    "source":"EXPENSE"
    },
    {
    "name":"PERSONAL",
    "translationKey":"{\n  \"en\": \"Personal\",\n  \"es\": \"Personal\"\n}\n",
    "source":"EXPENSE"
    },
    {
    "name":"TRANSFER",
    "translationKey":"{\n  \"en\": \"Transfer\",\n  \"es\": \"Transferencia\"\n}\n",
    "source":"EXPENSE"
    },
    {
    "name":"INVESTMENT_WITHDRAWAL_TO_CLOSE_IT",
    "translationKey":"{\n  \"en\": \"Investment Total Withdrawal\",\n  \"es\": \"Retiro Total de la Inversión\"\n}\n",
    "source":"EXPENSE"
    }
    ]
```

---

## Project Standards to Follow

### Authentication

- Send JWT token as it's currently working right now (with interceptors)
- Handle responses as it's currently working right now (with interceptors)

### Form Implementation

- Follow existing form component patterns in this project
- Use the project's validation library for client-side validation
- Display validation errors inline below each field
- Show loading state during form submission
- Display success message after successful operation
- Display error message if operation fails

### Multi-Platform Requirements

- Design must work on Desktop, Android, and iOS with ionic/capacitor
- Use responsive breakpoints
- Consider mobile keyboard behavior for forms
- Consider mobile touch interactions for date pickers and amount inputs

### Component Structure

Create the following components:

1. **AddMovementFormComponent**: Form to add a new movement
2. **MovementTypeSelectorComponent**: Toggle/Select between DEPOSIT, WITHDRAWAL, BALANCE_SNAPSHOT
3. **CategorySelectorComponent**: Dropdown/Select for category (filtered by movement type)
4. **MovementSuccessComponent**: Display success state after adding movement

### API Integration

- Create/Update the repository adapter implementing the current standard of the project
- Keep the existing structure of the repository layer

---

## Validation Rules (Client-Side)

Implement these validations before submitting:

| Field           | Validation                          | Error Message (EN)                 | Error Message (ES)                            |
|-----------------|-------------------------------------|------------------------------------|-----------------------------------------------|
| entryDate       | Required                            | Entry date is required             | La fecha de entrada es requerida              |
| entryDate       | Must be <= today                    | Entry date cannot be in the future | La fecha de entrada no puede ser en el futuro |
| movementType    | Required                            | Movement type is required          | El tipo de movimiento es requerido            |
| totalAmount     | Required when DEPOSIT or WITHDRAWAL | Amount is required                 | El monto es requerido                         |
| totalAmount     | Must be > 0                         | Amount must be greater than zero   | El monto debe ser mayor que cero              |
| balanceSnapshot | Required when BALANCE_SNAPSHOT      | Balance snapshot is required       | El saldo es requerido                         |
| balanceSnapshot | Must be >= 0                        | Balance cannot be negative         | El saldo no puede ser negativo                |
| categoryName    | Required                            | Category is required               | La categoría es requerida                     |

---

## Component Display Requirements

### AddMovementFormComponent

**Purpose**: Allow user to add a financial movement to a product.

**UI Elements**:

- Date picker for Entry Date (default: today). The format is YYYY-MM-DD
- Movement Type selector (DEPOSIT, WITHDRAWAL, BALANCE_SNAPSHOT)
- Amount input field (shown for DEPOSIT/WITHDRAWAL)
- Balance Snapshot input field (shown for BALANCE_SNAPSHOT only)
- Category dropdown (filtered by movement type)
- "Add Movement" / "Agregar Movimiento" button
- Cancel button

**Behavior**:

- When movement type changes, update category dropdown options
- DEPOSIT shows Income categories
- WITHDRAWAL shows Expense categories
- BALANCE_SNAPSHOT hides category and amount fields, shows only balance snapshot
- Validate inputs before enabling submit
- Show inline validation errors
- Clear form after successful submission (or close modal)

### MovementTypeSelectorComponent

**Purpose**: Allow user to select the type of movement.

**Display Options**:

| Type             | Label (EN)     | Label (ES)       | Description (EN)                 | Description (ES)                        |
|------------------|----------------|------------------|----------------------------------|-----------------------------------------|
| DEPOSIT          | Deposit        | Depósito         | Add money to your account        | Agregar dinero a tu cuenta              |
| WITHDRAWAL       | Withdrawal     | Retiro           | Remove money from your account   | Retirar dinero de tu cuenta             |
| BALANCE_SNAPSHOT | Balance Update | Actualizar Saldo | Set the current balance directly | Establecer el saldo actual directamente |

**Visual Indicators**:

- DEPOSIT: Green color indicator
- WITHDRAWAL: Red color indicator
- BALANCE_SNAPSHOT: Blue/neutral color indicator

### CategorySelectorComponent

**Purpose**: Allow user to select a category for the movement.

**Behavior**:

- Filter categories based on selected movement type
- Show translated labels based on current language
- Group by source if needed (INCOME/EXPENSE)

### MovementSuccessComponent

**Purpose**: Display success feedback after adding a movement.

**Display Elements**:

- Success icon
- "Movement Added Successfully" / "Movimiento Agregado Exitosamente"
- Summary of added movement:
    - Date
    - Type
    - Amount or Balance
    - Category
- Updated account balance (from response)
- "Add Another" / "Agregar Otro" button
- "Done" / "Listo" button (close/navigate back)

---

## Empty State

Not applicable - this is a form component.

---

## Loading State

- Show loading spinner on submit button
- Disable all form fields while submitting
- Keep form visible during submission

---

## Additional Notes

### Movement Type Logic

The API determines movement type based on the following:

- If `totalAmount` is provided and >= 0: `DEPOSIT`
- If `totalAmount` is provided and < 0: `WITHDRAWAL`
- If only `balanceSnapshot` is provided: `BALANCE_SNAPSHOT`

However, for better UX, explicitly select the movement type and only show relevant fields.

### Amount Handling

- For DEPOSIT: `totalAmount` should be positive
- For WITHDRAWAL: `totalAmount` should be positive (the API handles the sign)
- For BALANCE_SNAPSHOT: only `balanceSnapshot` is required

### Currency Formatting

- Use locale-aware currency formatting for amount inputs
- Display with 2 decimal places for financial data
- Use thousand separators
- Consider using a currency mask input component or pipe

### Date Handling

- Use ISO format (YYYY-MM-DD) when sending to API. Uses the existing PIPE .
- Display in user's locale format
- Default to today's date
- Do not allow future dates

### Response Handling

The response contains:

- `account`: Updated product with new balances
- `movement`: The created movement details
- `monthlyBalance`: Updated monthly balance for the movement's period

Use this data to update local state if needed.

---

Feel free to ask any questions about the implementation or if anything needs clarification before proceeding.
