# Frontend Component: Product Movements List

We need to create or update the component to display movements for a specific product.

If it's a new component, check what might be the best place to put it on the menu based on the context.
If you don't feel sure, ask the user by providing suggestions.

These are the details about the API to call. Follow the current standards of the project and follow the best practices.

Check if the component already exists to update it.

## API Specification

**Base Path**: `/jbh-api/finance/products/movements`
**Authentication**: JWT token required in Authorization header

### Operation to Implement

#### GET - List Movements by Product

**Endpoint**: `GET /jbh-api/finance/products/movements/{productId}`

**Description**: Retrieves movements for a specific product within the last 3 months (inclusive). This includes deposits, withdrawals, and balance snapshots.

**Path Parameters**:
- `productId` (UUID, required): The ID of the product to retrieve movements for

**Response**: Array of Movement objects

```typescript
interface MovementResponse {
  id: MovementId;              // UUID value object
  accountId: ProductId;         // UUID value object (same as productId)
  movementType: MovementType;   // Enum: "DEPOSIT" | "WITHDRAWAL" | "BALANCE_SNAPSHOT"
  category: CategoryType | null; // Object with source, typeName, translationsKey
  movementAmount: BigDecimal;   // Decimal number (use jbhDecimalFormat directive)
  movementDate: LocalDate;      // Date string (YYYY-MM-DD) (use jbhDate pipe)
  balanceSnapshot: BigDecimal;  // Decimal number (use jbhDecimalFormat directive)
  metadata: MovementMetadata;   // Map of metadata keys (e.g., target product info, import tags)
  createdAt: LocalDateTime;     // ISO 8601 datetime string (use jbhDate pipe)
  description: string;          // Movement description
}

// Supporting types
enum MovementType {
  DEPOSIT = "DEPOSIT",           // Positive amount movement
  WITHDRAWAL = "WITHDRAWAL",     // Negative amount movement
  BALANCE_SNAPSHOT = "BALANCE_SNAPSHOT" // No amount, just balance update
}

interface CategoryType {
  source: "INCOME" | "EXPENSE";
  typeName: string;
  translationsKey: string;
}

interface MovementMetadata {
  // Map containing optional metadata fields like:
  // - TARGET_INTERNAL_ACCOUNT_ID
  // - TARGET_INTERNAL_ACCOUNT_NAME
  // - INVESTMENT_INCOME_ACCOUNT
  // - FILE_IMPORTED_AT_TAG
  // - FILE_IMPORT_TAG
  [key: string]: any;
}

// Example Response:
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "accountId": "660e8400-e29b-41d4-a716-446655440001",
    "movementType": "DEPOSIT",
    "category": {
      "source": "INCOME",
      "typeName": "SALARY",
      "translationsKey": "categories.income.salary"
    },
    "movementAmount": 5000.00,
    "movementDate": "2025-02-01",
    "balanceSnapshot": 25000.00,
    "metadata": {},
    "createdAt": "2025-02-01T10:30:00",
    "description": "Monthly salary"
  },
  {
    "id": "550e8400-e29b-41d4-a716-446655440002",
    "accountId": "660e8400-e29b-41d4-a716-446655440001",
    "movementType": "WITHDRAWAL",
    "category": {
      "source": "EXPENSE",
      "typeName": "GROCERIES",
      "translationsKey": "categories.expense.groceries"
    },
    "movementAmount": -150.50,
    "movementDate": "2025-02-05",
    "balanceSnapshot": 24849.50,
    "metadata": {},
    "createdAt": "2025-02-05T14:20:00",
    "description": "Supermarket shopping"
  }
]
```

## Project Standards to Follow

### Authentication

- Send JWT token as it's currently working right now (with interceptors)
- Handle responses as it's currently working right now (with interceptors)

### List/Table Implementation

- Follow existing list/table component patterns in this project
- Display movements in a table or card list format
- Show empty state when no movements are found
- Sort movements by date (most recent first by default)
- Consider grouping by date or month for better UX
- Use loading state while fetching data

### Directives and Pipes

**CRITICAL - Apply these formatting rules:**

- **Always apply the directive `jbhDecimalFormat`** for `movementAmount` and `balanceSnapshot` (BigDecimal fields)
- **Always apply the pipe `jbhDate`** for `movementDate` and `createdAt` (Date/DateTime fields)
- **Consider using the `jbhCurrency` pipe** for `movementAmount` and `balanceSnapshot` if they represent currency values

### Display Guidelines

- **Movement Type Indicator**: Use visual indicators (icons/colors) for movement types:
  - DEPOSIT: Green color, up arrow icon
  - WITHDRAWAL: Red color, down arrow icon
  - BALANCE_SNAPSHOT: Blue/gray color, info icon

- **Category Display**: Show category name using the `translationsKey` for internationalization

- **Description**: Display the movement description prominently

- **Metadata**: If metadata contains transfer information (TARGET_INTERNAL_ACCOUNT_NAME), display it as additional context

- **Date Grouping**: Consider grouping movements by month or date for better readability

### Multi-Platform Requirements

- Design must work on Desktop, Android, and iOS with Ionic/Capacitor
- Use responsive breakpoints
- Consider touch-friendly tap targets for mobile
- Use Ionic components (ion-list, ion-card, ion-item) for consistent UI

### CRITICAL RULES

- Check the ### Critical Rules section of the CLAUDE instructions
- Register all Ionic icons used in the component

### Component Structure

#### Project Structure Overview

```
src/app/
├── core/domain/          # Models, repositories, use-cases
├── core/infrastructure/  # API, persistence, implementations
├── features/             # Lazy-loaded feature modules (pages, components)
├── shared/               # Reusable components, pipes, directives, utilities
└── theme/                # custom-components.scss (single source)
```

### API Integration

- Create/Update the repository adapter implementing the current standard of the project
- Keep the existing structure of the repository layer
- Check if there is already a model with the same attributes/schema to use it
- The DTO objects returned by the API are usually mapped to models with suffix `ApiResponse`
- Map the `MovementResponse` to a frontend model (e.g., `MovementApiResponse`)

## Additional Notes

### Business Rules

- The API returns movements for the last 3 months by default (inclusive)
- Movements include deposits, withdrawals, and balance snapshots
- Each movement has a category (expense or income) except for balance snapshots
- The `balanceSnapshot` field shows the account balance after the movement
- Metadata may contain additional information about transfers or imported files

### Edge Cases

- Handle empty list (no movements in the last 3 months)
- Handle movements without categories (balance snapshots)
- Handle movements without descriptions
- Consider pagination or infinite scroll if the list becomes too long

### User Experience Enhancements

- Allow filtering by movement type (deposits, withdrawals, snapshots)
- Allow filtering by category
- Allow searching by description
- Show total deposits and withdrawals for the period
- Show net change (deposits - withdrawals)
- Add a refresh button to reload movements

---

**Important Reminders:**

- Do not implement state management
- Do not forget to register the Ionic icons used by the HTML
- The error handling is already implemented - do not mention it
- Do not suggest adding unit tests - the project does not support that
- The application only supports EN and ES languages - be aware about the translations with good grammar
- Do not add comments unless the code is complex

---

**Feel free to ask any questions or request confirmation for anything that needs clarification before implementing this component!**
