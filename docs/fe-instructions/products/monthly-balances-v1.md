# Frontend Component: Product Monthly Balances

These are the details about the API to call. Follow the current standards of the project and follow the best practices.

## API Specification

**Base Path**: `/jbh-api/finance/products/monthly-balances`
**Authentication**: JWT token required in Authorization header

---

## Operation: POST - Find Monthly Balances by Product

- **Endpoint**: `POST /jbh-api/finance/products/monthly-balances/{productId}`
- **Path Parameter**: `productId` (UUID) - The product ID to query
- **Request Body**:

```typescript
interface MonthlyBalanceRequest {
  startPeriod: string; // YearMonth format: "YYYY-MM" (e.g., "2024-01")
  endPeriod: string;   // YearMonth format: "YYYY-MM" (e.g., "2024-12")
}
```

- **Response**: Array of `MonthlyBalanceDTO`

```typescript
interface MonthlyBalanceDTO {
  id: number;
  accountId: ProductId;
  year: number;
  month: number;
  period: string; // YearMonth format: "YYYY-MM"
  netGrowthRate: number; // BigDecimal - percentage
  totalDebits: number; // BigDecimal
  totalCredits: number; // BigDecimal
  movementBalance: number; // BigDecimal
  openingBalance: number; // BigDecimal
  closingBalance: number; // BigDecimal
  monthlyNetProfit: number; // BigDecimal
  totalMovements: number;
  gapPeriod: boolean;
  officialMonthlyReport: boolean;
  monthlyReportedProfit: number; // BigDecimal
  incomeWithholdingTaxAmount: number; // BigDecimal (RETEFUENTE)
  createdAt: string; // ISO DateTime
  updatedAt: string; // ISO DateTime
}

interface ProductId {
  value: string; // UUID
}
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
- Consider mobile touch interactions for date pickers

### Component Structure

Create the following components:

1. **MonthlyBalancesFilterComponent**: Date range picker form (startPeriod, endPeriod)
2. **MonthlyBalancesTableComponent**: Display results in a scrollable table
3. **MonthlyBalancesEmptyComponent**: Display when no data exists for the selected range

### API Integration

- Create/Update the repository adapter implementing the current standard of the project
- Keep the existing structure of the repository layer

---

## Validation Rules (Client-Side)

Implement these validations before submitting:

| Field | Validation | Error Message (EN) | Error Message (ES) |
|-------|------------|-------------------|-------------------|
| startPeriod | Required | Start period is required | El periodo inicial es requerido |
| endPeriod | Required | End period is required | El periodo final es requerido |
| startPeriod | Must be <= endPeriod | Start period must be before or equal to end period | El periodo inicial debe ser anterior o igual al periodo final |
| endPeriod | Must be <= current month | End period cannot be in the future | El periodo final no puede ser en el futuro |

---

## Component Display Requirements

### MonthlyBalancesFilterComponent

**Purpose**: Allow user to select a date range for querying monthly balances.

**UI Elements**:
- Month/Year picker for Start Period
- Month/Year picker for End Period
- "Search" / "Buscar" button
- Clear/Reset button (optional)

**Behavior**:
- Default range: Last 6 months to current month
- Validate inputs before enabling submit
- Show inline validation errors

### MonthlyBalancesTableComponent

**Purpose**: Display monthly balance records in a table format.

**Table Columns**:

| Column Header (EN/ES) | Field | Format |
|----------------------|-------|--------|
| Period / Periodo | period | "MMM YYYY" (e.g., "Jan 2024") |
| Opening / Apertura | openingBalance | Currency |
| Credits / Créditos | totalCredits | Currency (green) |
| Debits / Débitos | totalDebits | Currency (red) |
| Closing / Cierre | closingBalance | Currency |
| Net Profit / Ganancia Neta | monthlyNetProfit | Currency (+/- color) |
| Growth Rate / Crecimiento | netGrowthRate | Percentage with +/- |
| Movements / Movimientos | totalMovements | Number |
| Withholding Tax / Retención | incomeWithholdingTaxAmount | Currency |

**Features**:
- Sortable columns (by period default, ascending)
- Responsive horizontal scroll on mobile
- Summary row at bottom showing totals
- Visual indicator for `gapPeriod` rows (e.g., gray background or icon)
- Badge for `officialMonthlyReport` rows

**Visual Indicators**:
- Green for positive net profit
- Red for negative net profit
- Gray/muted style for gap periods
- Badge icon for official monthly reports

---

## Empty State

When the API returns an empty array:

**Title (EN)**: "No Monthly Balances Found"
**Title (ES)**: "No se Encontraron Saldos Mensuales"

**Message (EN)**: "No monthly balance records exist for the selected period. Try selecting a different date range."
**Message (ES)**: "No existen registros de saldos mensuales para el periodo seleccionado. Intenta seleccionar un rango de fechas diferente."

---

## Loading State

- Show loading spinner overlay on the table area
- Disable the search button while loading
- Keep filter form visible and interactive

---

## Additional Notes

### YearMonth Format

The API expects YearMonth in ISO format: `"YYYY-MM"` (e.g., `"2024-01"` for January 2024).

### Currency Formatting

- Use locale-aware currency formatting
- Display with 2 decimal places for financial data
- Use thousand separators

### Gap Periods

Rows where `gapPeriod: true` indicate months with no recorded data. Display these with:
- Muted/gray background color
- Tooltip: "No data recorded for this period" / "Sin datos registrados para este periodo"

### Official Monthly Reports

Rows where `officialMonthlyReport: true` indicate verified/official data. Display with:
- Small badge or checkmark icon
- Tooltip: "Official monthly report" / "Reporte mensual oficial"

---

Feel free to ask any questions about the implementation or if anything needs clarification before proceeding.
