# Frontend Component: Active Products List

These are the details about the API to call. Follow the current standards of the project and follow the best practices.

## API Specification

**Base Path**: `/jbh-api/finance/products`
**Authentication**: JWT token required in Authorization header

---

## Operation: GET All Active Products

- **Endpoint**: `GET /jbh-api/finance/products/`
- **Response**: Array of `ProductDTO`

```typescript
interface ProductDTO {
  id: ProductId;
  name: string;
  type: ProductType;
  userId: string; // UUID
  movementBalance: number; // BigDecimal
  currentBalance: number; // BigDecimal
  netProfitBalance: number; // BigDecimal
  isActive: boolean;
  createdAt: string; // ISO DateTime (LocalDateTime)
  updatedAt: string; // ISO DateTime (LocalDateTime)
  netGrowthRate: number; // BigDecimal
  metadata: ProductMetadata;
}

interface ProductId {
  value: string; // UUID
}

type ProductType =
  | 'SAVINGS'                  // {"en":"Savings","es":"Ahorros"}
  | 'REAL_ESTATE_INVESTMENT'   // {"en":"Real Estate Investment","es":"Inversión Inmobiliaria"}
  | 'LOAN'                     // {"en":"Loan","es":"Préstamo"}
  | 'CREDIT_CARD'              // {"en":"Credit Card","es":"Tarjeta de Crédito"}
  | 'INVESTMENT'               // {"en":"Investment","es":"Inversión"}
  | 'CDT';                     // {"en":"Certificate of Deposit (CDT)","es":"CDT"}

interface ProductMetadata {
  data: Record<ProductMetadataKey, unknown>;
}

type ProductMetadataKey =
  // Credit Card Metadata
  | 'CREDIT_LIMIT'                         // BigDecimal - {"en":"Credit Limit","es":"Límite de Crédito"}
  | 'PAYMENT_DUE_DAY'                      // Integer (1-31) - {"en":"Payment Due Day","es":"Día de Pago"}
  // Investment Metadata
  | 'BROKER_NAME'                          // String - {"en":"Broker Name","es":"Nombre del Corredor"}
  | 'COMMISSION_RATE'                      // BigDecimal (0-100) - {"en":"Commission Rate","es":"Tasa de Comisión"}
  // CDT Metadata
  | 'MATURITY_DATE'                        // Date - {"en":"Maturity Date","es":"Fecha de Vencimiento"}
  | 'OPENING_DATE'                         // Date - {"en":"Opening Date","es":"Fecha de Apertura"}
  | 'TERM_LENGTH_IN_DAYS'                  // Integer (1-3650) - {"en":"Term Length (Days)","es":"Plazo (Días)"}
  // Common Metadata (System Calculated)
  | 'COMMON_INITIAL_BALANCE'               // BigDecimal - {"en":"Initial Balance","es":"Saldo Inicial"}
  | 'COMMON_IS_FULLY_WITHDRAWN'            // Boolean - {"en":"Fully Withdrawn","es":"Retirado Completamente"}
  | 'COMMON_FULLY_WITHDRAWN_DATE'          // Date - {"en":"Fully Withdrawn Date","es":"Fecha de Retiro Completo"}
  | 'COMMON_FULLY_WITHDRAWN_AT'            // Date - {"en":"Fully Withdrawn At","es":"Retirado Completamente En"}
  // Loan Metadata
  | 'LOAN_PRINCIPAL_AMOUNT'                // BigDecimal - {"en":"Principal Amount","es":"Monto Principal"}
  | 'LOAN_INTEREST_RATE'                   // BigDecimal (0-100) - {"en":"Interest Rate","es":"Tasa de Interés"}
  | 'LOAN_TOTAL_AMOUNT_PAID'               // BigDecimal - {"en":"Total Amount Paid","es":"Monto Total Pagado"}
  | 'LOAN_PAYOFF_AMOUNT_TODAY'             // BigDecimal - {"en":"Payoff Amount Today","es":"Monto de Liquidación Hoy"}
  // Real Estate Metadata
  | 'REAL_ESTATE_PURCHASE_DATE'            // Date - {"en":"Purchase Date","es":"Fecha de Compra"}
  | 'REAL_ESTATE_PURCHASE_PRICE'           // BigDecimal - {"en":"Purchase Price","es":"Precio de Compra"}
  | 'REAL_ESTATE_PROPERTY_SIZE'            // BigDecimal - {"en":"Property Size","es":"Tamaño de la Propiedad"}
  | 'REAL_ESTATE_RENTAL_INCOME'            // BigDecimal - {"en":"Rental Income","es":"Ingreso por Alquiler"}
  | 'REAL_ESTATE_FINANCED_AMOUNT'          // BigDecimal - {"en":"Financed Amount","es":"Monto Financiado"}
  | 'REAL_ESTATE_DOWN_PAYMENT_AMOUNT'      // BigDecimal - {"en":"Down Payment Amount","es":"Monto de Cuota Inicial"}
  | 'REAL_ESTATE_DOWN_PAYMENT_PERCENTAGE'  // BigDecimal (0-100) - {"en":"Down Payment Percentage","es":"Porcentaje de Cuota Inicial"}
  | 'REAL_ESTATE_DOWN_PAYMENT_PAID_TO_DATE'; // BigDecimal - {"en":"Down Payment Paid to Date","es":"Cuota Inicial Pagada a la Fecha"}
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
- Consider mobile touch interactions for list items

### Component Structure

Create the following components:

1. **ProductListComponent**: Display all active products
2. **ProductCardComponent**: Individual product display card (reusable)
3. **ProductTypeIconComponent**: Icon based on product type
4. **EmptyProductsComponent**: Display when no products exist

### API Integration

- Create/Update the repository adapter implementing the current standard of the project
- Keep the existing structure of the repository layer

---

## Component Display Requirements

### ProductListComponent

**Purpose**: Display a list of all active products for the authenticated user.

**Features**:
- Fetch products on component initialization
- Display products in a card/list format
- Group or filter products by type (optional enhancement)
- Show loading skeleton while fetching
- Show empty state when no products exist

**UI Elements**:
- Header: "My Products" / "Mis Productos"
- Product cards showing key information
- Loading spinner/skeleton during data fetch

### ProductCardComponent

**Display Fields**:
| Field | Label (EN/ES) | Format |
|-------|---------------|--------|
| name | - | String |
| type | Product Type / Tipo de Producto | Use translationKey |
| currentBalance | Current Balance / Saldo Actual | Currency format |
| movementBalance | Movement Balance / Balance de Movimientos | Currency format |
| netProfitBalance | Net Profit / Ganancia Neta | Currency (green if positive, red if negative) |
| netGrowthRate | Growth Rate / Tasa de Crecimiento | Percentage with +/- indicator |
| updatedAt | Last Updated / Última Actualización | Relative time or date |

**Visual Indicators**:
- Product type icon (different icon per type)
- Color coding for positive/negative balances
- Badge for fully withdrawn products (check `metadata.data.COMMON_IS_FULLY_WITHDRAWN`)

---

## Empty State

When the API returns an empty array:

**Title (EN)**: "No Products Yet"
**Title (ES)**: "Sin Productos Aún"

**Message (EN)**: "You haven't created any products. Start by adding your first account or investment."
**Message (ES)**: "No has creado ningún producto. Comienza agregando tu primera cuenta o inversión."

---

## Loading State

- Show skeleton cards (3-4 placeholder cards)
- Each skeleton should match the ProductCard dimensions
- Animate with pulse/shimmer effect

---

## Additional Notes

### Product ID Handling

The `ProductId` is a wrapper object containing a UUID. Access the actual ID via `product.id.value`.

### Currency Formatting

- Use locale-aware currency formatting
- Display with appropriate decimal places for financial data

### Products That Allow Adding Movements

Only certain product types allow adding movements:
- SAVINGS
- CREDIT_CARD
- INVESTMENT
- CDT

(LOAN and REAL_ESTATE_INVESTMENT do not allow manual movement additions)
