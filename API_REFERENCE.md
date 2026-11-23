# Backend REST API Inventory
**Repo:** `BE-AI-driven-recipe-and-ingredient-marketplace-for-bakers`
**Base package:** `com.manhnv.capstonebakerai`
**Regenerated:** 2026-11-19, read directly from controller/DTO source (not from any prior copy of this file).

**Response envelope:** every endpoint below wraps its payload in `ApiResponse<T>`:
```json
{ "message": "string | null", "success": true, "data": { /* T */ } }
```
(`ApiResponse.success(data)` sets `success=true`, omits `message` (`@JsonInclude(NON_NULL)`); `ApiResponse.error(message)` sets `success=false`.)
Exceptions that do **not** follow this envelope:
- `PayOSWebhookController.handleWebhook` → raw `String` `"OK"`.
- The entire `warehouse/*` module returns **raw JPA `@Entity` objects** (`StockInNote`, `StockOutNote`, `StocktakeNote`, `StockLot`, and their nested `StockInItem`/`StockOutItem`/`StocktakeItem`) wrapped only in `ApiResponse<T>` — not hand-written DTOs. Treat their field shapes as unstable/implementation detail; see the Warehouse sections below for the exact fields as currently declared on the entity classes.
- `ProductController.setComboItems` echoes the request type (`List<ComboItemRequest>`), not a dedicated response DTO; `ProductController.comboStock` returns a raw `BigDecimal`.

