# Frontend Component: Register Monthly Balance

We need to create or update the component for registering official monthly balance reports.

If it's a new component, check what might be the best place to put it on the menu based on the context.
If you don't feel sure, ask the user by providing suggestions.

These are the details about the API to call. Follow the current standards of the project and follow the best practices.

Check if the component already exists to update it.

## API Specification

**Base Path**: `/api/v1/products/{productId}/register`
**Authentication**: JWT token required in Authorization header

### Operation to Implement

#### POST - Register Official Monthly Balance

- **Endpoint**: `POST /api/v1/products/{productId}/register`
- **Path Parameter**: `productId` (UUID) - The product/account identifier
- **Description**: Register official monthly balance report with closing balance and profit
- **Request Body**:

```typescript
interface RegisterMonthlyBalanceRequest {
  monthlyPeriod: string;              // Format: "YYYY-MM" (e.g., "2026-01")
  closingBalance: number;             // BigDecimal - Closing balance amount
  monthlyProfitReported: number;      // BigDecimal - Official reported profit
  incomeWithholdingTaxAmount: number; // BigDecimal - Tax withheld on income
}
```

**Example Request**:

```json
{
  "monthlyPeriod": "2026-01",
  "closingBalance": 150000.50,
  "monthlyProfitReported": 5000.00,
  "incomeWithholdingTaxAmount": 500.00
}
```

- **Response**: Created MonthlyBalance object

```typescript
interface MonthlyBalanceDTO {
  id: number;
  accountId: {
    value: string;  // UUID
  };
  year: number;
  month: number;
  period: string;                     // Format: "YYYY-MM"
  netGrowthRate: number;              // BigDecimal - Growth rate percentage
  totalDebits: number;                // BigDecimal
  totalCredits: number;               // BigDecimal
  movementBalance: number;            // BigDecimal - Balance from movements
  openingBalance: number;             // BigDecimal
  closingBalance: number;             // BigDecimal
  monthlyNetProfit: number;           // BigDecimal
  totalMovements: number;
  gapPeriod: boolean;
  officialMonthlyReport: boolean;
  monthlyReportedProfit: number;      // BigDecimal
  incomeWithholdingTaxAmount: number; // BigDecimal
  createdAt: string;                  // ISO DateTime format
  updatedAt: string;                  // ISO DateTime format
}
```

**Example Response**:

```json
{
  "id": 123,
  "accountId": {
    "value": "f47ac10b-58cc-4372-a567-0e02b2c3d479"
  },
  "year": 2026,
  "month": 1,
  "period": "2026-01",
  "netGrowthRate": 3.45,
  "totalDebits": 2000.00,
  "totalCredits": 7000.00,
  "movementBalance": 5000.00,
  "openingBalance": 145000.50,
  "closingBalance": 150000.50,
  "monthlyNetProfit": 5000.00,
  "totalMovements": 15,
  "gapPeriod": false,
  "officialMonthlyReport": true,
  "monthlyReportedProfit": 5000.00,
  "incomeWithholdingTaxAmount": 500.00,
  "createdAt": "2026-02-01T10:30:00",
  "updatedAt": "2026-02-01T10:30:00"
}
```

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
- Use decimal/Date/currency directives or pipes where they apply (Based on the Response Type object)
- Register all icons used in the component by adding them to ionic
- Ask the user if the component needs a button to refresh the info. Follow the pattern for that.

### Directives or Pipes

- **Always** apply the directive `jbhDecimalFormat` for BigDecimal objects in the API's response:
    - `closingBalance`
    - `monthlyProfitReported` / `monthlyReportedProfit`
    - `incomeWithholdingTaxAmount`
    - `netGrowthRate`
    - `totalDebits`
    - `totalCredits`
    - `movementBalance`
    - `openingBalance`
    - `monthlyNetProfit`

- **Always** apply the pipe `jbhDate` for Date objects present in the API's response:
    - `createdAt` (ISO DateTime format)
    - `updatedAt` (ISO DateTime format)

- **Always** apply the currency pipe `jbhCurrency` for currency values in the API's response:
    - All BigDecimal monetary values

### Multi-Platform Requirements

- Design must work on Desktop, Android, and iOS with ionic/capacitor
- Use responsive breakpoints
- Consider mobile keyboard behavior for forms

### CRITICAL RULES

- Check the ### Critical Rules section of the CLAUDE instructions

### Component Structure

#### Project Structure Overview

```
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
- Check if there is already a model with the same attributes/schema to use it. The DTO objects returned by the API
  are usually mapped to models with suffix `ApiResponse`. Apply same logic for 'Request` objects.
- If the DTO Response/Request is not mapped, mapped them with the same name of the API DTO Responses/Request.

## Business Rules & Context

1. **Purpose**: This endpoint allows users to register the official monthly balance report for a financial product/account
2. **Period Format**: The `monthlyPeriod` should be in "YYYY-MM" format (e.g., "2026-01" for January 2026)
3. **Official Report Flag**: When registered via this endpoint, `officialMonthlyReport` will be set to `true`
4. **Tax Handling**: The `incomeWithholdingTaxAmount` represents tax withheld on income and should be clearly labeled
5. **Validation Considerations**:
    - All monetary amounts should accept decimal values
    - Period should validate month (1-12) and reasonable year range
    - Product ID must be a valid UUID
    - Consider if closing balance should be >= 0 (confirm with user)

## Form Fields

The form should include:

1. **Monthly Period** (required)
    - Type: Month picker (YYYY-MM format)
    - Label: "Monthly Period" / "Período Mensual"

2. **Closing Balance** (required)
    - Type: Decimal input with currency formatting
    - Label: "Closing Balance" / "Saldo de Cierre"
    - Apply `jbhDecimalFormat` and `jbhCurrency`

3. **Monthly Profit Reported** (required)
    - Type: Decimal input with currency formatting
    - Label: "Monthly Profit Reported" / "Ganancia Mensual Reportada"
    - Apply `jbhDecimalFormat` and `jbhCurrency`

4. **Income Withholding Tax Amount** (required)
    - Type: Decimal input with currency formatting
    - Label: "Income Withholding Tax" / "Retención de Impuesto sobre Ingresos"
    - Apply `jbhDecimalFormat` and `jbhCurrency`

## Success Response Display

After successful registration, display:

- Success message
- Monthly period registered
- Closing balance
- Monthly profit
- Consider showing the full response or navigating to a detail view

## Additional Notes

- Do not implement state management
- Do not forget to register the Ionic icons used by the HTML
- The error handling is already implemented. Do not mention it.
- Do not suggest adding unit tests. The project does not support that.
- The application only supports EN and ES languages. Be aware about the translations with good grammar.

---

**Feel free to ask any questions or request confirmation about:**

- Where this component should be placed in the menu/navigation
- If the form should be modal or a full page
- If there should be validations on specific fields (e.g., profit can be negative?)
- If a confirmation dialog is needed before submitting
- Any specific UX patterns for month/period selection
- Whether to display calculated vs reported profit differences
