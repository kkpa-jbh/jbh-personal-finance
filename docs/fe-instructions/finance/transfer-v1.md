# Frontend Component: Transfer Between Products

We need to create or update the component for transferring funds between two JBH products.

Check if a transfer component already exists (e.g., inside the products or movements feature). If it does not exist, consider whether it belongs as a modal dialog triggered from
the product list/detail view, or as a dedicated page. Suggest the best option based on the existing navigation structure and ask the user to confirm before proceeding.

These are the details about the API to call. Follow the current standards of the project and best practices.

---

## API Specification

**Base Path**: `/jbh-api/finance/products/transfers`
**Authentication**: JWT token required in Authorization header (handled by interceptors)

### Operations to Implement

#### POST - Execute Transfer

- **Endpoint**: `POST /jbh-api/finance/products/transfers/`
- **Query Parameter**: `fromAccountId` (UUID, required) — the source product ID
- **Request Body**:

```typescript
interface AddTransferRequest {
  toAccountId: string;       // UUID — destination product ID
  totalAmount: number;       // BigDecimal — amount to transfer
  transferDate: string;      // LocalDate — ISO date string (YYYY-MM-DD)
}
```

- **Response**: `204 No Content` on success

---

## UX / Component Behavior

- The component should be a **form** that collects:
    1. **From Account** — pre-selected or selectable from the user's product list
    2. **To Account** — selectable from the user's product list (must differ from source)
    3. **Amount** — decimal input (BigDecimal)
    4. **Transfer Date** — date picker (defaults to today)
- Disable the submit button while the request is in flight (loading state)
- Show a success message after a successful transfer
- The error handling is already implemented — do not add extra error handling logic
- After a successful transfer, close the modal or navigate back, and refresh the source product balance if visible.
- Check if there is a products selection component in shared

---

## Project Standards to Follow

### Form Implementation

- Follow existing form component patterns in this project
- Use the project's validation library for client-side validation
- Display validation errors inline below each field
- Show loading state during form submission
- Display success message after successful operation

### Directives or Pipes

- Pipes (`|`) for displaying values in templates
- Directives (`[]`) ONLY for `<input>` form elements

```html
<!-- ❌ WRONG - Directive on non-input element -->
<span [jbhDecimalFormat]="value"></span>

<!-- ✅ CORRECT - Pipe for display -->
<span>{{ value | jbhCurrency }}</span>

<!-- ✅ CORRECT - Directive on input -->
<input [jbhDecimalFormat]="2" formControlName="totalAmount"/>
```

- Apply the directive `jbhDecimalFormat` to the `totalAmount` input field
- Apply the pipe `jbhDate` when displaying `transferDate` values
- Apply the pipe `jbhCurrency` when displaying `totalAmount` values

### Multi-Platform Requirements

- Design must work on Desktop, Android, and iOS (Ionic/Capacitor)
- Use responsive breakpoints
- Consider mobile keyboard behavior for forms

### CRITICAL RULES

- Check the ### Critical Rules section of the CLAUDE instructions

---

## Component Structure

### Project Structure Overview

```
src/app/
├── core/domain/          # Models, repositories, use-cases
├── core/infrastructure/  # API, persistence, implementations
├── features/             # Lazy-loaded feature modules (pages, components)
├── shared/               # Reusable components, pipes, directives, utilities
└── theme/                # custom-components.scss (single source)
```

### API Integration

- Create or update the repository adapter following the current project standard
- Keep the existing structure of the repository layer
- Check if a model already exists with matching attributes — DTO responses are usually mapped to models with the suffix `ApiResponse`; apply the same logic for `Request` objects
- Map the request as `AddTransferRequest` if not already mapped

```typescript
interface AddTransferRequest {
  toAccountId: string;
  totalAmount: number;
  transferDate: string; // ISO date YYYY-MM-DD
}
```

---

## Additional Notes

- `fromAccountId` is sent as a **query parameter**, not in the request body
- `totalAmount` uses BigDecimal precision — use the `jbhDecimalFormat` directive on the input
- `transferDate` is a `LocalDate` on the backend — send as ISO string `YYYY-MM-DD`
- The two products (source and destination) must belong to the same authenticated user — the backend enforces this via JWT
- Do not implement state management
- Do not forget to register the Ionic icons used by the HTML
- The application supports **EN** and **ES** languages — include translations with correct grammar for both
- Do not suggest adding unit tests — the project does not support that

---

Feel free to ask any questions or flag anything that needs confirmation before proceeding (e.g., whether the component should be a modal or a page, or which product list API to use
to populate the account selectors).
