# Frontend Migration Guide: BalanceHistoryResponse

**Purpose:** Help frontend developers rename models to match backend API naming conventions

---

## Quick Reference: Correct Names

| Object Type | ✅ Correct Name                    | ❌ Common Wrong Names                                  |
|-------------|-----------------------------------|-------------------------------------------------------|
| Main        | `BalanceHistoryResponse`          | BalanceHistoryDTO, BalanceHistory, BalanceHistoryData |
| Summary     | `BalanceHistorySummaryResponse`   | BalanceHistorySummary, SummaryDTO                     |
| Entry       | `BalanceHistoryEntryResponse`     | BalanceHistoryEntry, BalanceEntryDTO, HistoryItem     |

---

## Step-by-Step Frontend Alignment

### Step 1: Identify Your Current Model Names

Check your frontend code for interfaces/classes that handle balance history data.

Common locations:
```
src/app/models/
src/app/interfaces/
src/app/types/
```

### Step 2: Rename to Match Backend

If you currently have different names, rename them following this pattern:

#### Example: If you had `BalanceHistory` (wrong)

**Before:**
```typescript
export interface BalanceHistory {
  summary: BalanceHistorySummary;
  balances: BalanceHistoryEntry[];
}
```

**After:**
```typescript
export interface BalanceHistoryResponse {
  summary: BalanceHistorySummaryResponse;
  balances: BalanceHistoryEntryResponse[];
}
```

### Step 3: Update All References

Search and replace across your frontend codebase:

```bash
# If you're using VS Code or similar IDE
# Search for old name → Replace with new name

BalanceHistory → BalanceHistoryResponse
BalanceHistorySummary → BalanceHistorySummaryResponse
BalanceHistoryEntry → BalanceHistoryEntryResponse
```

### Step 4: Verify Field Names Match

Ensure all field names match the backend exactly (check the schema document).

**Common mismatches to check:**

| ❌ Frontend (Wrong)     | ✅ Backend (Correct)    |
|------------------------|------------------------|
| `total_balance`        | `totalBalance`         |
| `period_change`        | `periodChange`         |
| `growth_rate`          | `growthRate`           |
| `movement_count`       | `movementCount`        |
| `is_gap_period`        | `isGapPeriod`          |
| `is_official_report`   | `isOfficialReport`     |
| `product_id`           | `productId`            |
| `product_name`         | `productName`          |
| `is_profitable`        | `isProfitable`         |
| `is_loss`              | `isLoss`               |
| `total_debits`         | `totalDebits`          |
| `total_credits`        | `totalCredits`         |

---

## Complete TypeScript Model (Copy-Paste Ready)

Save this as `balance-history-response.model.ts`:

```typescript
/**
 * Balance History API Response Models
 * Auto-generated from backend API schema
 * DO NOT modify field names - they must match backend exactly
 */

/**
 * Main response object for balance history API
 */
export interface BalanceHistoryResponse {
  summary: BalanceHistorySummaryResponse;
  balances: BalanceHistoryEntryResponse[];
}

/**
 * Summary statistics for the entire balance history period
 */
export interface BalanceHistorySummaryResponse {
  /** The current total balance at the end of the selected period */
  totalBalance: number;
  /** How much the balance grew (or shrank) during this period */
  periodChange: number;
  /** Percentage change over the selected period */
  periodChangePercent: number;
  /** Average monthly growth rate across the period */
  avgGrowthRate: number;
  /** Total number of transactions/movements during the period */
  totalMovements: number;
}

/**
 * Detailed month-by-month breakdown of balance data
 */
export interface BalanceHistoryEntryResponse {
  /** Period in YYYY-MM format (e.g., "2024-01") */
  period: string;
  /** Opening balance at the start of the period */
  openingBalance: number;
  /** Closing balance at the end of the period */
  closingBalance: number;
  /** Profit for the period */
  profit: number;
  /** Growth rate percentage for the period */
  growthRate: number;
  /** Number of movements/transactions in this period */
  movementCount: number;
  /** True if this period has missing data (gap in records) */
  isGapPeriod: boolean;
  /** True if this period has an official monthly report */
  isOfficialReport: boolean;
  /** Product identifier as UUID string */
  productId: string;
  /** Human-readable product name */
  productName: string;
  /** True if the period was profitable (profit > 0) */
  isProfitable: boolean;
  /** True if the period resulted in a loss (profit < 0) */
  isLoss: boolean;
  /** Total debit movements for the period */
  totalDebits: number;
  /** Total credit movements for the period */
  totalCredits: number;
}

/**
 * Factory method to create an empty balance history response
 */
export function createEmptyBalanceHistoryResponse(): BalanceHistoryResponse {
  return {
    summary: {
      totalBalance: 0,
      periodChange: 0,
      periodChangePercent: 0,
      avgGrowthRate: 0,
      totalMovements: 0
    },
    balances: []
  };
}
```

---

## Usage Example

```typescript
import { BalanceHistoryResponse } from './models/balance-history-response.model';

// In your service
getBalanceHistory(productId: string, from: string, to: string): Observable<BalanceHistoryResponse> {
  return this.http.get<BalanceHistoryResponse>(
    `/api/v1/products/${productId}/balance-history`,
    { params: { from, to } }
  );
}

// In your component
this.balanceHistoryService.getBalanceHistory(productId, from, to)
  .subscribe((response: BalanceHistoryResponse) => {
    console.log('Total Balance:', response.summary.totalBalance);
    console.log('Period Change:', response.summary.periodChange);
    console.log('History Entries:', response.balances.length);

    response.balances.forEach(entry => {
      console.log(`${entry.period}: ${entry.profit}`);
    });
  });
```

---

## Testing Your Changes

After renaming, verify:

1. **Type Safety:** No TypeScript compilation errors
2. **API Calls:** All HTTP requests still work
3. **Data Binding:** UI components display data correctly
4. **Serialization:** JSON parsing works as expected

### Quick Test

```typescript
// This should compile without errors
const testResponse: BalanceHistoryResponse = {
  summary: {
    totalBalance: 1000,
    periodChange: 100,
    periodChangePercent: 10,
    avgGrowthRate: 2.5,
    totalMovements: 5
  },
  balances: [
    {
      period: '2024-01',
      openingBalance: 900,
      closingBalance: 1000,
      profit: 100,
      growthRate: 11.11,
      movementCount: 5,
      isGapPeriod: false,
      isOfficialReport: true,
      productId: '550e8400-e29b-41d4-a716-446655440000',
      productName: 'Test Product',
      isProfitable: true,
      isLoss: false,
      totalDebits: 200,
      totalCredits: 300
    }
  ]
};
```

---

## Checklist

Use this checklist when migrating:

- [ ] Renamed main interface to `BalanceHistoryResponse`
- [ ] Renamed summary interface to `BalanceHistorySummaryResponse`
- [ ] Renamed entry interface to `BalanceHistoryEntryResponse`
- [ ] Updated all imports
- [ ] Updated all service method signatures
- [ ] Updated all component property types
- [ ] Verified field names match backend (camelCase)
- [ ] Tested API calls still work
- [ ] Tested UI still renders correctly
- [ ] Committed changes with descriptive message

---

## Need Help?

If you're still confused about the naming:

1. **Check the schema:** See `BalanceHistoryResponse.schema.md`
2. **Use the validate-api-naming skill:** Run `/validate-api-naming` on your frontend models
3. **Compare with backend:** Look at the example JSON in the schema document

**Remember:** The backend defines the API contract. Frontend must always follow backend naming for response objects.
