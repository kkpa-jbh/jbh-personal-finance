# BalanceHistoryResponse API Schema

---

## Overview

This is the **official API response object** for balance history endpoints. The frontend must align its models with this exact structure.

---

## TypeScript Interface Definition

```typescript
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
  /** The current total balance at the end of the selected period.
   * Sum of closing balances across all products for the latest month. */
  totalBalance: number;

  /** How much the balance grew (or shrank) during this period */
  periodChange: number;

  /** Percentage change over the selected period */
  periodChangePercent: number;

  /** The average monthly growth rate across the period.
   * On average, balance grew by this percentage each month */
  avgGrowthRate: number;

  /** The total number of transactions/movements during the period */
  totalMovements: number;
}

/**
 * Detailed month-by-month breakdown of balance data for a specific product
 */
export interface BalanceHistoryEntryResponse {
  /** Period in YYYY-MM format (e.g., "2024-01") */
  period: string; // YearMonth serialized as string

  /** Opening balance at the start of the period */
  openingBalance: number;

  /** Closing balance at the end of the period */
  closingBalance: number;

  /** Profit for the period (uses official report if available, otherwise net profit) */
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
  productId: string; // ProductId.value (UUID)

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
```

---

## JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "BalanceHistoryResponse",
  "type": "object",
  "required": [
    "summary",
    "balances"
  ],
  "properties": {
    "summary": {
      "$ref": "#/definitions/BalanceHistorySummaryResponse"
    },
    "balances": {
      "type": "array",
      "items": {
        "$ref": "#/definitions/BalanceHistoryEntryResponse"
      }
    }
  },
  "definitions": {
    "BalanceHistorySummaryResponse": {
      "type": "object",
      "required": [
        "totalBalance",
        "periodChange",
        "periodChangePercent",
        "avgGrowthRate",
        "totalMovements"
      ],
      "properties": {
        "totalBalance": {
          "type": "number",
          "description": "Current total balance at period end"
        },
        "periodChange": {
          "type": "number",
          "description": "Balance change during period"
        },
        "periodChangePercent": {
          "type": "number",
          "description": "Percentage change over period"
        },
        "avgGrowthRate": {
          "type": "number",
          "description": "Average monthly growth rate"
        },
        "totalMovements": {
          "type": "integer",
          "description": "Total number of transactions"
        }
      }
    },
    "BalanceHistoryEntryResponse": {
      "type": "object",
      "required": [
        "period",
        "openingBalance",
        "closingBalance",
        "profit",
        "growthRate",
        "movementCount",
        "isGapPeriod",
        "isOfficialReport",
        "productId",
        "productName",
        "isProfitable",
        "isLoss",
        "totalDebits",
        "totalCredits"
      ],
      "properties": {
        "period": {
          "type": "string",
          "pattern": "^\\d{4}-\\d{2}$",
          "description": "Period in YYYY-MM format"
        },
        "openingBalance": {
          "type": "number"
        },
        "closingBalance": {
          "type": "number"
        },
        "profit": {
          "type": "number"
        },
        "growthRate": {
          "type": "number"
        },
        "movementCount": {
          "type": "integer"
        },
        "isGapPeriod": {
          "type": "boolean"
        },
        "isOfficialReport": {
          "type": "boolean"
        },
        "productId": {
          "type": "string",
          "format": "uuid"
        },
        "productName": {
          "type": "string"
        },
        "isProfitable": {
          "type": "boolean"
        },
        "isLoss": {
          "type": "boolean"
        },
        "totalDebits": {
          "type": "number"
        },
        "totalCredits": {
          "type": "number"
        }
      }
    }
  }
}
```

---

## Example JSON Response

```json
{
  "summary": {
    "totalBalance": 15250.75,
    "periodChange": 2500.00,
    "periodChangePercent": 19.63,
    "avgGrowthRate": 3.27,
    "totalMovements": 45
  },
  "balances": [
    {
      "period": "2024-01",
      "openingBalance": 10000.00,
      "closingBalance": 10500.00,
      "profit": 500.00,
      "growthRate": 5.00,
      "movementCount": 12,
      "isGapPeriod": false,
      "isOfficialReport": true,
      "productId": "550e8400-e29b-41d4-a716-446655440000",
      "productName": "Savings Account",
      "isProfitable": true,
      "isLoss": false,
      "totalDebits": 2000.00,
      "totalCredits": 2500.00
    },
    {
      "period": "2024-02",
      "openingBalance": 10500.00,
      "closingBalance": 11200.00,
      "profit": 700.00,
      "growthRate": 6.67,
      "movementCount": 15,
      "isGapPeriod": false,
      "isOfficialReport": true,
      "productId": "550e8400-e29b-41d4-a716-446655440000",
      "productName": "Savings Account",
      "isProfitable": true,
      "isLoss": false,
      "totalDebits": 1500.00,
      "totalCredits": 2200.00
    }
  ]
}
```

---

## Empty Response

When no data is available:

```json
{
  "summary": {
    "totalBalance": 0,
    "periodChange": 0,
    "periodChangePercent": 0,
    "avgGrowthRate": 0,
    "totalMovements": 0
  },
  "balances": []
}
```

---

## Naming Convention Compliance

✅ **CORRECT NAMING:**

- `BalanceHistoryResponse` - Main response object
- `BalanceHistorySummaryResponse` - Nested summary object
- `BalanceHistoryEntryResponse` - Individual balance entry

---


