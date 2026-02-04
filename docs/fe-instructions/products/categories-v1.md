# Frontend Component: Categories (Expense & Income)

We need to create or update the component for Categories. These are **read-only catalog endpoints** that return the available expense and income categories used when creating movements.

These are the details about the APIs to call. Follow the current standards of the project and follow the best practices.

Check if the component already exists to update it.

## API Specification

**Base Path**: `/jbh-api/finance/categories`
**Authentication**: JWT token required in Authorization header

---

## Operations to Implement

### GET All Expense Categories

- **Endpoint**: `GET /jbh-api/finance/categories/expenses`
- **Response**: Array of `CategoryDTO`

```typescript
interface CategoryDTO {
  name: string;           // Enum value name (e.g., "RETEFUENTE", "SALARY")
  translationKey: string; // JSON string with translations: {"en":"...","es":"..."}
  source: CategorySource; // "EXPENSE" or "INCOME"
}

type CategorySource = 'INCOME' | 'EXPENSE';
```

**Available Expense Categories**:
| Name | English | Spanish |
|------|---------|---------|
| RETEFUENTE | Withholding Tax | Retención en la Fuente |
| SOCIAL_SECURITY | Social Security | Seguridad Social |
| PUBLIC_SERVICES | Public Services | Servicios Públicos |
| PERSONAL | Personal | Personal |
| TRANSFER | Transfer | Transferencia |
| INVESTMENT_WITHDRAWAL_TO_CLOSE_IT | Investment Total Withdrawal | Retiro Total de la Inversión |

---

### GET All Income Categories

- **Endpoint**: `GET /jbh-api/finance/categories/incomes`
- **Response**: Array of `CategoryDTO`

```typescript
interface CategoryDTO {
  name: string;           // Enum value name (e.g., "SALARY", "DIVIDENDS")
  translationKey: string; // JSON string with translations: {"en":"...","es":"..."}
  source: CategorySource; // "EXPENSE" or "INCOME"
}

type CategorySource = 'INCOME' | 'EXPENSE';
```

**Available Income Categories**:
| Name | English | Spanish |
|------|---------|---------|
| TRANSFER | Transfer | Transferencia |
| SALARY | Salary | Salario |
| DIVIDENDS | Dividends | Dividendos |
| FREELANCE | Freelance | Freelance |
| INVESTMENT | Investment | Inversión |
| RENTAL | Rental | Renta |
| GIFT | Gift | Regalo |
| OTHER | Other | Otro |
| INITIAL_BALANCE | Initial Balance | Saldo Inicial |
| DEPOSIT | Deposit | Depósito |

---

## Project Standards to Follow

### Authentication

- Send JWT token as it's currently working right now (with interceptors)
- Handle responses as it's currently working right now (with interceptors)

### Multi-Platform Requirements

- Design must work on Desktop, Android, and iOS with ionic/capacitor
- Use responsive breakpoints

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

- Create/Update the repository adapter implementing the current standard of the project
- Keep the existing structure of the repository layer

---

## Implementation Notes

### Use Case

These APIs provide the category catalogs needed when:
1. Creating a new movement (the user selects a category)
2. Filtering movements by category
3. Displaying category labels in movement lists

### Translation Key Parsing

The `translationKey` field is a JSON string that needs to be parsed:

```typescript
// Example translationKey: '{"en":"Salary","es":"Salario"}'
interface Translation {
  en: string;
  es: string;
}

function parseTranslation(translationKey: string): Translation {
  return JSON.parse(translationKey);
}

// Usage based on current locale:
const translation = parseTranslation(category.translationKey);
const label = currentLocale === 'es' ? translation.es : translation.en;
```

### Caching Recommendation

Since categories are static catalog data:
- Cache the response locally after first fetch
- Categories don't change during user sessions
- Consider storing in a service as a singleton or use local storage

### Model Definition

Create the following TypeScript interfaces:

```typescript
// core/domain/models/category.model.ts

export type CategorySource = 'INCOME' | 'EXPENSE';

export interface Category {
  name: string;
  translationKey: string;
  source: CategorySource;
}

// Helper type for parsed translations
export interface CategoryWithLabel extends Category {
  label: string; // Resolved based on current locale
}
```

### Repository Interface

```typescript
// core/domain/repositories/category.repository.ts

export interface CategoryRepository {
  getExpenseCategories(): Promise<Category[]>;
  getIncomeCategories(): Promise<Category[]>;
}
```

---

## Additional Notes

- These are **read-only** endpoints - no POST, PUT, or DELETE operations
- The categories are fixed enums in the backend - they don't change dynamically
- Do not implement state management
- The application only supports EN and ES languages. Be aware about the translations with good grammar
- Consider creating a shared `CategorySelectComponent` that can be reused wherever category selection is needed (e.g., in the Add Movement form)

---

Feel free to ask any questions or if something needs to be confirmed before implementation.
