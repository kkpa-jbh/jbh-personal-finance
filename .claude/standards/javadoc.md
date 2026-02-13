# Use Case Documentation Standard

All Use Case **interfaces** (in `*.application.feature.<feature>.usecases` package) MUST be documented with comprehensive JavaDoc following this structure.

---

## Important Notes

- **Document ONLY the use case interface**, NOT the implementation (InputPort classes)
- User Explanation should be written as if speaking directly to the end user
- Database Operations should list ALL database changes (INSERT/UPDATE/DELETE)
- For read-only queries, use "SELECT: [what is queried]" or "None (reads from registry)"
- Validations should include ALL checks performed by the use case

---

## Class-Level Documentation

Use this template for the **use case interface** class:

```java
/**
 * [Technical purpose - what this use case does]
 *
 * <p><strong>User Explanation:</strong> "[User-friendly explanation for FE display -
 * describe the action in simple terms as if explaining to end user]"
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>[Key business rule 1]
 *   <li>[Key business rule 2]
 *   <li>[Additional rules as needed]
 * </ul>
 */
public interface CreateProductUseCase {
  // methods
}
```

### Class-Level Template Breakdown

#### 1. Technical Purpose (First Paragraph)
Write a concise technical description of what the use case does.

**Examples:**
- "Handles the creation of new financial products for users"
- "Manages the addition of movements to existing products"
- "Retrieves monthly balance history for specified products and time periods"
- "Processes transfer operations between two products"

#### 2. User Explanation (Second Paragraph)
Describe the functionality in user-friendly terms as if explaining to a front-end developer or end user.

Start with: `<p><strong>User Explanation:</strong> "`

**Examples:**
- "Allows users to create a new savings account, credit card, or investment account in the system"
- "Records a deposit, withdrawal, or purchase transaction on an account"
- "Shows how an account's balance changed over time, month by month"
- "Moves money from one account to another, updating both balances automatically"

#### 3. Business Rules (Third Section)
List the key business rules enforced by the use case.

Start with: `<p><strong>Business Rules:</strong>`

Use an unordered list (`<ul><li>`) for each rule.

**Examples:**
```java
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>Users can only create products for themselves
 *   <li>Product names must be unique per user
 *   <li>Initial balance must be zero or positive
 *   <li>Product type must be valid (SAVINGS, CREDIT_CARD, INVESTMENT)
 * </ul>
```

---

## Method-Level Documentation

Use this template for **each method** in the use case interface:

```java
/**
 * [Brief description of what the method does]
 *
 * <p><strong>Validations:</strong>
 *
 * <ul>
 *   <li>[Validation 1 - e.g., "Command cannot be null"]
 *   <li>[Validation 2 - e.g., "User must own the product"]
 *   <li>[Additional validations]
 * </ul>
 *
 * <p><strong>Database Operations:</strong>
 *
 * <ul>
 *   <li>INSERT: [Tables/entities created - e.g., "New movement record"]
 *   <li>UPDATE: [Tables/entities updated - e.g., "Product balance and net flow"]
 *   <li>DELETE: [Tables/entities deleted - e.g., "Sets deleted_at timestamp"]
 * </ul>
 *
 * @param [param] [description]
 * @return [description]
 * @throws BusinessException [when/why exception is thrown]
 */
ReturnType methodName(ParamType param) throws BusinessException;
```

### Method-Level Template Breakdown

#### 1. Brief Description (First Paragraph)
One-sentence summary of what the method does.

**Examples:**
- "Creates a new financial product for the specified user"
- "Adds a movement to an existing product and updates balances"
- "Retrieves movements for a product within a date range"
- "Processes a transfer between two products"

#### 2. Validations Section
List ALL validation checks performed before executing business logic.

Start with: `<p><strong>Validations:</strong>`

Use an unordered list for each validation.

**Examples:**
```java
 * <p><strong>Validations:</strong>
 *
 * <ul>
 *   <li>User ID cannot be null
 *   <li>Product ID cannot be null
 *   <li>Command cannot be null
 *   <li>User must own the product
 *   <li>Product must be active (not deleted)
 *   <li>Movement amount must be positive
 *   <li>Movement date cannot be in the future
 *   <li>Category must be valid for the movement type
 * </ul>
```

#### 3. Database Operations Section
Document ALL database changes made by the use case.

Start with: `<p><strong>Database Operations:</strong>`

Use these prefixes:
- **INSERT:** for new records
- **UPDATE:** for modified records
- **DELETE:** for deleted records (or soft deletes)
- **SELECT:** for read-only queries (if no mutations)
- **None:** if no database operations (e.g., reads from in-memory registry)

