# API Models Inventory

This document provides a complete inventory of all REST API endpoints with their corresponding Request and Response models.

## Summary

| API Path | Request Model | Response Model |
|----------|---------------|----------------|
| DELETE /jbh-api/finance/products/{productId} | None | None |
| GET /jbh-api/finance/categories/expenses | None | CategoryResponse |
| GET /jbh-api/finance/categories/incomes | None | CategoryResponse |
| GET /jbh-api/finance/product-types | None | ProductTypeResponse |
| GET /jbh-api/finance/products/ | None | ProductResponse |
| GET /jbh-api/finance/products/metadata-config | None | MetadataFieldConfigResponse |
| GET /jbh-api/finance/products/movements/{productId} | None | MovementResponse |
| GET /jbh-api/finance/products/{productId} | None | ProductResponse |
| GET /jbh-api/preferences/team-preferences/v1/{teamId} | None | TeamPreferencesResponse |
| GET /jbh-api/preferences/v1 | None | UserPreferencesResponse |
| PATCH /jbh-api/finance/products/{productId}/status | UpdateProductStatusRequest | ProductResponse |
| POST /jbh-api/finance/products/ | CreateProductRequest | ProductResponse |
| POST /jbh-api/finance/products/monthly-balances/ | MonthlyBalanceRequest | BalanceHistoryResponse |
| POST /jbh-api/finance/products/monthly-balances/{productId} | MonthlyBalanceRequest | BalanceHistoryResponse |
| POST /jbh-api/finance/products/monthly-balances/{productId}/register | RegisterMonthlyBalanceRequest | MonthlyBalanceResponse |
| POST /jbh-api/finance/products/movements/upload-excel | FileUpload (multipart) | ApiResponse\<Boolean\> |
| POST /jbh-api/finance/products/movements/{productId} | AddMovementRequest | AddBasicMovementResponse |
| POST /jbh-api/finance/products/transfers/ | AddTransferRequest | None |
| POST /jbh-api/finance/products/{productId}/liquidate | LiquidateProductRequest | LiquidationResultResponse |
| POST /jbh-api/notifications/v1 | SendNotificationCommand | NotificationResponse |
| POST /jbh-api/preferences/team-preferences/v1 | CreateTeamPreferencesRequest | TeamPreferencesResponse |
| POST /jbh-api/preferences/v1 | None | UserPreferencesResponse |
| PUT /jbh-api/finance/products/{productId} | EditProductRequest | ProductResponse |
| PUT /jbh-api/finance/products/{productId}/metadata | UpdateProductMetadataRequest | None |
| PUT /jbh-api/preferences/team-preferences/v1/{teamId} | UpdateTeamPreferencesRequest | TeamPreferencesResponse |
| PUT /jbh-api/preferences/v1 | UpdatePreferencesRequest | UserPreferencesResponse |

## Naming Convention Status

✅ **All naming violations have been fixed!**

All API endpoints now follow the proper naming conventions:
- Request objects use `*Request` or `*Command` suffix
- Response objects use `*Response` suffix
- DTOs are properly isolated in the application layer and mapped to Response objects

### Changes Applied

1. **Preferences Module** ✅
   - Created `UserPreferencesResponse` in `jbh-preferences-infra/adapters/in/rest/vo/`
   - Created `TeamPreferencesResponse` in `jbh-preferences-infra/adapters/in/rest/vo/`
   - Updated REST adapters to map from DTOs to Response objects

2. **Notification Module** ✅
   - Renamed `SendNotificationRequest` to `SendNotificationCommand` (shared contract in `-contracts` module)
   - Command pattern is acceptable for shared contracts between modules

3. **Finance Module - Balance History** ✅
   - Created `BalanceHistoryResponse` in `jbh-finance-infra/adapters/in/rest/balancehistory/response/`
   - Created `BalanceHistorySummaryResponse` in `jbh-finance-infra/adapters/in/rest/balancehistory/response/`
   - Created `BalanceHistoryEntryResponse` in `jbh-finance-infra/adapters/in/rest/balancehistory/response/`
   - Updated `MonthlyBalanceRestAdapter` to map from DTOs to Response objects

---

## Naming Standards Reference

### Request Objects
- **Pattern:** `<ActionName><EntityName>Request`
- **Location:** `*.infra.adapters.in.rest.<feature>.request/`
- **Examples:** AddMovementRequest, CreateProductRequest, UpdateProductStatusRequest

### Command Objects (Shared Contracts)
- **Pattern:** `<ActionName><EntityName>Command`
- **Location:** `*-contracts/` (for inter-module communication)
- **Examples:** SendNotificationCommand

### Response Objects
- **Pattern:** `<EntityName>Response` or `<EntityName>ApiResponse`
- **Location:** `*.infra.adapters.in.rest.<feature>.response/`
- **Examples:** MovementResponse, ProductResponse, MonthlyBalanceResponse, BalanceHistoryResponse

### DTOs (Internal Use Only)
- **Pattern:** `<EntityName>DTO`
- **Location:** `*.application.feature.<feature>.dto/`
- **Rule:** Must NEVER be returned directly from API controllers
- **Usage:** Internal data transfer between application layers only
- **Mapping:** Always map to Response objects using `fromDTO()` factory methods

---

*Last updated: 2026-02-14*
*Status: ✅ All naming conventions compliant*
