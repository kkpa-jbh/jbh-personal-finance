# Frontend Component: Account Transfers

We need to create or update the component for Account Transfers.

If it's a new component, check what might be the best place to put it on the menu based on the context.
If you don't feel sure, ask the user by providing suggestions.

These are the details about the API(s) to call. Follow the current standards of the project and follow the best practices.

Check if the component already exists to update it.

## API Specification

**Base Path**: `/jbh-api/finance/products/transfers`
**Authentication**: JWT token required in Authorization header

### Operations to Implement

#### POST - Transfer Funds Between Accounts

- **Endpoint**: `POST /jbh-api/finance/products/transfers/?fromAccountId={uuid}`
- **Description**: Transfer funds between two JBH accounts owned by the same user
- **Query Parameter**:
    - `fromAccountId` (required): Source account ID (UUID)
- **Request Body**:

```typescript
interface AddTransferRequest {
  toAccountId: string;        // UUID - Destination product ID
  totalAmount: number;        // BigDecimal - Amount to transfer
  transferDate: string;       // LocalDate - Date of transfer (YYYY-MM-DD)
}
```

**Example Request Body**:

```json
{
  "toAccountId": "550e8400-e29b-41d4-a716-446655440001",
  "totalAmount": 150.50,
  "transferDate": "2026-02-05"
}
```

- **Response**: 204 No Content (successful transfer)

## Business Rules

1. **Same User Ownership**: Both source and destination accounts must belong to the authenticated user
2. **Valid Accounts**: Both `fromAccountId` and `toAccountId` must be valid UUIDs of existing accounts
3. **Amount Validation**: `totalAmount` must be greater than zero
4. **Date Validation**: `transferDate` should not be in the future (optional client-side validation)
5. **Sufficient Balance**: The source account must have sufficient balance (validated on backend)

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
- Use decimal/Date/currency directives or pipes where they apply
- Register all icons used in the component by adding them to ionic
- Ask the user if the component needs a button to refresh the info. Follow the pattern for that.

### Directives or Pipes

- **CRITICAL**: Apply the directive `jbhDecimalFormat` for the `totalAmount` field (BigDecimal)
- **CRITICAL**: Apply the pipe `jbhDate` for the `transferDate` field (LocalDate format: YYYY-MM-DD)
- **CRITICAL**: Apply the currency pipe `jbhCurrency` for displaying amount values

### Multi-Platform Requirements

- Design must work on Desktop, Android, and iOS with ionic/capacitor
- Use responsive breakpoints
- Consider mobile keyboard behavior for forms
- Use numeric keyboard for amount input on mobile

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
- The repository method should be named something like: `transferFunds(fromAccountId: string, request: AddTransferRequest): Promise<void>`

### Form Fields

#### Source Account (fromAccountId)

- **Type**: Dropdown/Select
- **Label**: "From Account" (EN) / "Desde Cuenta" (ES)
- **Validation**: Required, must be a valid account owned by the user
- **Display**: Show account name/description with current balance

#### Destination Account (toAccountId)

- **Type**: Dropdown/Select
- **Label**: "To Account" (EN) / "Hacia Cuenta" (ES)
- **Validation**: Required, must be a valid account owned by the user, must be different from source account
- **Display**: Show account name/description

#### Amount (totalAmount)

- **Type**: Number input with decimal support
- **Label**: "Amount" (EN) / "Monto" (ES)
- **Validation**: Required, must be greater than 0, maximum 2 decimal places
- **Directives**: Apply `jbhDecimalFormat`
- **Display**: Show currency symbol based on user preferences

#### Transfer Date (transferDate)

- **Type**: Date picker
- **Label**: "Transfer Date" (EN) / "Fecha de Transferencia" (ES)
- **Validation**: Required, should not be in the future
- **Default**: Today's date
- **Format**: YYYY-MM-DD for API, localized display for user
- **Pipes**: Apply `jbhDate` when displaying

### UI/UX Recommendations

1. **Account Dropdowns**:
    - Filter out the selected source account from the destination account dropdown
    - Show current balance next to source account name
    - Disable "Transfer" button if source account has insufficient balance

2. **Amount Input**:
    - Show remaining balance after transfer amount is entered
    - Highlight in red if amount exceeds available balance

3. **Success Feedback**:
    - Show success toast/message: "Transfer completed successfully" (EN) / "Transferencia completada exitosamente" (ES)
    - Option to navigate back to accounts list or create another transfer

4. **Error Handling**:
    - Display clear error messages for validation failures
    - Handle 400 errors (invalid request)
    - Handle 401 errors (unauthorized)

5. **Loading State**:
    - Disable form and show loading spinner during API call
    - Prevent multiple submissions

### Translations

#### English (EN)

```json
{
  "transfers.title": "Transfer Funds",
  "transfers.fromAccount": "From Account",
  "transfers.toAccount": "To Account",
  "transfers.amount": "Amount",
  "transfers.transferDate": "Transfer Date",
  "transfers.submit": "Transfer",
  "transfers.cancel": "Cancel",
  "transfers.success": "Transfer completed successfully",
  "transfers.error.sameAccount": "Source and destination products must be different",
  "transfers.error.insufficientBalance": "Insufficient balance in source product",
  "transfers.error.invalidAmount": "Amount must be greater than zero",
  "transfers.availableBalance": "Available Balance"
}
```

#### Spanish (ES)

```json
{
  "transfers.title": "Transferir Fondos",
  "transfers.fromAccount": "Desde Cuenta",
  "transfers.toAccount": "Hacia Cuenta",
  "transfers.amount": "Monto",
  "transfers.transferDate": "Fecha de Transferencia",
  "transfers.submit": "Transferir",
  "transfers.cancel": "Cancelar",
  "transfers.success": "Transferencia completada exitosamente",
  "transfers.error.sameAccount": "Las cuentas de origen y destino deben ser diferentes",
  "transfers.error.insufficientBalance": "Saldo insuficiente en la cuenta de origen",
  "transfers.error.invalidAmount": "El monto debe ser mayor que cero",
  "transfers.availableBalance": "Saldo Disponible"
}
```

## Additional Notes

- **No Response Body**: The API returns 204 No Content on success, no need to parse response body
- **Query Parameter**: Don't forget that `fromAccountId` is a query parameter, not part of the request body
- **Account List Refresh**: After successful transfer, consider refreshing the accounts list to show updated balances
- **Transfer History**: Consider if this component should show a list of recent transfers or just the form
- **Validation Order**: Validate client-side first (required fields, positive amount, different accounts) before making API call

## Critical Rules

- Do not implement state management
- Do not forget to register the Ionic icons used by the HTML
- The error handling is already implemented, do not mention it
- Do not suggest adding unit tests. The project does not support that
- The application only supports EN and ES languages. Be aware about the translations with good grammar
- Always use the correct directives and pipes: `jbhDecimalFormat`, `jbhDate`, `jbhCurrency`

---

**Feel free to ask any questions or request clarification on any aspect of this implementation. If there are existing patterns for account selection dropdowns or similar forms in
the project, let me know and I'll adjust the implementation approach accordingly.**