**Global auth rules** (`identity/config/SecurityConfig.java`), on top of any per-endpoint `@PreAuthorize`:
- Public (`permitAll`, no token): `POST /api/v1/auth/{register,verify-email,resend-verification,login,refresh,logout,forgot-password,forgot-password/verify,reset-password}`, `POST /api/v1/media/videos/webhook`, `/api/v1/notifications/**`, `/api/v1/payos/webhook`, `/ws/**`, Swagger/OpenAPI paths (`/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`, `/swagger-resources/**`, `/webjars/**`).
- `/api/v1/admin/accounts`, `/api/v1/admin/accounts/**` — `hasRole('ADMIN')`.
- `/api/v1/admin/customers`, `/api/v1/admin/customers/**` — `hasRole('ADMIN')` (this also covers `AdminLoyaltyController`, whose base path is `/api/v1/admin/customers/{customerId}/loyalty`).
- `POST /api/products/low-stock/alerts` — `hasAnyRole('MANAGER','STAFF')` (the only secured path anywhere under `/api/products/**` or `/api/product-categories/**`).
- `/api/v1/users/me`, `/api/v1/users/me/**` — `authenticated()` (any role).
- `/api/v1/customers/me`, `/api/v1/customers/me/**` — `hasRole('CUSTOMER')` (this also covers `CustomerLoyaltyController` at `/api/v1/customers/me/loyalty` and `RecipeInteractionController` at `/api/v1/customers/me/interaction-history`).
- `/api/v1/recommendations/**` — `hasRole('CUSTOMER')`.
- `/api/v1/admin/recommendations`, `/api/v1/admin/recommendations/**` — `hasRole('ADMIN')`.
- `/api/v1/management/ingredient-catalog`, `/api/v1/management/ingredient-catalog/**` — `hasRole('BAKER_STAFF')`.
- `/api/v1/recipe-suggestions`, `/api/v1/recipe-suggestions/**` — `hasRole('CUSTOMER')`.
- `/api/v1/admin/hobbies`, `/api/v1/admin/hobbies/**` — `hasRole('ADMIN')`.
- `GET /api/v1/hobbies` — `hasAnyRole('CUSTOMER','ADMIN')`.
- Everything else not explicitly listed — `anyRequest().authenticated()` (any authenticated role, enforced only by each controller's own `@PreAuthorize`, if any).
- JWT roles arrive as the `role` claim, converted to Spring authorities with the `ROLE_` prefix (`JwtGrantedAuthoritiesConverter`). Roles enum (`user/enums/Role.java`): `ADMIN, MANAGER, STAFF, BAKER_STAFF, CUSTOMER`.
- `X-Auth-Request: 1` header is required by `AuthController` application code (not by `SecurityConfig`) on `POST /api/v1/auth/refresh` and `POST /api/v1/auth/logout` — a missing/invalid header throws `IdentityErrorCode.INVALID_AUTH_REQUEST` before the refresh-cookie logic runs.
- CORS: allowed methods `GET, POST, PUT, PATCH, DELETE, OPTIONS`; allowed headers `Authorization, Content-Type, X-Auth-Request`; credentials allowed; origin from `app.cors.allowed-origin`.
- `PayOSWebhookController` (`POST /api/v1/payos/webhook`) **is** in the public matcher list — no bug here (previously flagged, now confirmed fixed and current).

---

## 1. Identity — `identity/controller/AuthController.java`
Base path: `/api/v1/auth`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `/register` | `RegisterUserRequest{fullName(NotBlank, max 100), email(NotBlank, Email, max 255), password(NotBlank, 8-72 chars), phone?(max 20), address?(max 255), dateOfBirth?(max 10, string), gender?(max 20, string)}` | `RegisterUserResponse{id, fullName, email, role, phone?, status, address?, dateOfBirth?, gender?, createdAt}` | Public | Self-register as CUSTOMER only; creates User+Customer, sends verification email |
| POST | `/verify-email` | `VerifyEmailRequest{email(NotBlank, Email), code(NotBlank, Pattern \d{6})}` | `MessageResponse{message}` | Public | Confirm email via 6-digit code |
| POST | `/resend-verification` | `ResendVerificationRequest{email(NotBlank, Email)}` | `MessageResponse{message}` | Public | Resend email verification code |
| POST | `/forgot-password` | `ForgotPasswordRequest{email(NotBlank, Email)}` | `MessageResponse{message}` | Public | Request password-reset code (same response regardless of account existence) |
| POST | `/forgot-password/verify` | `VerifyPasswordResetCodeRequest{email(NotBlank, Email), code(NotBlank, Pattern \d{6})}` | `MessageResponse{message}` | Public | Verify reset code without consuming it |
| POST | `/reset-password` | `ResetPasswordRequest{email(NotBlank, Email), code(NotBlank, Pattern \d{6}), newPassword(NotBlank, 8-72 chars)}` | `MessageResponse{message}` | Public | Reset password, revokes refresh-token chain, clears refresh cookie |
| POST | `/login` | `LoginRequest{email(NotBlank, Email), password(NotBlank)}` | `LoginResponse{accessToken, tokenType, expiresIn(long, seconds), userId, email, role}` (+ sets `refresh_token` HttpOnly cookie, path `/api/v1/auth`) | Public | Login; access token + refresh cookie |
| POST | `/refresh` | Cookie `refresh_token`(optional); Header `X-Auth-Request: 1`(required) | `LoginResponse` (+ rotates refresh cookie) | Public (requires app-level header check; throws `INVALID_AUTH_REQUEST` if missing/not `"1"`) | Rotate access/refresh token pair |
| POST | `/logout` | Cookie `refresh_token`(optional); Header `X-Auth-Request: 1`(required) | `MessageResponse{message}` | Public (requires app-level header check) | Revoke refresh-token chain if cookie present, clear cookie |

## 2. Identity — Admin account management — `user/controller/AdminAccountController.java`
Base path: `/api/v1/admin/accounts`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `CreateStaffAccountRequest{fullName(NotBlank, max 100), email(NotBlank, Email, max 255), role(NotNull; STAFF\|MANAGER\|BAKER_STAFF), phone?(max 20), address?(max 255), dateOfBirth?(max 10), gender?(max 20)}` (201) | `StaffAccountResponse{id, fullName, email, role, status, phone?, address?, dateOfBirth?, gender?, avatarUrl?, createdAt, updatedAt}` | `hasRole('ADMIN')` (URL-level) | Create STAFF/MANAGER/BAKER_STAFF account; no password supplied, instructions emailed via forgot-password flow |
| GET | `` | query `status?(ACTIVE\|BLOCKED\|INACTIVE)` | `List<StaffAccountResponse>` (no paging) | `hasRole('ADMIN')` | List staff/manager/baker_staff accounts |
| GET | `/{id}` | path `id` | `StaffAccountResponse` | `hasRole('ADMIN')` | Get one staff account |
| PUT | `/{id}` | `UpdateStaffAccountRequest{fullName(NotBlank, max 100), role(NotNull; STAFF\|MANAGER\|BAKER_STAFF), phone?(max 20), address?(max 255), dateOfBirth?(max 10), gender?(max 20)}` | `StaffAccountResponse` | `hasRole('ADMIN')` | Update staff account; revokes refresh tokens on role/status change |
| PATCH | `/{id}/status` | `UpdateAccountStatusRequest{status(NotNull; ACTIVE\|BLOCKED\|INACTIVE)}` | `StaffAccountResponse` | `hasRole('ADMIN')` | Change account status; revokes refresh tokens |

## 3. User — `user/controller/UserProfileController.java`
Base path: `/api/v1/users/me`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | — | `UserProfileResponse{id, fullName, email, role, status, phone?, address?, dateOfBirth?, gender?, avatarUrl?, createdAt, updatedAt}` | `authenticated()` (URL-level, any role) | Get own profile from access token |
| PUT | `` | `UpdateUserProfileRequest{fullName(max 100), phone?(max 20), address?(max 255), dateOfBirth?(max 10), gender?(max 20)}` | `UserProfileResponse` | `authenticated()` | Update own profile (fullName required; phone/address/dateOfBirth/gender null → cleared) |
| PUT | `/avatar` (multipart) | form field `file`(MultipartFile, required; JPEG/PNG/WebP, max 5 MB) | `UserProfileResponse` | `authenticated()` | Upload/replace avatar (old avatar cleaned from R2) |
| DELETE | `/avatar` | — | `UserProfileResponse` | `authenticated()` | Remove avatar URL + clean R2 object |

**`user/dto/UserSummaryResponse.java`**: `{id, fullName, avatarUrl}` — lightweight actor summary embedded as `createdBy`/`updatedBy`/`handledBy` across many other modules' responses (Order, Expense Voucher, Receipt Voucher, Product unit price history, Request Quote, Voucher, Stock Transaction, Supplier Payment). **Not** the same class as `recipe/dto/response/UserSummaryResponse.java` (see §24 below).

## 4. Customer — `customer/controller/CustomerProfileController.java`
Base path: `/api/v1/customers/me`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | — | `CustomerProfileResponse{customerId, skillLevel?(BEGINNER\|INTERMEDIATE\|ADVANCED), emailVerified, hobbies: HobbyResponse[]}` | `hasRole('CUSTOMER')` (URL-level) | Get own customer profile |
| PUT | `` | `UpdateCustomerProfileRequest{skillLevel?(BEGINNER\|INTERMEDIATE\|ADVANCED)}` | `CustomerProfileResponse` | `hasRole('CUSTOMER')` | Update skill level (null clears it) |
| PUT | `/hobbies` | `UpdateCustomerHobbiesRequest{hobbyIds(NotNull, Set<UUID>)}` | `CustomerProfileResponse` | `hasRole('CUSTOMER')` | Replace own hobby list wholesale (`[]` clears all; every id must be an active hobby) |

## 5. Customer — `customer/controller/CustomerAddressController.java`
Base path: `/api/v1/customers/me/addresses`
Class-level `@PreAuthorize("hasRole('CUSTOMER')")` (redundant with, and consistent with, the `/api/v1/customers/me/**` URL matcher).

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | — | `List<CustomerAddressResponse{id, receiverName, receiverPhone, provinceCode, districtCode, wardCode, addressDetail, fullAddress, defaultAddress(boolean), createdAt}>` (no paging) | `hasRole('CUSTOMER')` | List own saved addresses |
| POST | `` | `CustomerAddressRequest{receiverName(NotBlank, max 150), receiverPhone(NotBlank, Pattern ^(0\|+84)\d{9,10}$), provinceCode(NotBlank, max 20), districtCode(NotBlank, max 20), wardCode(NotBlank, max 20), addressDetail(NotBlank, max 255), fullAddress(NotBlank, max 500), defaultAddress?(Boolean)}` | `CustomerAddressResponse` | `hasRole('CUSTOMER')` | Add address |
| PUT | `/{id}` | `CustomerAddressRequest` (same fields) | `CustomerAddressResponse` | `hasRole('CUSTOMER')` | Update address |
| DELETE | `/{id}` | — | `Void` | `hasRole('CUSTOMER')` | Delete address |
| PATCH | `/{id}/default` | path `id` | `CustomerAddressResponse` | `hasRole('CUSTOMER')` | Set this address as the default |

## 6. Customer — `customer/controller/StaffCustomerAddressController.java`
Base path: `/api/v1/staff/customers/{customerId}/addresses`
Class-level `@PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")`.

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | path `customerId` | `List<CustomerAddressResponse>` | `hasAnyRole('MANAGER','STAFF')` | Staff view of a customer's saved addresses (read-only; no create/update/delete exposed to staff) |

## 7. Customer — `customer/controller/CustomerPublicProfileController.java` *(new)*
Base path: `/api/v1/customers`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `/{userId}` | path `userId` | `CustomerPublicProfileResponse{userId, customerId, fullName, avatarUrl?, skillLevel?(BEGINNER\|INTERMEDIATE\|ADVANCED), gender?, dateOfBirth?, hobbies: HobbyResponse[]}` | `authenticated()` (global default; no `@PreAuthorize`, path not matched by any URL rule) | Public-display profile of a Customer by their **User ID** (e.g. to show as a comment author); no email/phone/address |

## 8. Customer — `customer/controller/AdminCustomerController.java` *(new)*
Base path: `/api/v1/admin/customers`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | — | `List<AdminCustomerResponse{customerId, userId, fullName, email, status(ACTIVE\|BLOCKED\|INACTIVE), phone?, address?, dateOfBirth?, gender?, avatarUrl?, skillLevel?, emailVerified(boolean), createdAt, updatedAt}>` (no paging) | `hasRole('ADMIN')` (URL-level) | List all Customers with merged User+Customer profile fields (no password) |

## 9. Hobby — `hobby/controller/HobbyController.java` (public-facing) and `hobby/controller/AdminHobbyController.java` (admin CRUD)

Base path `/api/v1/hobbies`:

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | — | `List<HobbyResponse{id, name, description?, active}>` | `hasAnyRole('CUSTOMER','ADMIN')` (URL-level) | List active hobbies only, sorted by name asc |

Base path `/api/v1/admin/hobbies`:

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | — | `List<HobbyResponse>` | `hasRole('ADMIN')` (URL-level) | List all hobbies (active + inactive) |
| GET | `/{id}` | path `id` | `HobbyResponse` | `hasRole('ADMIN')` | Get hobby by id (including inactive) |
| POST | `` | `SaveHobbyRequest{name(NotBlank, max 100), description?(max 255)}` (201) | `HobbyResponse` | `hasRole('ADMIN')` | Create hobby (defaults active=true; name case-insensitively unique) |
| PUT | `/{id}` | `SaveHobbyRequest{name(NotBlank, max 100), description?(max 255)}` | `HobbyResponse` | `hasRole('ADMIN')` | Update name/description; active flag unchanged |
| PATCH | `/{id}/active` | `UpdateHobbyActiveRequest{active(NotNull, Boolean)}` | `HobbyResponse` | `hasRole('ADMIN')` | Toggle active flag |
| DELETE | `/{id}` | — | `HobbyResponse` | `hasRole('ADMIN')` | Soft-delete (sets active=false) |

## 10. Cart — `cart/controller/CartController.java`
Base path: `/api/v1/carts`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | — | `CartResponse{id, items: CartItemResponse[], totalItems(int), totalAmount}` where `CartItemResponse{id, productId, productName, imgUrl, uomId, unitName, unitPrice, quantity, subtotal, addedAt}` | `hasAnyRole('CUSTOMER','STAFF','MANAGER')` (method-level) | Get current user's cart (auto-created if absent) |
| POST | `/items` | `AddCartItemRequest{productId(NotNull), uomId(NotNull), quantity(NotNull, >0, 10 int/3 frac digits)}` | `CartResponse` | `hasAnyRole('CUSTOMER','STAFF','MANAGER')` | Add item to cart |
| PATCH | `/items/{itemId}` | `UpdateCartItemQuantityRequest{quantity(NotNull, >0, 10 int/3 frac digits)}` | `CartResponse` | `hasAnyRole('CUSTOMER','STAFF','MANAGER')` | Update item quantity |
| DELETE | `/items/{itemId}` | path `itemId` | `CartResponse` | `hasAnyRole('CUSTOMER','STAFF','MANAGER')` | Remove item from cart |

## 11. Expense — `expense/controller/ExpenseCategoryController.java`
Base path: `/api/v1/expense-categories`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `ExpenseCategoryRequest{name(NotBlank, max 150), type(NotNull; SALARY\|RENT\|MARKETING\|SHIPPING\|UTILITY\|OTHER)}` | `ExpenseCategoryResponse{id, name, type, createdAt}` | `hasRole('MANAGER')` | Create expense category (**MANAGER only**, not STAFF) |
| PUT | `/{id}` | `ExpenseCategoryRequest` | `ExpenseCategoryResponse` | `hasRole('MANAGER')` | Update category |
| DELETE | `/{id}` | — | `Void` | `hasRole('MANAGER')` | Delete category |
| GET | `` | — | `List<ExpenseCategoryResponse>` | `hasAnyRole('MANAGER','STAFF')` | List categories |

## 12. Expense — `expense/controller/ExpenseVoucherController.java`
Base path: `/api/v1/expense-vouchers`

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `CreateExpenseVoucherRequest{expenseCategoryId(NotNull), amount(NotNull, ≥0.01), reason?(max 500)}` | `ExpenseVoucherResponse{id, code, expenseCategoryId, expenseCategoryName, amount, reason?, status(ACTIVE\|CANCELLED), createdBy: UserSummaryResponse, createdAt}` | `hasAnyRole('MANAGER','STAFF')` | Create expense voucher |
| PUT | `/{id}` | `UpdateExpenseVoucherRequest{expenseCategoryId(NotNull), amount(NotNull, ≥0.01), reason?(max 500)}` | `ExpenseVoucherResponse` | `hasAnyRole('MANAGER','STAFF')` | Update voucher |
| PATCH | `/{id}/cancel` | `CancelExpenseVoucherRequest{cancelReason?(max 500)}` (optional body) | `ExpenseVoucherResponse` | `hasAnyRole('MANAGER','STAFF')` | Cancel voucher |
| GET | `/{id}` | path `id` | `ExpenseVoucherResponse` | `hasAnyRole('MANAGER','STAFF')` | Get voucher by id |
| GET | `` | `Pageable` (page/size/sort) | `Page<ExpenseVoucherResponse>` | `hasAnyRole('MANAGER','STAFF')` | List vouchers, paged |
| GET | `/report` | query `month?(yyyy-MM)`, `categoryId?` | `ExpenseReportResponse{month, categoryId, totalAmount, totalVouchers(long), categories: ExpenseCategorySummary[]}` where `ExpenseCategorySummary{categoryId, categoryName, categoryType, totalAmount, voucherCount(Long)}` | `hasAnyRole('MANAGER','STAFF')` | Aggregated expense report by category |
| GET | `/report-detail` | query `month?`, `categoryId?`, `page=0`, `size=20` | `Page<ExpenseVoucherWithNameResponse{response: ExpenseVoucherResponse, staffName}>` sorted createdAt desc | `hasAnyRole('MANAGER','STAFF')` | Drill-down voucher list behind a report row (note: response wraps `ExpenseVoucherResponse` plus `staffName`, not a flat object) |
| GET | `/summary` *(new)* | query `from`(ISO date, required), `to`(ISO date, required) | `ExpenseVoucherSummaryResponse{totalAmount, voucherCount(long), cancelledCount(long), averagePerVoucher}` | `hasRole('MANAGER')` | Stat-card summary for a date range (MANAGER only) |

## 13. Notification — `notification/controller/NotificationController.java`
Base path: `/api/v1/notifications`

| Method | Path | Request/Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | query `read?(boolean)`, `page=0`, `size=10` | `Page<NotificationResponse{id, title, content, refType(ORDER\|COMMENT\|RATING\|REQUEST_QUOTE\|PRODUCT), refId, read(boolean), readAt?, createdAt}>` (sorted createdAt desc) | `isAuthenticated()` (method-level; URL is `permitAll()` at filter-chain level, so a Bearer token is still required by method security) | List own notifications. **Field is `read`, not `isRead`. `refType` is `ORDER\|COMMENT\|RATING\|REQUEST_QUOTE\|PRODUCT` — not `SHIPMENT`/`RECIPE`/`SYSTEM`.** |
| GET | `/unread-count` | — | `UnreadCountResponse{unreadCount(long)}` | `isAuthenticated()` | Count unread |
| GET | `/{id}` | path `id` | `NotificationResponse` | `isAuthenticated()` | Get one notification |
| PATCH | `/{id}/read` | path `id` | `NotificationResponse` | `isAuthenticated()` | Mark one as read |
| PATCH | `/read-all` | — | `MarkAllReadResponse{updatedCount(int)}` | `isAuthenticated()` | Mark all as read |

## 14. Loyalty — `loyalty/controller/CustomerLoyaltyController.java` *(new module)*
Base path: `/api/v1/customers/me/loyalty`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | — | `LoyaltyBalanceResponse{balance(int), ingredientImagePrice(int), recipeVideoPrice(int)}` | `hasRole('CUSTOMER')` (URL-level, via `/api/v1/customers/me/**`) | Current point balance + AI-feature prices (0 balance if no transactions yet) |
| GET | `/transactions` | query `page=0`, `size=20` (capped at 100) | `Page<LoyaltyTransactionResponse{id, type(WELCOME\|ORDER_EARN\|AI_SPEND\|AI_REFUND_ERROR\|AI_REFUND_PURCHASE), points(int, +/-), balanceAfter(int), note?, orderId?, createdAt}>` | `hasRole('CUSTOMER')` | Own point transaction history, newest first |

## 15. Loyalty — `loyalty/controller/AdminLoyaltyController.java` *(new module)*
Base path: `/api/v1/admin/customers/{customerId}/loyalty`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | path `customerId` | `LoyaltyBalanceResponse` | `hasRole('ADMIN')` (URL-level, via `/api/v1/admin/customers/**`) | Admin read-only view of a Customer's point balance (404 if customerId not found) |
| GET | `/transactions` | path `customerId`; query `page=0`, `size=20` (capped at 100) | `Page<LoyaltyTransactionResponse>` | `hasRole('ADMIN')` | Admin read-only view of a Customer's point transaction history |

## 16. Product — `product/controller/CategoryController.java`
Base path: `/api/product-categories` (note: **no `/v1` prefix**, inconsistent with the rest of the API)

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `CategoryRequest{categoryName(NotBlank, max 255)}` (201) | `CategoryResponse{productCategoryId, categoryName, createdAt}` | Global default (`authenticated()`, no `@PreAuthorize`) | Create product category |
| PUT | `/{id}` | `CategoryRequest` | `CategoryResponse` | Global default | Update category |
| GET | `` | — | `List<CategoryResponse>` | Global default | List categories |
| DELETE | `/{id}` | — | `Void` | Global default | Delete category |

## 17. Product — `product/controller/ProductController.java`
Base path: `/api/products` (note: **no `/v1` prefix**)

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `ProductUpsertRequest{sku(NotBlank, max 100), name(NotBlank, max 255), description?, categoryId?, imgUrl?, discountPercent?(0-100, 3int/2frac), storageGuide?, minStock?(≥0, 8int/2frac), maxStock?(≥0, 8int/2frac), status?, brand?(max 255), combo?(Boolean), units: ProductUnitRequest[]}` where `ProductUnitRequest{unitId?, unitName(NotBlank, max 100), conversionFactor(NotNull, ≥0.01, 8int/2frac), base(NotNull, Boolean), sale?(Boolean), sellingPrice(NotNull, ≥0.00, 13int/2frac), status?, weightGram?(1-1,000,000)}` (201) | `ProductResponse{productId, sku, name, description?, categoryId?, imgUrl?, discountPercent?, storageGuide?, minStock?, maxStock?, stockQuantity, status(ACTIVE\|INACTIVE\|DISCONTINUE), brand?, combo(boolean), createdAt, updatedAt, units: ProductUnitResponse[]}` where `ProductUnitResponse{unitId, unitName, conversionFactor, base(boolean), sale(boolean), sellingPrice, status, weightGram?}` | Global default | Create product. **`ProductUnitRequest`/`ProductUnitResponse` now also carry `weightGram`** (used for shipping weight calc) |
| PUT | `/{productId}` | `ProductUpsertRequest` | `ProductResponse` | Global default | Update product |
| GET | `/{productId}` | path `productId` | `ProductResponse` | Global default | Get product |
| GET | `` | query `categoryId?`, `brand?`, `name?`, `status?(ACTIVE\|INACTIVE\|DISCONTINUE)`, `Pageable` | `Page<ProductResponse>` | Global default | List/search products (now also filters by `name` and `status`, in addition to `categoryId`/`brand`) |
| POST | `/low-stock/alerts` *(new)* | — | `LowStockAlertSummary{lowStockProducts(int), notificationsSent(int)}` | `hasAnyRole('MANAGER','STAFF')` (URL-matched specifically for this `POST` path) | Trigger low-stock alert sweep; sends PRODUCT-type notifications and returns counts |
| DELETE | `/{productId}` | — | `Void` | Global default | Deactivate product |
| GET | `/{productId}/units/{unitId}/price-history` | path `productId, unitId` | `List<UnitPriceHistoryResponse{unitHistoryId, unitId, updatedBy: UserSummaryResponse, sellingPrice, createdAt}>` | Global default | Price history for a unit. **`updatedBy` is now a `UserSummaryResponse{id, fullName, avatarUrl}` object, not a bare UUID.** |
| PUT | `/{productId}/combo-items` | `List<ComboItemRequest{productId(NotNull), quantity(NotNull, ≥0.01, 8int/2frac)}>` | `List<ComboItemRequest>` (echoes request — no dedicated response DTO) | Global default | Set combo (bundle) items for a combo product |
| GET | `/{productId}/combo-stock` | path `productId` | `BigDecimal` (raw number, no named DTO) | Global default | Calculate computed stock for a combo product |

## 18. Recipe — `recipe/controller/RecipeController.java`
Base path: `/api/v1/recipes`

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `/{recipeId}` | path `recipeId` | `RecipeResponse{id, userId, categoryId?, title, description?, videoId?, videoUrl?, imageIds[], imageUrls[], difficulty(EASY\|MEDIUM\|HARD), prepTimeMinutes, cookTimeMinutes, servings, averageRating?, recipeStatus(DRAFT\|PUBLISHED\|HIDDEN), createdAt, publishedAt?, viewCount(Long), bookmarkCount(Long), ratingCount(Long), tools: ToolResponse[], ingredients: IngredientResponse[], steps: RecipeStepResponse[]}` | Global default | Get recipe detail with nested tools/ingredients/steps. **`publishedAt`, `viewCount`, `bookmarkCount`, `ratingCount` are new fields alongside the existing `averageRating`.** |
| POST | `` | `RecipeRequest{categoryId?, title(NotNull), description?, videoId?, imageIds[]?, difficulty(NotNull), prepTimeMinutes(NotNull), cookTimeMinutes(NotNull), servings(NotNull), recipeStatus(NotNull), tools: ToolEmbeddedRequest[]{toolId(NotNull, UUID), quantity(NotNull)}, ingredients: IngredientEmbeddedRequest[]{ingredientId(NotNull, UUID), quantity(NotNull)}, steps: RecipeStepEmbeddedRequest[]{stepNumber(NotNull), instruction(NotNull), durationMinutes(NotNull, ≥0), notes?, recipeStepType(NotNull; PREPARATION\|COOKING), imageIds[]?, ingredients: IngredientEmbeddedRequest[]?, tools: ToolEmbeddedRequest[]?}}` | `RecipeResponse` | Global default | Create recipe with nested tools/ingredients/steps in one call. **`tools`/`ingredients` embedded items now reference catalog UUIDs (`toolId`/`ingredientId`) instead of free-text name/unit; steps now require `durationMinutes` and `recipeStepType`, and accept per-step nested `ingredients`/`tools`.** |
| PUT | `/{recipeId}` | `RecipeRequest` (same shape) | `RecipeResponse` | Global default | Update recipe (full replace of nested lists; null/omitted/`[]` clears the corresponding list) |
| DELETE | `/{recipeId}` | — | `String` `"Deleted"` | Global default | Delete recipe |
| GET | `/search` | query `userId?`, `categoryId?`, `search?`, `difficulty?`, `recipeStatus?`, `Pageable` (default `page=0,size=10`, sort `createdAt desc`) | `PageResponse<RecipeResponse>` (custom `recipe.dto.response.PageResponse<T>{content, totalElements, totalPages, currentPage}`) | Global default | Search/filter recipes |

## 19. Recipe — `recipe/controller/RecipeCategoryController.java`
Base path: `/api/v1/recipe-categories`

| Method | Path | Request/Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | query `search?`, `Pageable` (default `page=0,size=10`, sort `name asc`) | `PageResponse<RecipeCategoryResponse{id, name}>` | Global default | List categories |
| GET | `/{categoryId}` | path | `RecipeCategoryResponse` | Global default | Get category |
| POST | `` | `RecipeCategoryRequest{name(NotNull)}` | `RecipeCategoryResponse` | Global default | Create category |
| PUT | `/{categoryId}` | `RecipeCategoryRequest{name(NotNull)}` | `RecipeCategoryResponse` | Global default | Update category (full replace, not partial) |
| DELETE | `/{categoryId}` | — | `String` `"Deleted"` | Global default | Delete category |

## 20. Recipe — catalog-level ingredients — `recipe/controller/IngredientController.java`
Base path: `/api/v1/ingredients`
**Restructured 2026-11-13**: this controller no longer has a `GET /recipe/{recipeId}` endpoint — it now manages the flat `Ingredient` catalog entity only. Per-recipe linkage moved to `RecipeIngredientController` (§22).

| Method | Path | Request/Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | query `search?`, `Pageable` (default `page=0,size=10`, sort `name asc`) | `PageResponse<IngredientResponse{id, recipeId?, name, unit, quantity?, catalogIngredientId?}>` | Global default | List ingredients |
| GET | `/{ingredientId}` | path | `IngredientResponse` | Global default | Get ingredient |
| POST | `` | `IngredientRequest{name(NotNull), unit(NotNull)}` | `IngredientResponse` | Global default | Create ingredient |
| PUT | `/{ingredientId}` | `IngredientRequest` | `IngredientResponse` | Global default | Update ingredient (full replace) |
| DELETE | `/{ingredientId}` | — | `String` `"Deleted"` | Global default | Delete ingredient |

## 21. Recipe — catalog-level tools — `recipe/controller/ToolController.java`
Base path: `/api/v1/tools`
Same 2026-11-13 restructuring as §20: no more `GET /recipe/{recipeId}`.

| Method | Path | Request/Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | query `search?`, `Pageable` (default `page=0,size=10`, sort `name asc`) | `PageResponse<ToolResponse{id, recipeId?, name, quantity?}>` | Global default | List tools |
| GET | `/{toolId}` | path | `ToolResponse` | Global default | Get tool |
| POST | `` | `ToolRequest{name(NotNull)}` | `ToolResponse` | Global default | Create tool |
| PUT | `/{toolId}` | `ToolRequest` | `ToolResponse` | Global default | Update tool |
| DELETE | `/{toolId}` | — | `String` `"Deleted"` | Global default | Delete tool |

## 22. Recipe — recipe↔ingredient link — `recipe/controller/RecipeIngredientController.java` *(new controller)*
Base path: `/api/v1/recipe-ingredients`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | — | `List<RecipeIngredientResponse{id, recipeId, ingredientId, ingredientName, quantity, unit}>` (no paging) | Global default | List all recipe↔ingredient links |
| GET | `/{id}` | path | `RecipeIngredientResponse` | Global default | Get link by id |
| GET | `/recipe/{recipeId}` | path `recipeId` | `List<RecipeIngredientResponse>` | Global default | Links for a recipe |
| POST | `` | `RecipeIngredientRequest{recipeId(NotNull), ingredientId(NotNull), quantity(NotNull)}` | `RecipeIngredientResponse` | Global default | Link an ingredient to a recipe |
| PUT | `/{id}` | `RecipeIngredientRequest` | `RecipeIngredientResponse` | Global default | Update link |
| DELETE | `/{id}` | — | `String` `"Deleted"` | Global default | Delete link |

## 23. Recipe — recipe↔tool link — `recipe/controller/RecipeToolController.java` *(new controller)*
Base path: `/api/v1/recipe-tools`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | — | `List<RecipeToolResponse{id, recipeId, toolId, toolName, quantity}>` (no paging) | Global default | List all recipe↔tool links |
| GET | `/{id}` | path | `RecipeToolResponse` | Global default | Get link by id |
| GET | `/recipe/{recipeId}` | path `recipeId` | `List<RecipeToolResponse>` | Global default | Links for a recipe |
| POST | `` | `RecipeToolRequest{recipeId(NotNull), toolId(NotNull), quantity(NotNull)}` | `RecipeToolResponse` | Global default | Link a tool to a recipe |
| PUT | `/{id}` | `RecipeToolRequest` | `RecipeToolResponse` | Global default | Update link |
| DELETE | `/{id}` | — | `String` `"Deleted"` | Global default | Delete link |

## 24. Recipe — `recipe/controller/RecipeStepController.java`
Base path: `/api/v1/recipe-steps`

| Method | Path | Request/Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | query `search?`, `Pageable` (default `page=0,size=10`, sort `stepNumber asc`) | `PageResponse<RecipeStepResponse{id, recipeId, stepNumber, instruction, durationMinutes, notes?, recipeStepType(PREPARATION\|COOKING), imageIds[], imageUrls[], ingredients: RecipeStepIngredientResponse[], tools: RecipeStepToolResponse[]}>` | Global default | List all steps |
| GET | `/{recipeStepId}` | path | `RecipeStepResponse` | Global default | Get step |
| GET | `/recipe/{recipeId}` | path `recipeId`; query `search?`, `Pageable` | `PageResponse<RecipeStepResponse>` | Global default | Steps for a recipe |
| POST | `` | `RecipeStepRequest{recipeId(NotNull), stepNumber(NotNull), instruction(NotNull), durationMinutes(NotNull, ≥0), notes?, recipeStepType(NotNull; PREPARATION\|COOKING), imageIds[]?, ingredients: IngredientEmbeddedRequest[]{ingredientId, quantity}?, tools: ToolEmbeddedRequest[]{toolId, quantity}?}` | `RecipeStepResponse` | Global default | Create step; `stepNumber` must keep a 1-based contiguous sequence within the recipe |
| PUT | `/{recipeStepId}` | `RecipeStepEmbeddedRequest` (same shape as above, no `recipeId`) | `RecipeStepResponse` | Global default | Update step; replaces ingredient/tool lists wholesale (null/`[]` clears), `imageIds` null/`[]` clears all images |
| DELETE | `/{recipeStepId}` | — | `String` `"Deleted"` | Global default | Delete step (cascades to its ingredient/tool links) |

**`RecipeStepResponse`/`RecipeResponse.steps[]` now include `durationMinutes`, `notes`, `recipeStepType`, nested `ingredients: RecipeStepIngredientResponse[]` and `tools: RecipeStepToolResponse[]`** on top of the prior fields.

## 25. Recipe — recipe-step↔ingredient link — `recipe/controller/RecipeStepIngredientController.java`
Base path: `/api/v1/recipe-step-ingredients`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | — | `List<RecipeStepIngredientResponse{id, recipeStepId, ingredientId, ingredientName, quantity, unit}>` | Global default | List all links |
| GET | `/{id}` | path | `RecipeStepIngredientResponse` | Global default | Get link by id |
| GET | `/recipe-step/{recipeStepId}` | path | `List<RecipeStepIngredientResponse>` | Global default | Links for a recipe step |
| POST | `` | `RecipeStepIngredientRequest{recipeStepId(NotNull), ingredientId(NotNull), quantity(NotNull)}` | `RecipeStepIngredientResponse` | Global default | Create link (step cannot have duplicate links to the same ingredient) |
| PUT | `/{id}` | `RecipeStepIngredientRequest` | `RecipeStepIngredientResponse` | Global default | Update link |
| DELETE | `/{id}` | — | `String` `"Deleted"` | Global default | Delete link (step and ingredient preserved) |

## 26. Recipe — recipe-step↔tool link — `recipe/controller/RecipeStepToolController.java`
Base path: `/api/v1/recipe-step-tools`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | — | `List<RecipeStepToolResponse{id, recipeStepId, toolId, toolName, quantity}>` | Global default | List all links |
| GET | `/{id}` | path | `RecipeStepToolResponse` | Global default | Get link by id |
| GET | `/recipe-step/{recipeStepId}` | path | `List<RecipeStepToolResponse>` | Global default | Links for a recipe step |
| POST | `` | `RecipeStepToolRequest{recipeStepId(NotNull), toolId(NotNull), quantity(NotNull)}` | `RecipeStepToolResponse` | Global default | Create link |
| PUT | `/{id}` | `RecipeStepToolRequest` | `RecipeStepToolResponse` | Global default | Update link |
| DELETE | `/{id}` | — | `String` `"Deleted"` | Global default | Delete link |

## 27. Recipe — `recipe/controller/RecipeCommentController.java`
Base path: `/api/v1/recipe-comments`

| Method | Path | Request/Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | query `search?`, `Pageable` (default `page=0,size=10`, sort `createdAt desc`) | `PageResponse<RecipeCommentResponse{id, recipeId, userId, user: recipe.dto.response.UserSummaryResponse{id, avatar_url, full_name}, content, parentId?, createdAt, updatedAt}>` | Global default | List all comments |
| GET | `/{commentId}` | path | `RecipeCommentResponse` | Global default | Get comment |
| GET | `/recipe/{recipeId}` | path; query `search?`, `Pageable` | `PageResponse<RecipeCommentResponse>` | Global default | Comments for a recipe |
| GET | `/user/{userId}` | path; query `search?`, `Pageable` | `PageResponse<RecipeCommentResponse>` | Global default | Comments by a user |
| GET | `/reply/{parentId}` | path; query `search?`, `Pageable` | `PageResponse<RecipeCommentResponse>` | Global default | Replies to a comment |
| POST | `` | `RecipeCommentRequest{recipeId(NotNull), content(NotNull), parentId?}` | `RecipeCommentResponse` | Global default | Create comment/reply |
| PUT | `/{commentId}` | `RecipeCommentRequest` | `RecipeCommentResponse` | Global default | Update comment (`parentId` null → detaches from parent) |
| DELETE | `/{commentId}` | — | `String` `"Deleted"` | Global default | Delete comment |

**`recipe/dto/response/UserSummaryResponse.java` is a SEPARATE class** from `user/dto/UserSummaryResponse.java` (§3). It has literal snake_case fields `{id, avatar_url, full_name}` and is used **only** as `RecipeCommentResponse.user`. Do not confuse it with the camelCase `UserSummaryResponse{id, fullName, avatarUrl}` used everywhere else (Order/Expense/Receipt/Voucher/Quote/Product/StockTransaction/SupplierPayment).

## 28. Recipe — `recipe/controller/RecipeRatingController.java`
Base path: `/api/v1/recipe-ratings`

| Method | Path | Request/Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | query `search?`, `Pageable` (default `page=0,size=10`, sort `createdAt desc`) | `PageResponse<RecipeRatingResponse{id, recipeId, customerId, score, createdAt}>` | Global default | List all ratings |
| GET | `/{ratingId}` | path | `RecipeRatingResponse` | Global default | Get rating |
| GET | `/user/{customerId}` | path `customerId`; query `search?`, `Pageable` | `PageResponse<RecipeRatingResponse>` | Global default | Ratings by a customer. **Service method is `getByCustomerId` (renamed from `getByUserId` on 2026-10-25); path unchanged (`/user/{customerId}`).** |
| GET | `/recipe/{recipeId}` | path; query `search?`, `Pageable` | `PageResponse<RecipeRatingResponse>` | Global default | Ratings for a recipe |
| POST | `` | `RecipeRatingRequest{recipeId(NotNull), score(NotNull, 1-5), recommId?(max 100)}` | `RecipeRatingResponse` | Global default | Create rating; `recommId` optional, set only when rating a recipe surfaced by the recommendation API |
| PUT | `/{ratingId}` | `RecipeRatingRequest` | `RecipeRatingResponse` | Global default | Update rating |
| DELETE | `/{ratingId}` | — | `String` `"Deleted"` | Global default | Delete rating |

## 29. Recipe — `recipe/controller/RecipeBookmarkController.java`
Base path: `/api/v1/recipe-bookmarks`

| Method | Path | Request/Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | `Pageable` (default `page=0,size=10`, sort `createdAt desc`) | `PageResponse<RecipeBookmarkResponse{id, recipeId, customerId, createdAt}>` | Global default | List all bookmarks |
| GET | `/{recipeBookmarkId}` | path | `RecipeBookmarkResponse` | Global default | Get bookmark |
| GET | `/user/{customerId}` | path; `Pageable` | `PageResponse<RecipeBookmarkResponse>` | Global default | Bookmarks by customer |
| GET | `/recipe/{recipeId}` | path; `Pageable` | `PageResponse<RecipeBookmarkResponse>` | Global default | Bookmarks for a recipe |
| POST | `` | `RecipeBookmarkRequest{recipeId(NotNull), recommId?(max 100)}` | `RecipeBookmarkResponse` | Global default | Bookmark a recipe; `recommId` optional for recommendation attribution |
| PUT | `/{recipeBookmarkId}` | `RecipeBookmarkRequest` | `RecipeBookmarkResponse` | Global default | Update bookmark |
| DELETE | `/{recipeBookmarkId}` | — | `String` `"Deleted"` | Global default | Remove bookmark |

## 30. Recipe — `recipe/controller/RecipeInteractionController.java` *(new controller)*
Base path: `/api/v1/customers/me/interaction-history`

| Method | Path | Request/Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | query `type?(VIEW\|BOOKMARK\|RATING)`, `Pageable` (default `page=0,size=20`, sort `occurredAt desc`) | `PageResponse<RecipeInteractionResponse{id, type, recipeId, recipeTitle, ratingScore?, recommId?, occurredAt(Instant), active?(Boolean)}>` | `hasRole('CUSTOMER')` (URL-level, via `/api/v1/customers/me/**`) | Current customer's interaction log. For BOOKMARK/RATING, `active=true` means this row is the customer's current bookmark/rating; `active=false` means unbookmarked/deleted/superseded. `active` is always `null` for VIEW rows |
| POST | `/views/{recipeId}` | path `recipeId`; query `recommId?(max 100)` | `String` `"View recorded"` | `hasRole('CUSTOMER')` | Record that the customer viewed a PUBLISHED recipe (404 if not published); re-views within 30 minutes count once |

## 31. Recipe — `recipe/controller/RecipeVideoController.java` *(new controller)*
Base path: `/api/v1/recipes`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `/videos/render` | `CreateRecipeVideoRequest{recipeId(NotNull)}` | `RecipeVideoRenderResponse{id, recipeId, renderId, status, videoUrl?}` (HTTP 202 Accepted) | Global default (no `@PreAuthorize`) | Kick off Shotstack video render for a recipe (pulls recipe/category/images/tools/ingredients/steps from DB for a Vietnamese voice-over video); returns immediately with `status=PROCESSING` |

Related webhook: `POST /api/v1/media/videos/webhook` (see §32, `ShotstackWebhookController`) is `permitAll()` in `SecurityConfig` and updates render status asynchronously.

## 32. Media — `media/controller/MediaController.java`
Base path: `/api/v1/media`

| Method | Path | Request Body (multipart) | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `/images` | form field `files`(List<MultipartFile>, 1-10 files; each ≤5 MB, total ≤25 MB; JPEG/PNG/WebP) | `List<MediaResponse{id, url, type(IMAGE\|VIDEO), status(TEMP\|ATTACHED\|DELETING)}>` | Global default (`authenticated()`, no `@PreAuthorize`) | Upload multiple images, creates media records in TEMP status. **Now a batch endpoint (`files`, not `file`), replacing the single-file version in the prior doc.** |
| POST | `/videos` | form field `file`(MultipartFile, required, ≤50 MB; MP4/MOV/WebM) | `MediaResponse` | Global default | Upload one video |
| DELETE | `/{id}` | path `id` | `MediaResponse` | Global default | Delete media by id; only allowed while status is TEMP or DELETING |

## 33. Media — `media/controller/ShotstackWebhookController.java`
Base path: `/api/v1/media/videos`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `/webhook` | `ShotstackWebhookRequest{type?, action?, id?, status?, url?, error?}` (unknown JSON fields ignored) | `ApiResponse<Void>` | `permitAll()` (explicit `POST`-only matcher in `SecurityConfig`) | Shotstack render-completion/failure callback |

## 34. Ingredient Catalog — `recipe/ingredientcatalog/controller/IngredientCatalogManagementController.java`
Base path: `/api/v1/management/ingredient-catalog`
Class not annotated with `@PreAuthorize`, but URL matched by `SecurityConfig` to `hasRole('BAKER_STAFF')`.

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | query `active?(Boolean)` | `List<IngredientCatalogResponse{id, canonicalName, normalizedName, active, createdAt, updatedAt}>` | `hasRole('BAKER_STAFF')` | Browse the standard-ingredient catalog |
| GET | `/{id}` | path `id` | `IngredientCatalogResponse` | `hasRole('BAKER_STAFF')` | Get one catalog entry (including inactive); 404 if not found |
| POST | `` | `SaveIngredientCatalogRequest{canonicalName(NotBlank, max 150)}` (201) | `IngredientCatalogResponse` | `hasRole('BAKER_STAFF')` | Create a canonical ingredient; auto-creates a self-alias; 409 if the normalized name belongs to another catalog entry (even inactive); auto-links any not-yet-linked recipe ingredient rows with a matching raw name |
| PUT | `/{id}` | `SaveIngredientCatalogRequest{canonicalName(NotBlank, max 150)}` | `IngredientCatalogResponse` | `hasRole('BAKER_STAFF')` | Rename canonical name (old name kept as an alias); auto-links matching unlinked rows to the new name |
| PATCH | `/{id}/active` | `UpdateIngredientCatalogActiveRequest{active(NotNull, Boolean)}` | `IngredientCatalogResponse` | `hasRole('BAKER_STAFF')` | Toggle active (soft delete only — no hard delete); 409 if deactivating a catalog entry still used by a PUBLISHED recipe; reactivating re-links matching unlinked rows |
| GET | `/{id}/aliases` | path `id` | `List<IngredientAliasResponse{id, catalogIngredientId, alias, normalizedAlias, languageCode?, createdAt}>` | `hasRole('BAKER_STAFF')` | List a catalog entry's aliases (including the self-alias) |
| POST | `/{id}/aliases` | `SaveIngredientAliasRequest{alias(NotBlank, max 150), languageCode?(max 10, Pattern language-tag-ish)}` (201) | `IngredientAliasResponse` | `hasRole('BAKER_STAFF')` | Add alias; 409 on duplicate normalized alias; auto-links matching unlinked rows if catalog is active |
| PUT | `/{id}/aliases/{aliasId}` | `SaveIngredientAliasRequest` | `IngredientAliasResponse` | `hasRole('BAKER_STAFF')` | Rename alias (cannot rename the self-alias to a different canonicalized value; new name must not collide with another catalog/alias) |
| DELETE | `/{id}/aliases/{aliasId}` | — | `Void` | `hasRole('BAKER_STAFF')` | Delete alias (409 if it is the current canonical self-alias); does not affect already-saved recipe links |
| GET | `/ingredient-links` | query `keyword?(max 150)`, `page=0`, `size=20(1-100)` | `PageResponse<UnlinkedIngredientGroup{name, normalizedName, rowCount(long), spellings: String[], candidates: IngredientCatalogCandidate[](max 3)}>` sorted by `rowCount desc` | `hasRole('BAKER_STAFF')` | View unlinked recipe-ingredient raw names grouped by normalized name, with up to 3 suggested catalog candidates (suggestions only, not auto-linked) |
| POST | `/ingredient-links/by-name` | `LinkByNameRequest{name(NotBlank, max 150), catalogIngredientId(NotNull)}` | `LinkByNameResponse{catalogIngredientId, canonicalName, name, aliasCreated(boolean), linkedIngredientCount(int)}` | `hasRole('BAKER_STAFF')` | Link every unlinked recipe-ingredient row with a matching raw name to the given active catalog entry (creates an alias if the name isn't already one); 409 if name belongs to a different catalog entry, 400 if target catalog is missing/inactive |

`IngredientCatalogCandidate{catalogIngredientId, canonicalName, matchedName, similarity(double), matchType(EXACT\|PARTIAL\|FUZZY)}` is shared with the Recipe Suggestion module (§35).

## 35. Recipe Suggestion — `recipe/suggestion/controller/RecipeSuggestionController.java`
Base path: `/api/v1/recipe-suggestions`
Class not annotated, but URL matched by `SecurityConfig` to `hasRole('CUSTOMER')`.

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `/catalog-ingredients` | query `keyword?(max 150)`, `limit=10(1-20)` | `List<IngredientCatalogCandidate{catalogIngredientId, canonicalName, matchedName, similarity, matchType(EXACT\|PARTIAL\|FUZZY)}>` | `hasRole('CUSTOMER')` | Typeahead search over canonical names + aliases; empty `keyword` lists active catalog entries (then `matchType=null`, `similarity=0`) |
| POST | `` | `RecipeSuggestionRequest{catalogIngredientIds(NotEmpty, 1-20 UUIDs), limit?(1-5, default 3)}` | `RecipeSuggestionResponse{selectedIngredients: {catalogIngredientId, canonicalName}[], recipes: RecipeSuggestionItem[]{recipeId, title, matchedIngredientCount, totalIngredientCount, coverage(double), matchedIngredients: String[], missingIngredients: String[], unmappedIngredientCount(int)}, coverageBasis, warnings: String[]}` | `hasRole('CUSTOMER')` | Find recipes matching the selected catalog ingredients, ranked by ingredient-type coverage (not quantity); no AI call |
| POST | `/images/recognize` | multipart `file`(required; JPEG/PNG, ≤5 MB, ≤16 MP) | `IngredientImageRecognitionResponse{remainingDailyQuota(int), pointsCharged(int), pointsBalance(int), ingredients: Item[]{observedName, vietnameseName, uncertain(boolean), matched(boolean), catalogIngredientId?, canonicalName?, candidates: IngredientCatalogCandidate[]}}` | `hasRole('CUSTOMER')` | AI ingredient recognition from a photo. Quota 5/day (resets midnight Vietnam time) → 429 when exhausted (not charged); costs loyalty points per `GET /api/v1/customers/me/loyalty` price → 402 if insufficient points (quota not consumed); image not stored on R2; AI error/timeout refunds points, AI refusal/no-detection does not |

## 36. Recommendation — `recommendation/controller/RecipeRecommendationController.java`
Base path: `/api/v1/recommendations`
Class not annotated, but URL matched by `SecurityConfig` to `hasRole('CUSTOMER')`.

| Method | Path | Request/Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `/recipes` | query `count=10(1-20)` | `RecipeRecommendationResponse{modelVersion?(Integer), recommId, recipes: Item[]{recipe: RecipeResponse, score(double), reasons: String[]}}` | `hasRole('CUSTOMER')` | Personalized recipe recommendations for the logged-in customer, blending content/skill/hobby/collaborative/quality signals; excludes already-bookmarked/rated recipes; capped at 3 per category; results cached 5 minutes, refreshed on view/save/rate/profile edit. FE should echo `recommId` back on subsequent view/bookmark/rating calls for this list |

## 37. Recommendation — `recommendation/controller/AdminRecommendationController.java`
Base path: `/api/v1/admin/recommendations`

| Method | Path | Request/Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `/models/train` | query `k=10(1-20)` | `TrainingResponse{modelId, modelVersion(int), active(boolean), trainingSamples(int), positiveSamples(int), testCustomers(int), k(int), finalLoss(double), weights, baseline, defaultWeights, trained, note?}` (201) | `hasRole('ADMIN')` (URL-level) | Train the 5-feature logistic-regression recommender from real likes (bookmark or 4-5★ rating); compares baseline vs default vs trained metrics (Hit@k then MRR) and auto-activates if trained wins; 400 if insufficient data (needs ≥20 customers with ≥3 likes and ≥100 likes total) |
| GET | `/knowledge` | — | `KnowledgeResponse{activeCatalogIngredients(long), ingredientAliases(long), linkedIngredientRatio(double), publishedRecipes(long), likes(long), skillFit: SkillFit[]{skillLevel, difficulty, fit(double)}, learnedHobbies: LearnedHobby[]{hobby, likes(int), categories: LearnedCategory[]{category, likes(int), lift(double), score(double)}}, activeModelVersion?(Integer), weightsInUse}` | `hasRole('ADMIN')` | View the recommender's stored + learned knowledge base and currently active weights; data refreshed at most every 5 minutes |

## 38. Order — `order/controller/CustomerOrderController.java`
Base path: `/api/v1/orders`
Class-level `@PreAuthorize("hasRole('CUSTOMER')")` — path not covered by any `SecurityConfig` URL matcher, relies entirely on method security.

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `CreateOrderRequest{paymentMethod(NotNull; COD\|PAYOS\|BANK_TRANSFER), note?(max 500), receiverName(NotBlank, max 150), receiverPhone(NotBlank, max 20), provinceCode(NotBlank), districtCode(NotBlank), wardCode(NotBlank), addressDetail(NotBlank, max 255), fullAddress(NotBlank, max 500), shippingRateId(NotBlank, max 100), voucherCode?(max 50), items: OrderItemRequest[]{productUnitId(NotNull), quantity(NotNull, 1-1000)}}` | `OrderResponse{id, code, customerId, createdBy: UserSummaryResponse, status(AWAITING_PAYMENT\|PENDING\|CONFIRMED\|READY_TO_SHIP\|SHIPPING\|DELIVERED\|CANCELLED), paymentMethod(COD\|PAYOS\|BANK_TRANSFER), productsPrice, shippingFee, discountPercent, discountAmount, voucherCode?, voucherDiscountAmount?, total, revenue, note?, receiverName, receiverPhone, provinceCode, districtCode, wardCode, addressDetail, fullAddress, cancelReason?, carrierName?, shippingService?, cancelledAt?, deliveredAt?, receivedConfirmedAt?, createdAt, items: OrderItemResponse[]{id, productId, productUnitId, productName, sku, unitName, quantity(int), unitPrice, lineTotal}}` | `hasRole('CUSTOMER')` (method-level) | Place order from checkout; snapshots the chosen shipping rate (`shippingRateId`); optional `voucherCode`. **`PaymentMethod` is now `COD\|PAYOS\|BANK_TRANSFER`; `createdBy` is a `UserSummaryResponse` object, not a bare UUID; `OrderResponse` also now carries `voucherCode`/`voucherDiscountAmount`.** |
| GET | `/me` | query `status?`, `page=0`, `size=20` | `Page<OrderSummaryResponse{id, code, customerId, status, paymentMethod, total, receiverName, createdAt}>` (sorted createdAt desc) | `hasRole('CUSTOMER')` | List own orders |
| GET | `/{id}` | path `id` | `OrderResponse` | `hasRole('CUSTOMER')` | Get own order detail |
| GET | `/{id}/tracking` | path `id` | `OrderTrackingResponse{id, code, status, paymentMethod, createdAt, updatedAt, deliveredAt?, cancelledAt?}` | `hasRole('CUSTOMER')` | Lightweight tracking view |
| PATCH | `/{id}/cancel` | path `id`; `CancelOrderRequest{reason?(max 500)}` (optional body) | `OrderResponse` | `hasRole('CUSTOMER')` | Cancel own order (only from `AWAITING_PAYMENT`/`PENDING`/`CONFIRMED`) |
| PATCH | `/{id}/confirm-received` | path `id` | `OrderResponse` | `hasRole('CUSTOMER')` | Customer confirms delivery received |
| GET | `/{id}/history` *(new)* | path `id` | `List<OrderHistoryResponse{action(CREATED\|STATUS_CHANGED\|CANCELLED\|RECEIVED_CONFIRMED), fromStatus?, toStatus?, actorType(CUSTOMER\|STAFF\|SYSTEM), note?, createdAt}>` (no paging, ascending for a timeline view) | `hasRole('CUSTOMER')` | View own order's status-change history |
| POST | `/preview-points` *(new)* | `PreviewPointsRequest{items: OrderItemRequest[](NotEmpty), voucherCode?}` | `ExpectedPointsResponse{expectedPoints(int), pointBase}` | `hasRole('CUSTOMER')` | Preview expected loyalty points for a prospective order (shown in cart/checkout); points = post-voucher merchandise total / 10,000đ floored, doubled on a customer's first successfully delivered order, capped at 500/order; actually credited only on `DELIVERED` |

## 39. Order — `order/controller/StaffOrderController.java`
Base path: `/api/v1/staff/orders`
Class-level `@PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")` — not covered by a `SecurityConfig` URL matcher.

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `StaffCreateOrderRequest{customerId(NotNull), discountPercent?(0-100), order: CreateOrderRequest(NotNull)}` | `OrderResponse` | `hasAnyRole('MANAGER','STAFF')` | Staff-created order on behalf of a customer (phone/walk-in order), with optional manual discount |
| GET | `` | query `status?`, `paymentMethod?(COD\|PAYOS\|BANK_TRANSFER)`, `page=0`, `size=20` | `Page<OrderSummaryResponse>` (sorted createdAt desc) | `hasAnyRole('MANAGER','STAFF')` | List all orders |
| GET | `/revenue` | query `type(DAY\|WEEK\|MONTH, required)`, `from?(ISO date)`, `to?(ISO date)` (defaults to last 30 days ending today) | `RevenueReportResponse{type, from, to, totalRevenue, totalOrders(long), points: RevenuePoint[]{period, revenue, orderCount(long)}}` | `hasAnyRole('MANAGER','STAFF')` | Revenue report bucketed by day/week/month |
| GET | `/summary` *(new)* | — | `OrderStatusSummaryResponse{totalOrders(long), pendingApproval(long), shipping(long), completed(long)}` | `hasAnyRole('MANAGER','STAFF')` | Snapshot stat-card totals (not filtered by time) |
| GET | `/{id}` | path `id` | `OrderResponse` | `hasAnyRole('MANAGER','STAFF')` | Get any order detail |
| PATCH | `/{id}/status` | `UpdateOrderStatusRequest{status(NotNull)}` | `OrderResponse` | `hasAnyRole('MANAGER','STAFF')` | Advance order status (server enforces `OrderStatus` state machine) |
| PATCH | `/{id}/cancel` | path `id`; `CancelOrderRequest{reason?}` | `OrderResponse` | `hasAnyRole('MANAGER','STAFF')` | Cancel order (only from `AWAITING_PAYMENT`/`PENDING`/`CONFIRMED`/`READY_TO_SHIP`) |
| GET | `/{id}/history` *(new)* | path `id` | `List<OrderHistoryStaffResponse{id, action, fromStatus?, toStatus?, actorType, actorId?, note?, createdAt}>` | `hasAnyRole('MANAGER','STAFF')` | Staff order-history view; same as customer's history but with row `id` and the acting `actorId` |

`OrderStatus` transitions: `AWAITING_PAYMENT→{PENDING,CANCELLED}`, `PENDING→{CONFIRMED,CANCELLED}`, `CONFIRMED→{READY_TO_SHIP,CANCELLED}`, `READY_TO_SHIP→{SHIPPING,CANCELLED}`, `SHIPPING→{DELIVERED}`, terminal: `DELIVERED`, `CANCELLED`.

## 40. Payment — `payment/controller/PaymentController.java`
Base path: `/api/v1/payments`
No `@PreAuthorize` at all, class or method level — falls through to `anyRequest().authenticated()`; any authenticated role (including another customer) can request a PayOS link for an arbitrary `orderId` unless `PaymentService.createPayosLink` does its own ownership check internally.

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `/orders/{orderId}/payos-link` | path `orderId` | `PayosLinkResponse{paymentId, checkoutUrl}` | Global default (authenticated, no role restriction) | Create a PayOS checkout link for paying an existing order |

**Note:** the old doc's `WalletController`, `TopUpController`, and `PayoutController` (store-wallet/top-up/payout endpoints under `payment/*`) **no longer exist in this codebase** — only `PaymentController` and `PayOSWebhookController` remain in the `payment` module.

## 41. Payment — `payment/controller/PayOSWebhookController.java`
Base path: `/api/v1/payos`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `/webhook` | PayOS SDK `Webhook` object (external payload, not a project DTO) | Raw `String` `"OK"` (**not** wrapped in `ApiResponse`) | `permitAll()` (`SecurityConfig` explicitly lists `/api/v1/payos/webhook`) | PayOS payment-gateway webhook callback; updates payment/order status |

## 42. Voucher — `voucher/controller/CustomerVoucherController.java` *(new module)*
Base path: `/api/v1/vouchers`

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `/available` | query `page=0`, `size=20` | `Page<VoucherSummaryResponse{id, code, name, description?, discountType(PERCENT\|FIXED_AMOUNT), discountValue, maxDiscountAmount?, minOrderValue?, remainingQuantity, maxUsagePerCustomer, startDate, endDate}>` sorted `endDate asc` | Global default — **no `@PreAuthorize` and no URL matcher**; despite the Swagger description claiming "no login required," the global `anyRequest().authenticated()` rule still applies, so a Bearer token is actually required (undocumented-in-code anomaly) | List publicly available (enabled, in-window, in-stock) vouchers, soonest-expiring first |
| POST | `/validate` | `ValidateVoucherRequest{voucherCode(NotBlank, max 50), items: OrderItemRequest[](NotEmpty)}` | `VoucherPreviewResponse{voucherId, code, name, discountType, discountValue, orderValue, discountAmount, amountAfterVoucher}` | `hasRole('CUSTOMER')` | Validate a voucher code against cart items and preview the discount before checkout |

## 43. Voucher — `voucher/controller/ManagerVoucherController.java` *(new module)*
Base path: `/api/v1/manager/vouchers`
Class-level `@PreAuthorize("hasRole('MANAGER')")`; not covered by a `SecurityConfig` URL matcher.

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `CreateVoucherRequest{code?(max 50, Pattern alnum/_/-), name(NotBlank, max 150), description?(max 500), discountType(NotNull; PERCENT\|FIXED_AMOUNT), discountValue(NotNull, >0), maxDiscountAmount?(>0; PERCENT only), minOrderValue?(≥0), totalQuantity(NotNull, ≥1), maxUsagePerCustomer(NotNull, ≥1), startDate(NotNull), endDate(NotNull)}` | `VoucherResponse{id, code, name, description?, discountType, discountValue, maxDiscountAmount?, minOrderValue?, totalQuantity, usedQuantity, remainingQuantity, maxUsagePerCustomer, startDate, endDate, status(ACTIVE\|INACTIVE), displayStatus(UPCOMING\|RUNNING\|EXPIRED\|OUT_OF_STOCK\|DISABLED), createdBy: UserSummaryResponse, createdAt, updatedAt}` | `hasRole('MANAGER')` | Create voucher; omitted `code` is auto-generated; input code is case-normalized to upper |
| PUT | `/{id}` | `UpdateVoucherRequest` (same fields minus `code`) | `VoucherResponse` | `hasRole('MANAGER')` | Update voucher; if already used by a customer, only name/description/startDate/endDate and increasing `totalQuantity` are effectively editable |
| PATCH | `/{id}/status` | `UpdateVoucherStatusRequest{status(NotNull; ACTIVE\|INACTIVE)}` | `VoucherResponse` | `hasRole('MANAGER')` | Enable/disable voucher; past usages unaffected |
| GET | `` | query `status?(UPCOMING\|RUNNING\|EXPIRED\|OUT_OF_STOCK\|DISABLED)`, `keyword?`, `page=0`, `size=20` | `Page<VoucherResponse>` sorted `createdAt desc` | `hasRole('MANAGER')` | List vouchers, filterable by display status and code/name keyword |
| GET | `/{id}` | path `id` | `VoucherResponse` | `hasRole('MANAGER')` | Get voucher detail |
| GET | `/{id}/usages` | path `id`; query `page=0`, `size=20` | `Page<VoucherUsageResponse{id, voucherId, customerId, orderId, discountAmount, status(USED\|REFUNDED), usedAt, refundedAt?}>` sorted `usedAt desc` | `hasRole('MANAGER')` | Usage history for a voucher |

## 44. Request-Quote — `quote/controller/CustomerRequestQuoteController.java`
Base path: `/api/v1/request-quotes`
Class-level `@PreAuthorize("hasRole('CUSTOMER')")`.

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `RequestQuoteRequest{contactPhone(NotBlank, max 20), contactName?(max 150), content?(max 1000), items: RequestQuoteItemRequest[]{productUnitId(NotNull), quantity(≥1)}(NotEmpty)}` | `RequestQuoteResponse{id, customerId, contactPhone, contactName?, content?, status(PENDING\|IN_PROGRESS\|ORDERED\|REJECTED\|CANCELLED), handledBy?: UserSummaryResponse, handledAt?, rejectReason?, resultNote?, createdAt, items: RequestQuoteItemResponse[]{productUnitId, productId, productName, sku, unitName, quantity(int)}}` | `hasRole('CUSTOMER')` | Create a bulk-order request quote. **`handledBy` is a `UserSummaryResponse` object, not a bare UUID.** |
| GET | `/me` | query `status?`, `page=0`, `size=20` | `Page<RequestQuoteSummaryResponse{id, customerId, contactPhone, contactName?, status, itemCount(int), handledBy?: UserSummaryResponse, createdAt}>` sorted `createdAt desc` | `hasRole('CUSTOMER')` | List own request quotes |
| GET | `/{id}` | path `id` | `RequestQuoteResponse` | `hasRole('CUSTOMER')` | Get own request-quote detail |
| PUT | `/{id}` | `RequestQuoteRequest` | `RequestQuoteResponse` | `hasRole('CUSTOMER')` | Edit own request quote; only while `PENDING` |
| PATCH | `/{id}/cancel` | path `id` | `RequestQuoteResponse` | `hasRole('CUSTOMER')` | Cancel own request quote; only while `PENDING` or `IN_PROGRESS` |

## 45. Request-Quote — `quote/controller/StaffRequestQuoteController.java`
Base path: `/api/v1/staff/request-quotes`
Class-level `@PreAuthorize("hasAnyRole('STAFF','MANAGER')")`.

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `/summary` *(new)* | — | `RequestQuoteStaffSummaryResponse{totalRequests(long), waitingClaim(long), handledByMe(long), ordered(long)}` | `hasAnyRole('STAFF','MANAGER')` | Snapshot stat-card totals; `handledByMe` scoped to the logged-in staff member |
| GET | `` | query `status?`, `page=0`, `size=20` | `Page<RequestQuoteSummaryResponse>` sorted `createdAt desc` | `hasAnyRole('STAFF','MANAGER')` | List all request quotes |
| GET | `/{id}` | path `id` | `RequestQuoteResponse` | `hasAnyRole('STAFF','MANAGER')` | Get request-quote detail |
| PATCH | `/{id}/claim` | path `id` | `RequestQuoteResponse` | `hasAnyRole('STAFF','MANAGER')` | Claim: `PENDING` → `IN_PROGRESS`; 409 if already claimed by someone else |
| PATCH | `/{id}/ordered` | path `id`; `MarkOrderedRequest{resultNote?(max 500)}` (optional body) | `RequestQuoteResponse` | `hasAnyRole('STAFF','MANAGER')` | Mark order created: `IN_PROGRESS` → `ORDERED` |
| PATCH | `/{id}/reject` | path `id`; `RejectRequestQuoteRequest{reason(NotBlank, max 500)}` | `RequestQuoteResponse` | `hasAnyRole('STAFF','MANAGER')` | Reject the request quote; reason required |

`RequestQuoteStatus` transitions: `PENDING→{IN_PROGRESS,REJECTED,CANCELLED}`, `IN_PROGRESS→{ORDERED,REJECTED,CANCELLED}`; terminal: `ORDERED`, `REJECTED`, `CANCELLED`.

## 46. Shipping — `shipment/controller/ShippingController.java`
Base path: `/api/v1/shipping`
Class-level `@PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'STAFF')")` — `BAKER_STAFF` and `ADMIN` excluded.

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `/cities` | — | `List<AddressOptionResponse{code, name}>` | `hasAnyRole('CUSTOMER','MANAGER','STAFF')` | List Goship cities for address pickers |
| GET | `/cities/{cityId}/districts` | path `cityId` | `List<AddressOptionResponse>` | same | List districts in a city |
| GET | `/districts/{districtId}/wards` | path `districtId` | `List<AddressOptionResponse>` | same | List wards in a district |
| POST | `/rates` | `ShippingRatesRequest{paymentMethod(NotNull; COD\|PAYOS\|BANK_TRANSFER), provinceCode(NotBlank), districtCode(NotBlank), wardCode(NotBlank), items: OrderItemRequest[](NotEmpty)}` | `List<ShippingRateResponse{id, carrierName, carrierLogo, service, expected, codFee, totalFee, shippingFee}>` | same | Preview shipping-rate quotes before checkout; the chosen `id` becomes `shippingRateId` on order creation |

## 47. Shipping — `shipment/controller/ShipmentWebhookController.java`
Base path: `/api/v1/webhooks/goship`
No `@PreAuthorize`, and this path is **absent** from `SecurityConfig`'s `permitAll()` list (only `/api/v1/payos/webhook` and `/api/v1/media/videos/webhook` are listed) — falls under `anyRequest().authenticated()`.

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `ShipmentWebhookRequest{externalId(NotBlank), statusCode(NotNull, Integer)}` | `ApiResponse<Void>` | Global default (`authenticated()`) — **likely unreachable by Goship's actual server-to-server callback**, which cannot supply a Bearer JWT | Goship shipment-status webhook callback |

## 48. Shipping — `shipment/controller/StaffShipmentController.java`
Base path: `/api/v1/staff/orders/{orderId}/shipment`
Class-level `@PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")`.

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | path `orderId` | `ShipmentResponse{id, orderId, provider, carrier, trackingCode, status(PENDING\|PICKING\|IN_TRANSIT\|DELIVERED\|FAILED\|RETURNED\|CANCELLED), shippingFee, codAmount, weightGram(int), shippedAt?, deliveredAt?, createdAt}` | `hasAnyRole('MANAGER','STAFF')` | Get the shipment record tied to an order |

## 49. Receipt — `receipt/controller/ReceiptCategoryController.java`
Base path: `/api/v1/receipt-categories`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `ReceiptCategoryRequest{name(NotBlank, max 150), type(NotNull; SALE\|SETTLEMENT\|OTHER)}` | `ReceiptCategoryResponse{id, name, type, createdAt}` | `hasRole('MANAGER')` | Create receipt category |
| PUT | `/{id}` | `ReceiptCategoryRequest` | `ReceiptCategoryResponse` | `hasRole('MANAGER')` | Update category |
| DELETE | `/{id}` | — | `Void` | `hasRole('MANAGER')` | Delete category |
| GET | `/{id}` | path `id` | `ReceiptCategoryResponse` | `hasAnyRole('MANAGER','STAFF')` | Get category |
| GET | `` | — | `List<ReceiptCategoryResponse>` (no paging) | `hasAnyRole('MANAGER','STAFF')` | List all categories |

## 50. Receipt — `receipt/controller/ReceiptVoucherController.java`
Base path: `/api/v1/receipt-vouchers`

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `CreateReceiptVoucherRequest{receiptCategoryId(NotNull), amount(NotNull, ≥0.01), note?(max 500)}` | `ReceiptVoucherResponse{id, code, receiptCategoryId, receiptCategoryName, receiptCategoryType(SALE\|SETTLEMENT\|OTHER), paymentId?, amount, note?, createdBy: UserSummaryResponse, createdAt}` | `hasAnyRole('MANAGER','STAFF')` | Create receipt (money-in) voucher |
| GET | `/{id}` | path `id` | `ReceiptVoucherResponse` | `hasAnyRole('MANAGER','STAFF')` | Get voucher |
| GET | `` | query `categoryId?`, `page=" 0"`†, `size=10` | `Page<ReceiptVoucherResponse>` (sorted createdAt desc) | `hasAnyRole('MANAGER','STAFF')` | List vouchers, paged — †`@RequestParam(defaultValue = " 0")` still has a leading space in the default literal (confirmed still present as of this read); a request that omits `page` will fail `Integer` parsing instead of defaulting to 0. Always pass `page` explicitly |

## 51. Report — `report/controller/FinancialReportController.java` *(new module)*
Base path: `/api/v1/staff/reports`
Class-level `@PreAuthorize("hasRole('MANAGER')")`; not covered by any `SecurityConfig` URL matcher (there is still no blanket `/api/v1/staff/**` rule).

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `/financial` | query `from(yyyy-MM, required)`, `to(yyyy-MM, required)` | `FinancialReportResponse{from, to, months: MonthlyFinancial[]{month, deliveredOrders(long), grossSales, discounts, netRevenue, cogs, grossProfit, operatingExpenses, stocktakeLoss, disposalLoss, totalShrinkage, netProfit}, total: MonthlyFinancial}` (`total.month = "TOTAL"`) | `hasRole('MANAGER')` | Monthly P&L report for `[from, to]` inclusive, plus a `TOTAL` row; max 36 months per call; `from` must not be after `to`. `netProfit = grossSales − discounts − cogs − operatingExpenses − stocktakeLoss − disposalLoss`, dated by delivery date (orders/COGS), voucher creation date (expenses), and completion date (stocktake/disposal) respectively |

## 52. Warehouse — `warehouse/controller/SupplierController.java`
Base path: `/api/suppliers` (note: **no `/v1` prefix**)
No `@PreAuthorize` anywhere in the class — Global default (authenticated, any role).

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `SupplierRequest{supplierName(NotBlank, max 255), phoneNumber?(max 20), email?(Email, max 100), address?(max 5000)}` (201) | `SupplierResponse{supplierId, supplierName, phoneNumber?, email?, address?, createdAt}` | Global default | Create supplier |
| PUT | `/{id}` | `SupplierRequest` | `SupplierResponse` | Global default | Update supplier |
| GET | `/{id}` | path `id` | `SupplierResponse` | Global default | Get supplier |
| GET | `/{id}/stock-in-notes` *(new)* | path `id`; query `status?(PENDING\|COMPLETED\|CANCELLED)`, `Pageable` (default `size=20, sort=createdAt DESC`) | `Page<StockInNote>` — **raw JPA entity** (see §55 field list) | Global default | Stock-in history for a supplier |
| GET | `` | query `keyword?`, `Pageable` (default `size=20, sort=supplierName ASC`) | `Page<SupplierResponse>` | Global default | List/search suppliers |
| DELETE | `/{id}` | — | `Void` | Global default | Delete supplier |

## 53. Warehouse — `warehouse/controller/SupplierPaymentController.java` *(new controller)*
Base path: `/api/suppliers/{supplierId}/payments`
No `@PreAuthorize` — Global default (authenticated, any role).

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | path `supplierId`; `SupplierPaymentCreateRequest{amount(NotNull, ≥0.01), details: Allocation[]{stockInNoteId(NotNull), amount(NotNull, ≥0.01)}(NotEmpty)}` (201) | `SupplierPaymentDetailResponse{payment: SupplierPaymentResponse{id, code, supplierId, amount, createdAt, createdBy: UserSummaryResponse}, details: Allocation[]{id, stockInNoteId, stockInNoteCode, amount}}` | Global default | Record a payment to a supplier, allocated across one or more stock-in notes (pays down their `debtAmount`) |
| GET | `` | path `supplierId`; `Pageable` (default `size=20, sort=createdAt DESC`) | `Page<SupplierPaymentResponse>` | Global default | List payments made to a supplier |
| GET | `/{paymentId}` | path `supplierId, paymentId` | `SupplierPaymentDetailResponse` | Global default | Get one supplier payment with its stock-in-note allocations |

## 54. Warehouse — `warehouse/controller/InventoryController.java`
Base path: `/api/warehouse/inventory` (note: **no `/v1` prefix**)
No `@PreAuthorize` — Global default (authenticated, any role).

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `/lots` | query `productId`(required) | `List<StockLot>` — **raw JPA entity**: `{lotId, productId, stockInNoteId, lotCode, initialQuantity, currentQuantity, unitCost, expiryDate, status(STORAGE\|EXPIRED\|CANCELED\|EMPTY), createdAt}` | Global default | List stock lots for a product, ordered by expiry then created date |
| GET | `/expiring-lots` | query `withinDays=30(0-3650)` | `List<StockLot>` (raw) | Global default | Lots with status `STORAGE` expiring within N days |
| GET | `/expired-lots` | — | `List<StockLot>` (raw) | Global default | Expired lots with positive remaining quantity |

## 55. Warehouse — `warehouse/controller/StockInController.java`
Base path: `/api/warehouse/stock-in` (note: **no `/v1` prefix**)
No class-level `@PreAuthorize`; one method (`/summary`) carries its own.

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `StockInCreateRequest{supplierId(NotNull), receiptDate?(PastOrPresent), paidAmount?(≥0.00, 13int/2frac), note?(max 5000), items: StockInItemRequest[]{productId(NotNull), unitId(NotNull), quantity(NotNull, ≥0.01, 8int/2frac), unitPrice(NotNull, ≥0.01, 13int/2frac), expiryDate(NotNull, FutureOrPresent)}(NotEmpty)}` (201) | `StockInNote` — **raw entity**: `{noteId, code, supplierId, receiptDate, totalAmount, paidAmount, debtAmount, paymentStatus(UNPAID\|PARTIAL\|PAID), note, status(PENDING\|COMPLETED\|CANCELLED), updatedAt, createdAt}` | Global default | Create stock-in note (status `PENDING`) |
| PUT | `/{id}` *(new)* | path `id`; `StockInUpdateRequest` (same shape as create) | `StockInDetailResponse{note: StockInNote, items: List<StockInItem>}` (raw entities) | Global default | Edit a pending stock-in note's items/supplier/amounts before completing it |
| POST | `/{id}/complete` | path `id`; every item needs `expiryDate` today or later | `StockInNote` (raw) | Global default | Complete stock-in note; generates `StockLot` rows |
| POST | `/{id}/cancel` | path `id` | `StockInNote` (raw) | Global default | Cancel stock-in note |
| GET | `` | query `status?(PENDING\|COMPLETED\|CANCELLED)`, `Pageable` (default `size=20, sort=createdAt DESC`) | `Page<StockInNote>` (raw) | Global default | List stock-in notes |
| GET | `/summary` *(new)* | — | `StockInStatusSummaryResponse{totalNotes(long), pending(long), totalDebt, cancelled(long)}` | `hasAnyRole('MANAGER','STAFF')` (method-level `@PreAuthorize`, the only secured endpoint in this controller) | Stat-card summary of stock-in notes |
| GET | `/{id}` | path `id` | `StockInDetailResponse{note: StockInNote, items: List<StockInItem>}` (raw entities, `StockInItem` now also has `conversionFactor`, `totalPrice`) | Global default | Get stock-in note with its line items |

## 56. Warehouse — `warehouse/controller/StockOutController.java`
Base path: `/api/warehouse/stock-out` (note: **no `/v1` prefix**)
No `@PreAuthorize` — Global default (authenticated, any role).

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `StockOutCreateRequest{type(NotNull; SALE\|DISPOSAL), referenceOrderId?, note?(max 5000), items: StockOutItemRequest[]{productId(NotNull), unitId(NotNull), lotId?, quantity(NotNull, ≥0.01, 8int/2frac)}(NotEmpty)}` (201) | `StockOutNote` — **raw entity**: `{noteId, code, type, referenceOrderId?, totalCost, note?, status(PENDING\|COMPLETED\|CANCELLED), updatedAt, createdAt}` | Global default | Create stock-out note (sale or disposal) |
| POST | `/{id}/complete` | path `id` | `StockOutNote` (raw) | Global default | Complete stock-out note; deducts lot quantities |
| POST | `/{id}/cancel` | path `id` | `StockOutNote` (raw) | Global default | Cancel stock-out note |
| POST | `/expired-lots/{lotId}/dispose` | path `lotId` | `StockOutNote` (raw, `COMPLETED`) | Global default | Create and complete an `EXPIRED`/`DISPOSAL` note for the lot's entire remaining quantity |
| GET | `` | query `status?`, `type?(SALE\|DISPOSAL)`, `Pageable` (default `size=20, sort=createdAt DESC`) | `Page<StockOutNote>` (raw) | Global default | List stock-out notes |
| GET | `/{id}` | path `id` | `StockOutDetailResponse{note: StockOutNote, items: List<StockOutItem>}` (raw entities) | Global default | Get stock-out note with line items |

## 57. Warehouse — `warehouse/controller/StockTransactionController.java` *(new controller)*
Base path: `/api/warehouse/stock-transactions` (note: **no `/v1` prefix**)

| Method | Path | Request/Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | query `productId?`, `stockLotId?`, `transactionType?(STOCK_IN\|SALE\|DISPOSAL\|STOCKTAKE_INCREASE\|STOCKTAKE_DECREASE\|SALE_REVERSAL)`, `refType?(STOCK_IN_NOTE\|STOCK_OUT_NOTE\|STOCKTAKE_NOTE)`, `refId?`, `from?(ISO date-time)`, `to?(ISO date-time)`, `Pageable` (default `size=20, sort=createdAt DESC`) | `Page<StockTransactionResponse{id, stockLotId, productId, transactionType, quantityChange, balanceAfter, refType, refId, createdBy: UserSummaryResponse, createdAt}>` | `hasAnyRole('MANAGER','STAFF')` (method-level) | Audit-log style ledger of every stock-quantity movement across lots, filterable by product/lot/type/reference/time range |

## 58. Warehouse — `warehouse/controller/StocktakeController.java`
Base path: `/api/warehouse/stocktakes` (note: **no `/v1` prefix**)
No `@PreAuthorize` — Global default (authenticated, any role).

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `StocktakeCreateRequest{productIds: UUID[](NotEmpty), handleBy?}` (201) | `StocktakeDetailResponse{note: StocktakeNote(raw: {noteId, code, stocktakeDate, handleBy?, createdBy, status(DRAFT\|COMPLETED\|CANCELLED), updatedAt, createdAt}), products: ProductLots[]{productId, lots: List<StocktakeItem>(raw)}}` | Global default | Start a stocktake (DRAFT), snapshotting current lots for the given products |
| PUT | `/{noteId}/counts` | `StocktakeCountRequest{products: [{productId(NotNull), lots: [{itemId(NotNull), actualQuantity(NotNull, ≥0.00, 8int/2frac), reason?(max 255), note?(max 5000)}](NotEmpty)}](NotEmpty)}` | `StocktakeDetailResponse` | Global default | Record counted quantities per lot |
| POST | `/{noteId}/complete` | path `noteId` | `StocktakeDetailResponse` | Global default | Complete stocktake; applies quantity adjustments |
| POST | `/{noteId}/cancel` | path `noteId` | `StocktakeNote` (raw entity) | Global default | Cancel stocktake |
| GET | `` | query `status?(DRAFT\|COMPLETED\|CANCELLED)`, `Pageable` (default `size=20, sort=createdAt DESC`) | `Page<StocktakeNote>` (raw) | Global default | List stocktakes |
| GET | `/{noteId}` | path `noteId` | `StocktakeDetailResponse` | Global default | Get stocktake detail |

---

## Notes / anomalies to flag for frontend planning

1. **Inconsistent prefixes**: `product/controller/CategoryController` (`/api/product-categories`), `product/controller/ProductController` (`/api/products`), `warehouse/controller/SupplierController` (`/api/suppliers`), `warehouse/controller/SupplierPaymentController` (`/api/suppliers/{supplierId}/payments`), and all four warehouse note/inventory controllers (`/api/warehouse/**`) omit the `/v1` segment that the rest of the API uses. Confirm with backend before wiring these up.
2. **`ProductController.setComboItems`** echoes the request type (`List<ComboItemRequest>`), not a dedicated response DTO, and **`ProductController.comboStock`** returns a raw `BigDecimal` — neither has a named response DTO.
3. **`PayOSWebhookController`** is correctly `permitAll()` in `SecurityConfig` (confirmed current, not a gap).
4. **`ShipmentWebhookController` (`POST /api/v1/webhooks/goship`) is a PayOS-style external gap, still unfixed**: no `@PreAuthorize`, and the path is **not** in `SecurityConfig`'s `permitAll()` list, so it falls under `anyRequest().authenticated()`. Goship's own server-to-server callback almost certainly cannot supply a Bearer JWT — flag for backend before depending on shipment-status push updates.
5. **`/api/v1/notifications/**`** is `permitAll()` at the Spring Security filter-chain level, but every controller method carries `@PreAuthorize("isAuthenticated()")`, so the frontend still needs a valid Bearer token in practice.
6. **Two incompatible pagination shapes coexist, and the split has grown since the last audit.** Custom `recipe.dto.response.PageResponse<T>{content, totalElements, totalPages, currentPage}` is used by: `RecipeController.search`, `RecipeCategoryController`, `IngredientController`, `ToolController`, `RecipeStepController`, `RecipeCommentController`, `RecipeRatingController`, `RecipeBookmarkController`, `RecipeInteractionController`, and `IngredientCatalogManagementController.unlinkedGroups`. Spring's native `Page<T>` (`content, totalElements, totalPages, number, size, …, pageable, sort`) is used by essentially everything else that paginates: `ExpenseVoucherController`, `NotificationController`, `ProductController.list`, `CustomerOrderController.myOrders`, `StaffOrderController.getAll`, `CustomerRequestQuoteController.mine`, `StaffRequestQuoteController.list`, `CustomerVoucherController.available`, `ManagerVoucherController` (list + usages), `ReceiptVoucherController.getAll`, `CustomerLoyaltyController`/`AdminLoyaltyController` transactions, `SupplierController` (list + stock-in-notes), `SupplierPaymentController.list`, `StockInController.list`, `StockOutController.list`, `StockTransactionController.list`, `StocktakeController.list`. Always check the specific endpoint's row above rather than assuming by module.
7. Delete endpoints across the `recipe/*` module (`RecipeController`, `RecipeCategoryController`, `IngredientController`, `ToolController`, `RecipeStepController`, `RecipeStepIngredientController`, `RecipeStepToolController`, `RecipeCommentController`, `RecipeRatingController`, `RecipeBookmarkController`, `RecipeIngredientController`, `RecipeToolController`) return a bare `String` `"Deleted"` wrapped in `ApiResponse<String>`, not `Void`.
8. **Warehouse responses mostly leak raw JPA entities** instead of DTOs: `InventoryController` returns `List<StockLot>` directly; `StockInController`/`StockOutController`/`StocktakeController`/`SupplierController.stockInHistory` return `StockInNote`/`StockOutNote`/`StocktakeNote` (and nested `StockInItem`/`StockOutItem`/`StocktakeItem`) directly, wrapped only in `ApiResponse<T>`. These are Hibernate `@Entity` classes with JPA annotations and lazy-loaded fields — treat their field shapes as unstable/implementation-detail. The two *new* warehouse controllers (`StockTransactionController`, `SupplierPaymentController`) break this pattern and correctly return hand-written DTOs.
9. **`ReceiptVoucherController.getAll` still has the malformed default**: `@RequestParam(defaultValue = " 0") int page` — the literal is `" 0"` (leading space), not `"0"`. A request omitting `page` will likely 400/500 instead of defaulting to page 0. Always pass `page` explicitly on this endpoint.
10. **No blanket `/api/v1/staff/**` rule exists in `SecurityConfig`.** Staff-role protection for `StaffOrderController`, `StaffShipmentController`, `StaffCustomerAddressController`, `StaffRequestQuoteController`, and the new `FinancialReportController` (`/api/v1/staff/reports`) relies entirely on each controller's own class-level `@PreAuthorize`, not a URL-pattern matcher. Any future endpoint added under `/api/v1/staff/**` without its own `@PreAuthorize` would silently fall through to `anyRequest().authenticated()`.
11. **`PaymentController` (`POST /api/v1/payments/orders/{orderId}/payos-link`) has no `@PreAuthorize` at all** and its base path isn't in the `SecurityConfig` matcher list either, so it's reachable by any authenticated user regardless of role; ownership of `orderId` is enforced (if at all) only inside `PaymentService.createPayosLink`.
12. **The `WalletController`/`TopUpController`/`PayoutController` store-wallet endpoints documented in a prior version of this file no longer exist in the codebase** — the `payment` module now contains only `PaymentController` and `PayOSWebhookController`. Do not build against the old wallet/top-up/payout endpoints.
13. **`CustomerVoucherController.available` (`GET /api/v1/vouchers/available`) is documented in Swagger as not requiring login, but no code enforces that**: there's no `@PreAuthorize` and no `SecurityConfig` URL matcher for `/api/v1/vouchers/**`, so the global `anyRequest().authenticated()` rule applies and a Bearer token is actually required. Confirm with backend whether the intent is to make this endpoint truly public.
14. **Warehouse module auth is now a patchwork, not uniformly open.** Most of `InventoryController`, `StockInController`, `StockOutController`, `StocktakeController`, `SupplierController`, `SupplierPaymentController` still have zero `@PreAuthorize` (any authenticated role, including `CUSTOMER`, can call them) — but `StockInController.summary()` and the entirety of `StockTransactionController` are now method-level `hasAnyRole('MANAGER','STAFF')`. Check each endpoint individually.
15. **Pagination defaults vary by module** for hand-rolled `page`/`size` query params (not bound `Pageable`): most default to `page=0,size=20`; `NotificationController` defaults to `size=10`; `ReceiptVoucherController` defaults to `size=10` (and has the `page` bug in #9); loyalty endpoints cap `size` at 100 server-side (`Math.min(size, 100)`), silently clamping rather than rejecting an oversized request.
16. **`RecipeResponse.ingredients`/`.tools` use the catalog-shaped `IngredientResponse`/`ToolResponse` DTOs** (`recipe/dto/response/IngredientResponse.java`, `ToolResponse.java`), which are the *same* classes returned by the catalog-level `IngredientController`/`ToolController` list endpoints (§20/§21) — not the link-entity DTOs `RecipeIngredientResponse`/`RecipeToolResponse` used by the new `RecipeIngredientController`/`RecipeToolController` (§22/§23). The two shapes carry different field sets (e.g. `ToolResponse` has `name`+`quantity`+`recipeId`, no separate `toolId`; `RecipeToolResponse` has both `toolId` and `toolName`). Do not conflate them when generating FE types.