**Examples:**

**For a Create Operation:**
```java
 * <p><strong>Database Operations:</strong>
 *
 * <ul>
 *   <li>INSERT: New product record in products table
 *   <li>INSERT: Initial balance entry in monthly_balances table
 * </ul>
```

**For an Update Operation:**
```java
 * <p><strong>Database Operations:</strong>
 *
 * <ul>
 *   <li>INSERT: New movement record in movements table
 *   <li>UPDATE: Product balance and net_flow fields
 *   <li>UPDATE: Monthly balance snapshot for current period
 * </ul>
```

**For a Delete Operation:**
```java
 * <p><strong>Database Operations:</strong>
 *
 * <ul>
 *   <li>DELETE: Sets deleted_at timestamp on product record (soft delete)
 *   <li>DELETE: Sets deleted_at timestamp on all related movements
 * </ul>
```

**For a Read-Only Operation:**
```java
 * <p><strong>Database Operations:</strong>
 *
 * <ul>
 *   <li>SELECT: Queries movements table filtered by product_id and date range
 * </ul>
```

**For Registry Lookups:**
```java
 * <p><strong>Database Operations:</strong>
 *
 * <ul>
 *   <li>None (reads from in-memory product type registry)
 * </ul>
```

#### 4. JavaDoc Tags
Standard JavaDoc parameter, return, and exception documentation.

**@param:** Describe each parameter
```java
 * @param userId the unique identifier of the user
 * @param productId the unique identifier of the product
 * @param command the command object containing movement details
```

**@return:** Describe what is returned
```java
 * @return DTO containing the created product details
 * @return DTO containing movement ID and updated balance
 * @return List of movement DTOs for the specified period
```

**@throws:** Describe when/why exceptions are thrown
```java
 * @throws BusinessException if user does not own the product
 * @throws BusinessException if product is not active
 * @throws ValidationException if command validation fails
 * @throws GenericSpecificationException if required parameters are null
```

---

## Complete Examples

### Example 1: Create Operation

```java
package com.jbh.finance.application.feature.product.usecases;

/**
 * Handles the creation of new financial products for users.
 *
 * <p><strong>User Explanation:</strong> "Allows users to create a new savings account,
 * credit card, or investment account in the system. Each product tracks transactions
 * and maintains a balance history."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>Users can only create products for themselves
 *   <li>Product names must be unique per user
 *   <li>Initial balance must be zero or positive
 *   <li>Product type must be valid (SAVINGS, CREDIT_CARD, INVESTMENT)
 *   <li>Each product must have a valid currency
 * </ul>
 */
public interface CreateProductUseCase {

  /**
   * Creates a new financial product for the specified user.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>User ID cannot be null
   *   <li>Command cannot be null
   *   <li>Product name must be unique for the user
   *   <li>Product type must exist in the system
   *   <li>Initial balance must not be negative
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>INSERT: New product record in products table
   *   <li>INSERT: Initial balance entry in monthly_balances table
   * </ul>
   *
   * @param userId the unique identifier of the user creating the product
   * @param command the command object containing product creation details
   * @return DTO containing the created product details including generated ID
   * @throws BusinessException if product name already exists for user
   * @throws ValidationException if command validation fails
   * @throws GenericSpecificationException if userId or command is null
   */
  ProductDTO createProduct(UUID userId, CreateProductCommand command)
      throws BusinessException;
}
```

### Example 2: Update Operation

```java
package com.jbh.finance.application.feature.movement.usecases;

/**
 * Manages the addition of movements to existing products.
 *
 * <p><strong>User Explanation:</strong> "Records a deposit, withdrawal, or purchase
 * transaction on an account. Automatically updates the account balance and maintains
 * a history of all transactions."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>Users can only add movements to their own products
 *   <li>Movement date cannot be in the future
 *   <li>Movement amount must be positive
 *   <li>Product must be active (not deleted)
 *   <li>Balance snapshot is recalculated after each movement
 *   <li>Category must be valid for the movement type
 * </ul>
 */
public interface AddMovementUseCase {

  /**
   * Adds a movement to an existing product and updates balances.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>User ID cannot be null
   *   <li>Product ID cannot be null
   *   <li>Command cannot be null
   *   <li>User must own the product
   *   <li>Product must be active (not deleted)
   *   <li>Movement amount must be positive
   *   <li>Movement date cannot be in the future
   *   <li>Category must be valid for the movement type
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>INSERT: New movement record in movements table
   *   <li>UPDATE: Product balance and net_flow fields
   *   <li>UPDATE: Monthly balance snapshot for current period
   * </ul>
   *
   * @param userId the unique identifier of the user
   * @param productId the unique identifier of the product
   * @param command the command object containing movement details
   * @return DTO containing movement ID and updated balance information
   * @throws BusinessException if user does not own the product
   * @throws BusinessException if product is not active
   * @throws ValidationException if command validation fails
   * @throws GenericSpecificationException if required parameters are null
   */
  AddBasicMovementDTO addMovement(UUID userId, ProductId productId, AddMovementCommand command)
      throws BusinessException;
}
```

### Example 3: Read Operation

```java
package com.jbh.finance.application.feature.movement.usecases;

/**
 * Retrieves movements for products within a specified date range.
 *
 * <p><strong>User Explanation:</strong> "Shows all transactions (deposits, withdrawals,
 * purchases) for an account during a specific time period. Helps users track their
 * spending and income over time."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>Users can only view movements for their own products
 *   <li>Date range is inclusive on both ends
 *   <li>Returns movements ordered by date (newest first)
 *   <li>Includes soft-deleted movements if specified
 * </ul>
 */
public interface FindMovementsUseCase {

  /**
   * Retrieves movements for a product within a date range.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>User ID cannot be null
   *   <li>Product ID cannot be null
   *   <li>Start date cannot be null
   *   <li>End date cannot be null
   *   <li>End date must be after or equal to start date
   *   <li>User must own the product
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>SELECT: Queries movements table filtered by product_id and date range
   *   <li>SELECT: Verifies product ownership from products table
   * </ul>
   *
   * @param userId the unique identifier of the user
   * @param productId the unique identifier of the product
   * @param startDate the start date of the range (inclusive)
   * @param endDate the end date of the range (inclusive)
   * @return List of movement DTOs for the specified period, ordered by date descending
   * @throws BusinessException if user does not own the product
   * @throws ValidationException if date range is invalid
   * @throws GenericSpecificationException if required parameters are null
   */
  List<MovementDTO> findMovements(
      UUID userId,
      ProductId productId,
      LocalDate startDate,
      LocalDate endDate) throws BusinessException;
}
```

### Example 4: Delete Operation

```java
package com.jbh.finance.application.feature.product.usecases;

/**
 * Handles soft deletion of products and their associated data.
 *
 * <p><strong>User Explanation:</strong> "Allows users to remove an account from their
 * active list. The account and its transaction history are preserved in the system but
 * hidden from normal views."
 *
 * <p><strong>Business Rules:</strong>
 *
 * <ul>
 *   <li>Users can only delete their own products
 *   <li>Deletion is soft (sets deleted_at timestamp)
 *   <li>All related movements are also soft-deleted
 *   <li>Deleted products can be restored by admin
 *   <li>Product must exist and be active
 * </ul>
 */
public interface DeleteProductUseCase {

  /**
   * Soft deletes a product and all its related movements.
   *
   * <p><strong>Validations:</strong>
   *
   * <ul>
   *   <li>User ID cannot be null
   *   <li>Product ID cannot be null
   *   <li>User must own the product
   *   <li>Product must be active (not already deleted)
   * </ul>
   *
   * <p><strong>Database Operations:</strong>
   *
   * <ul>
   *   <li>UPDATE: Sets deleted_at timestamp on product record (soft delete)
   *   <li>UPDATE: Sets deleted_at timestamp on all related movements
   * </ul>
   *
   * @param userId the unique identifier of the user
   * @param productId the unique identifier of the product to delete
   * @throws BusinessException if user does not own the product
   * @throws BusinessException if product is already deleted
   * @throws GenericSpecificationException if required parameters are null
   */
  void deleteProduct(UUID userId, ProductId productId) throws BusinessException;
}
```

---

## Quality Checklist

When documenting a use case, verify:

- [ ] Class-level JavaDoc includes all three sections (Technical Purpose, User Explanation, Business Rules)
- [ ] User Explanation is written in user-friendly language
- [ ] Business Rules list ALL key rules enforced
- [ ] Method-level JavaDoc includes Validations section
- [ ] Method-level JavaDoc includes Database Operations section
- [ ] ALL database changes are documented (INSERT/UPDATE/DELETE/SELECT)
- [ ] For read-only operations, "SELECT" is specified
- [ ] For registry lookups, "None (reads from registry)" is specified
- [ ] All parameters have @param tags with descriptions
- [ ] Return value has @return tag with description
- [ ] All thrown exceptions have @throws tags with when/why
- [ ] Only the **interface** is documented (NOT the InputPort implementation)
