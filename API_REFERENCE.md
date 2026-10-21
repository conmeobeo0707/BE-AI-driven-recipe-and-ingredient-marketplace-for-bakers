# Backend REST API Inventory
**Repo:** `BE-AI-driven-recipe-and-ingredient-marketplace-for-bakers`
**Base package:** `com.manhnv.capstonebakerai`
**Response envelope:** every endpoint below (except `PayOSWebhookController`, which returns a raw `String`) wraps its payload in `ApiResponse<T>`:
```json
{ "message": "string | null", "success": true, "data": { /* T */ } }
```
(`ApiResponse.success(data)` sets `success=true` with no message; `ApiResponse.error(message)` sets `success=false`.)

**Global auth rules** (`identity/config/SecurityConfig.java`), on top of any per-endpoint `@PreAuthorize`:
- Public (no token needed): `POST /api/v1/auth/{register,verify-email,resend-verification,login,refresh,logout,forgot-password,forgot-password/verify,reset-password}`, Swagger/OpenAPI paths.
- `/api/v1/notifications/**` — `permitAll()` in the filter chain (note: controller methods still carry `@PreAuthorize("isAuthenticated()")`, so in practice a token is still required at the method level despite the URL-level permitAll).
- `/api/v1/admin/accounts/**` — `hasRole('ADMIN')`.
- `/api/v1/users/me/**` — any authenticated user.
- `/api/v1/customers/me/**` — `hasRole('CUSTOMER')`.
- `/api/v1/admin/hobbies/**` — `hasRole('ADMIN')`.
- `GET /api/v1/hobbies` — `hasAnyRole('CUSTOMER','ADMIN')`.
- Everything else not explicitly listed — `anyRequest().authenticated()`.
- JWT roles arrive as the `role` claim, mapped to Spring authorities prefixed `ROLE_`. Roles enum: `ADMIN, MANAGER, STAFF, BAKER_STAFF, CUSTOMER`.
- `X-Auth-Request: 1` header required on `POST /api/v1/auth/refresh` and `POST /api/v1/auth/logout` (CSRF mitigation for cookie-based refresh token).

---

## 1. Identity — `identity/controller/AuthController.java`
Base path: `/api/v1/auth`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `/register` | `RegisterUserRequest{fullName, email, password, phone?, address?, dateOfBirth?, gender?}` | `RegisterUserResponse{id, fullName, email, role, phone, status, address, dateOfBirth, gender, createdAt}` | Public | Self-register as CUSTOMER only; creates User+Customer, sends verification email |
| POST | `/verify-email` | `VerifyEmailRequest{email, code(6 digits)}` | `MessageResponse{message}` | Public | Confirm email via 6-digit code |
| POST | `/resend-verification` | `ResendVerificationRequest{email}` | `MessageResponse{message}` | Public | Resend email verification code |
| POST | `/forgot-password` | `ForgotPasswordRequest{email}` | `MessageResponse{message}` | Public | Request password-reset code (same response regardless of account existence) |
| POST | `/forgot-password/verify` | `VerifyPasswordResetCodeRequest{email, code}` | `MessageResponse{message}` | Public | Verify reset code without consuming it |
| POST | `/reset-password` | `ResetPasswordRequest{email, code, newPassword}` | `MessageResponse{message}` | Public | Reset password, revokes refresh tokens, clears refresh cookie |
| POST | `/login` | `LoginRequest{email, password}` | `LoginResponse{accessToken, tokenType, expiresIn, userId, email, role}` (+ sets `refresh_token` HttpOnly cookie) | Public | Login, 15-min access JWT, 30-day refresh cookie |
| POST | `/refresh` | Cookie `refresh_token`; Header `X-Auth-Request: 1` (no body) | `LoginResponse` (+ rotates refresh cookie) | Public (requires valid refresh cookie + header) | Rotate access/refresh token pair |
| POST | `/logout` | Cookie `refresh_token`; Header `X-Auth-Request: 1` (no body) | `MessageResponse{message}` | Public (requires header) | Revoke refresh token chain, clear cookie |

## 2. Identity — Admin account management — `user/controller/AdminAccountController.java`
Base path: `/api/v1/admin/accounts`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `CreateStaffAccountRequest{fullName, email, role(STAFF\|MANAGER\|BAKER_STAFF), phone?, address?, dateOfBirth?, gender?}` | `StaffAccountResponse{id, fullName, email, role, status, phone, address, dateOfBirth, gender, avatarUrl, createdAt, updatedAt}` | `ROLE_ADMIN` (URL-level) | Create STAFF/MANAGER/BAKER_STAFF account, random password emailed via forgot-password flow |
| GET | `` | — | `List<StaffAccountResponse>` (no paging) | `ROLE_ADMIN` | List all staff/manager/baker_staff accounts |
| GET | `/{id}` | path `id` | `StaffAccountResponse` | `ROLE_ADMIN` | Get one staff account |
| PUT | `/{id}` | `UpdateStaffAccountRequest{fullName, role, phone?, address?, dateOfBirth?, gender?}` | `StaffAccountResponse` | `ROLE_ADMIN` | Update staff account; revokes refresh tokens on role/status change |
| PATCH | `/{id}/status` | `UpdateAccountStatusRequest{status(ACTIVE\|BLOCKED\|INACTIVE)}` | `StaffAccountResponse` | `ROLE_ADMIN` | Change account status |

## 3. User — `user/controller/UserProfileController.java`
Base path: `/api/v1/users/me`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | — | `UserProfileResponse{id, fullName, email, role, status, phone, address, dateOfBirth, gender, avatarUrl, createdAt, updatedAt}` | Authenticated (any role, URL-level) | Get own profile |
| PUT | `` | `UpdateUserProfileRequest{fullName?, phone?, address?, dateOfBirth?, gender?}` | `UserProfileResponse` | Authenticated | Update own profile |
| PUT | `/avatar` (multipart) | form field `file` (MultipartFile) | `UserProfileResponse` | Authenticated | Upload/replace avatar |
| DELETE | `/avatar` | — | `UserProfileResponse` | Authenticated | Remove avatar |

## 4. Customer — `customer/controller/CustomerProfileController.java`
Base path: `/api/v1/customers/me`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | — | `CustomerProfileResponse{customerId, skillLevel(BEGINNER\|INTERMEDIATE\|ADVANCED), emailVerified, hobbies: HobbyResponse[]}` | `ROLE_CUSTOMER` (URL-level) | Get own customer profile |
| PUT | `` | `UpdateCustomerProfileRequest{skillLevel?}` | `CustomerProfileResponse` | `ROLE_CUSTOMER` | Update skill level |
| PUT | `/hobbies` | `UpdateCustomerHobbiesRequest{hobbyIds: UUID[]}` | `CustomerProfileResponse` | `ROLE_CUSTOMER` | Replace own hobby list |

## 5. Hobby — `hobby/controller/HobbyController.java` (public-facing) and `hobby/controller/AdminHobbyController.java` (admin CRUD)

Base path `/api/v1/hobbies`:

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | — | `List<HobbyResponse{id, name, description, active}>` | `hasAnyRole('CUSTOMER','ADMIN')` (URL-level) | List active hobbies only |

Base path `/api/v1/admin/hobbies`:

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | — | `List<HobbyResponse>` | `ROLE_ADMIN` (URL-level) | List all hobbies (active + inactive) |
| GET | `/{id}` | path `id` | `HobbyResponse` | `ROLE_ADMIN` | Get hobby by id |
| POST | `` | `SaveHobbyRequest{name, description?}` | `HobbyResponse` (201) | `ROLE_ADMIN` | Create hobby |
| PUT | `/{id}` | `SaveHobbyRequest{name, description?}` | `HobbyResponse` | `ROLE_ADMIN` | Update hobby |
| PATCH | `/{id}/active` | `UpdateHobbyActiveRequest{active: boolean}` | `HobbyResponse` | `ROLE_ADMIN` | Toggle active flag |
| DELETE | `/{id}` | — | `HobbyResponse` | `ROLE_ADMIN` | Soft-delete (sets active=false) |

## 6. Cart — `cart/controller/CartController.java`
Base path: `/api/v1/carts`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | — | `CartResponse{id, items: CartItemResponse[], totalItems, totalAmount}` where `CartItemResponse{id, productId, productName, imgUrl, uomId, unitName, unitPrice, quantity, subtotal, addedAt}` | `hasAnyRole('CUSTOMER','STAFF','MANAGER')` | Get current user's cart |
| POST | `/items` | `AddCartItemRequest{productId, uomId, quantity(>0, 3dp)}` | `CartResponse` | same | Add item to cart |
| PATCH | `/items/{itemId}` | `UpdateCartItemQuantityRequest{quantity(>0, 3dp)}` | `CartResponse` | same | Update item quantity |
| DELETE | `/items/{itemId}` | path `itemId` | `CartResponse` | same | Remove item from cart |

## 7. Expense — `expense/controller/ExpenseCategoryController.java`
Base path: `/api/v1/expense-categories`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `ExpenseCategoryRequest{name, type(SALARY\|RENT\|MARKETING\|SHIPPING\|UTILITY\|OTHER)}` | `ExpenseCategoryResponse{id, name, type, createdAt}` | `hasAnyRole('MANAGER','STAFF')` | Create expense category |
| PUT | `/{id}` | `ExpenseCategoryRequest` | `ExpenseCategoryResponse` | same | Update category |
| DELETE | `/{id}` | — | `Void` | same | Delete category |
| GET | `` | — | `List<ExpenseCategoryResponse>` | same | List categories |

## 8. Expense — `expense/controller/ExpenseVoucherController.java`
Base path: `/api/v1/expense-vouchers`

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `CreateExpenseVoucherRequest{expenseCategoryId, amount(≥0.01), reason?}` | `ExpenseVoucherResponse{id, code, expenseCategoryId, expenseCategoryName, amount, reason, status(ACTIVE\|CANCELLED), createdBy, createdAt}` | `hasAnyRole('MANAGER','STAFF')` | Create expense voucher |
| PUT | `/{id}` | `UpdateExpenseVoucherRequest{expenseCategoryId, amount, reason?}` | `ExpenseVoucherResponse` | same | Update voucher |
| PATCH | `/{id}/cancel` | `CancelExpenseVoucherRequest{cancelReason?}` (optional body) | `ExpenseVoucherResponse` | same | Cancel voucher |
| GET | `/{id}` | path `id` | `ExpenseVoucherResponse` | same | Get voucher by id |
| GET | `` | `Pageable` (page/size/sort) | `Page<ExpenseVoucherResponse>` | same | List vouchers, paged |
| GET | `/report` | query `month?(yyyy-MM)`, `categoryId?` | `ExpenseReportResponse{month, categoryId, totalAmount, totalVouchers, categories: ExpenseCategorySummary[]}` where `ExpenseCategorySummary{categoryId, categoryName, categoryType, totalAmount, voucherCount}` | same | Aggregated expense report |
| GET | `/report-detail` | query `month?`, `categoryId?`, `page=0`, `size=20` | `Page<ExpenseVoucherResponse>` (sorted by createdAt desc) | same | Drill-down voucher list behind a report |

## 9. Notification — `notification/controller/NotificationController.java`
Base path: `/api/v1/notifications`

| Method | Path | Request/Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | query `read?(boolean)`, `page=0`, `size=10` | `Page<NotificationResponse{id, title, content, refType(ORDER\|COMMENT\|RATING), refId, read, readAt, createdAt}>` (sorted createdAt desc) | `isAuthenticated()` (method-level; URL is permitAll at filter-chain level) | List own notifications |
| GET | `/unread-count` | — | `UnreadCountResponse{unreadCount}` | `isAuthenticated()` | Count unread |
| GET | `/{id}` | path `id` | `NotificationResponse` | `isAuthenticated()` | Get one notification |
| PATCH | `/{id}/read` | path `id` | `NotificationResponse` | `isAuthenticated()` | Mark one as read |
| PATCH | `/read-all` | — | `MarkAllReadResponse{updatedCount}` | `isAuthenticated()` | Mark all as read |

## 10. Payment — `payment/controller/WalletController.java`
Base path: `/api/v1/wallet/{storeId}`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | path `storeId` | `WalletResponse{storeWalletId, storeId, totalBalance, lockedBalance, availableBalance, createdAt}` | None configured (falls to `anyRequest().authenticated()` global default) | Get store wallet |
| POST | `` | path `storeId` | `WalletResponse` | same (global default) | Create wallet for a store |

## 11. Payment — `payment/controller/TopUpController.java`
Base path: `/api/v1/payment/{storeId}/topup-transactions`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | path `storeId`; `TopUpRequest{amount(≥10000)}` | `TopUpResponse{topUpId, storeId, amount, status(PENDING\|SUCCESS\|FAILED\|CANCELLED), checkoutUrl, createdAt}` | Global default (authenticated) | Create PayOS top-up checkout session |
| GET | `` | path `storeId`; `Pageable` | `Page<TopUpTransactionResponse{topupId, amount, paymentMethod(BANK_TRANSFER\|CASH), externalTransactionId, status, createdAt, updatedAt}>` | Global default | Top-up history for a store |
| GET | `/{topUpId}` | path `storeId, topUpId` | `TopUpTransactionResponse` | Global default | Get one top-up transaction |

## 12. Payment — `payment/controller/PayoutController.java`
Base path: `/api/v1/payouts`
Note: `@PreAuthorize("hasRole('ADMIN')")` is commented out with `// TODO` on approve/reject — currently only the global `anyRequest().authenticated()` applies.

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `/stores/{storeId}` | path `storeId`; `PayoutRequest{amount(≥10000)}` | `PayoutResponse{payoutId, storeId, amount, status(PENDING\|APPROVED\|REJECTED\|COMPLETED\|FAILED), processedBy, createdAt, rejectReason, approvedAt, rejectedAt, completedAt, failedAt, updatedAt}` (raw, not wrapped in ApiResponse) | Global default (authenticated) | Store requests a payout |
| PUT | `/{payoutId}/approve` | path `payoutId` | `PayoutResponse` (raw) | **TODO: should be ADMIN**, currently just authenticated | Approve payout → triggers payment |
| PUT | `/{payoutId}/reject` | path `payoutId`; `PayoutRejectRequest{reason}` | `PayoutResponse` (raw) | **TODO: should be ADMIN**, currently just authenticated | Reject payout, refund to availableBalance |

## 13. Payment — `payment/controller/PayOSWebhookController.java`
Base path: `/api/v1/payos`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `/webhook` | PayOS SDK `Webhook` object (external payload, not a project DTO) | Raw `String` `"OK"` (NOT wrapped in ApiResponse) | Global default (authenticated) — **likely a bug/gap**: PayOS calls this server-to-server and probably cannot supply a JWT; verify with backend team whether this route needs to be added to the public matcher list | PayOS payment gateway webhook callback (updates top-up status) |

## 14. Product — `product/controller/CategoryController.java`
Base path: `/api/product-categories` (note: **no `/v1` prefix**, inconsistent with the rest of the API)

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `CategoryRequest{categoryName}` | `CategoryResponse{productCategoryId, categoryName, createdAt}` (201) | Global default (authenticated) | Create product category |
| PUT | `/{id}` | `CategoryRequest{categoryName}` | `CategoryResponse` | Global default | Update category |
| GET | `` | — | `List<CategoryResponse>` | Global default | List categories |
| DELETE | `/{id}` | — | `Void` | Global default | Delete category |

## 15. Product — `product/controller/ProductController.java`
Base path: `/api/products` (note: **no `/v1` prefix**)

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `ProductUpsertRequest{sku, name, description?, categoryId?, imgUrl?, discountPercent?, storageGuide?, minStock?, maxStock?, status?, brand?, combo?, units: ProductUnitRequest[]}` where `ProductUnitRequest{unitId?, unitName, conversionFactor, base, sale?, sellingPrice, status?}` | `ProductResponse{productId, sku, name, description, categoryId, imgUrl, discountPercent, storageGuide, minStock, maxStock, stockQuantity, status(ACTIVE\|INACTIVE\|DISCONTINUE), brand, combo, createdAt, updatedAt, units: ProductUnitResponse[]}` (201) | Global default | Create product |
| PUT | `/{productId}` | `ProductUpsertRequest` | `ProductResponse` | Global default | Update product |
| GET | `/{productId}` | path `productId` | `ProductResponse` | Global default | Get product |
| GET | `` | query `categoryId?`, `brand?`, `Pageable` | `Page<ProductResponse>` | Global default | List/search products |
| DELETE | `/{productId}` | — | `Void` | Global default | Deactivate product |
| GET | `/{productId}/units/{unitId}/price-history` | path `productId, unitId` | `List<UnitPriceHistoryResponse{unitHistoryId, unitId, updatedBy, sellingPrice, createdAt}>` | Global default | Price history for a unit |
| PUT | `/{productId}/combo-items` | `List<ComboItemRequest{productId, quantity}>` | `List<ComboItemRequest>` (echoes request — response DTO is same as request type) | Global default | Set combo (bundle) items for a combo product |
| GET | `/{productId}/combo-stock` | path `productId` | `BigDecimal` (raw number, not wrapped in a named DTO) | Global default | Calculate computed stock for a combo product |

## 16. Recipe — `recipe/controller/RecipeController.java`
Base path: `/api/v1/recipes`

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `/{recipeId}` | path `recipeId` | `RecipeResponse{id, userId, categoryId, title, description, videoId, videoUrl, imageIds, imageUrls, difficulty(EASY\|MEDIUM\|HARD), prepTimeMinutes, cookTimeMinutes, servings, recipeStatus(DRAFT\|PUBLISHED\|HIDDEN), createdAt, tools: ToolResponse[], ingredients: IngredientResponse[], steps: RecipeStepResponse[]}` | Global default | Get recipe detail (with nested tools/ingredients/steps) |
| POST | `` | `RecipeRequest{categoryId?, title, description?, videoId?, imageIds?, difficulty?, prepTimeMinutes?, cookTimeMinutes?, servings?, recipeStatus?, tools: ToolEmbeddedRequest[]{name}, ingredients: IngredientEmbeddedRequest[]{name, unit, quantity}, steps: RecipeStepEmbeddedRequest[]{stepNumber, instruction, imageIds}}` | `RecipeResponse` | Global default | Create recipe with nested tools/ingredients/steps in one call |
| PUT | `/{recipeId}` | `RecipeRequest` | `RecipeResponse` | Global default | Update recipe (full replace of nested lists) |
| DELETE | `/{recipeId}` | — | `String` `"Deleted"` | Global default | Delete recipe |
| GET | `/search` | query `userId?`, `categoryId?`, `search?`, `difficulty?`, `recipeStatus?`, `Pageable` (default sort createdAt desc) | `PageResponse<RecipeResponse>` where `PageResponse<T>{content, totalElements, totalPages, currentPage}` | Global default | Search/filter recipes |

## 17. Recipe — `recipe/controller/RecipeCategoryController.java`
Base path: `/api/v1/recipe-categories`

| Method | Path | Request/Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | query `search?`, `Pageable` (default sort name asc) | `PageResponse<RecipeCategoryResponse{id, name}>` | Global default | List categories |
| GET | `/{categoryId}` | path | `RecipeCategoryResponse` | Global default | Get category |
| POST | `` | `RecipeCategoryRequest{name}` | `RecipeCategoryResponse` | Global default | Create category |
| PUT | `/{categoryId}` | `RecipeCategoryRequest{name}` | `RecipeCategoryResponse` | Global default | Update category |
| DELETE | `/{categoryId}` | — | `String` `"Deleted"` | Global default | Delete category |

## 18. Recipe — `recipe/controller/IngredientController.java`
Base path: `/api/v1/ingredients`

| Method | Path | Request/Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | query `search?`, `Pageable` (default sort name asc) | `PageResponse<IngredientResponse{id, recipeId, name, unit, quantity}>` | Global default | List all ingredients |
| GET | `/{ingredientId}` | path | `IngredientResponse` | Global default | Get ingredient |
| GET | `/recipe/{recipeId}` | path `recipeId`; query `search?`, `Pageable` | `PageResponse<IngredientResponse>` | Global default | List ingredients for a recipe |
| POST | `` | `IngredientRequest{recipeId, name, unit, quantity}` | `IngredientResponse` | Global default | Add ingredient to a recipe |
| PUT | `/{ingredientId}` | `IngredientRequest` | `IngredientResponse` | Global default | Update ingredient |
| DELETE | `/{ingredientId}` | — | `String` `"Deleted"` | Global default | Delete ingredient |

## 19. Recipe — `recipe/controller/RecipeStepController.java`
Base path: `/api/v1/recipe-steps`

| Method | Path | Request/Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | query `search?`, `Pageable` (default sort stepNumber asc) | `PageResponse<RecipeStepResponse{id, recipeId, stepNumber, instruction, imageIds, imageUrls}>` | Global default | List all steps |
| GET | `/{recipeStepId}` | path | `RecipeStepResponse` | Global default | Get step |
| GET | `/recipe/{recipeId}` | path `recipeId`; query `search?`, `Pageable` | `PageResponse<RecipeStepResponse>` | Global default | List steps for a recipe |
| POST | `` | `RecipeStepRequest{recipeId, stepNumber, instruction, imageIds}` | `RecipeStepResponse` | Global default | Create step |
| PUT | `/{recipeStepId}` | `RecipeStepRequest` | `RecipeStepResponse` | Global default | Update step |
| DELETE | `/{recipeStepId}` | — | `String` `"Deleted"` | Global default | Delete step |

## 20. Recipe — `recipe/controller/ToolController.java`
Base path: `/api/v1/tools`

| Method | Path | Request/Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | query `search?`, `Pageable` (default sort name asc) | `PageResponse<ToolResponse{id, recipeId, name}>` | Global default | List all tools |
| GET | `/{toolId}` | path | `ToolResponse` | Global default | Get tool |
| GET | `/recipe/{recipeId}` | path `recipeId`; query `search?`, `Pageable` | `PageResponse<ToolResponse>` | Global default | List tools for a recipe |
| POST | `` | `ToolRequest{recipeId, name}` | `ToolResponse` | Global default | Add tool to recipe |
| PUT | `/{toolId}` | `ToolRequest` | `ToolResponse` | Global default | Update tool |
| DELETE | `/{toolId}` | — | `String` `"Deleted"` | Global default | Delete tool |

## 21. Recipe — `recipe/controller/RecipeCommentController.java`
Base path: `/api/v1/recipe-comments`

| Method | Path | Request/Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | query `search?`, `Pageable` (default sort createdAt desc) | `PageResponse<RecipeCommentResponse{id, recipeId, userId, content, parentId, createdAt, updatedAt}>` | Global default | List all comments |
| GET | `/{commentId}` | path | `RecipeCommentResponse` | Global default | Get comment |
| GET | `/recipe/{recipeId}` | path `recipeId`; query `search?`, `Pageable` | `PageResponse<RecipeCommentResponse>` | Global default | Comments for a recipe |
| GET | `/user/{userId}` | path `userId`; query `search?`, `Pageable` | `PageResponse<RecipeCommentResponse>` | Global default | Comments by a user |
| GET | `/reply/{parentId}` | path `parentId`; query `search?`, `Pageable` | `PageResponse<RecipeCommentResponse>` | Global default | Replies to a comment |
| POST | `` | `RecipeCommentRequest{recipeId, content, parentId?}` | `RecipeCommentResponse` | Global default | Create comment/reply |
| PUT | `/{commentId}` | `RecipeCommentRequest` | `RecipeCommentResponse` | Global default | Update comment |
| DELETE | `/{commentId}` | — | `String` `"Deleted"` | Global default | Delete comment |

## 22. Recipe — `recipe/controller/RecipeRatingController.java`
Base path: `/api/v1/recipe-ratings`

| Method | Path | Request/Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | query `search?`, `Pageable` (default sort createdAt desc) | `PageResponse<RecipeRatingResponse{id, recipeId, customerId, score, createdAt}>` | Global default | List all ratings |
| GET | `/{ratingId}` | path | `RecipeRatingResponse` | Global default | Get rating |
| GET | `/user/{customerId}` | path `customerId`; query `search?`, `Pageable` | `PageResponse<RecipeRatingResponse>` | Global default | Ratings by a customer |
| GET | `/recipe/{recipeId}` | path `recipeId`; query `search?`, `Pageable` | `PageResponse<RecipeRatingResponse>` | Global default | Ratings for a recipe |
| POST | `` | `RecipeRatingRequest{recipeId, score(e.g. 1-5)}` | `RecipeRatingResponse` | Global default | Create rating |
| PUT | `/{ratingId}` | `RecipeRatingRequest` | `RecipeRatingResponse` | Global default | Update rating |
| DELETE | `/{ratingId}` | — | `String` `"Deleted"` | Global default | Delete rating |

## 23. Recipe — `recipe/controller/RecipeBookmarkController.java`
Base path: `/api/v1/recipe-bookmarks`

| Method | Path | Request/Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | `Pageable` (default sort createdAt desc) | `PageResponse<RecipeBookmarkResponse{id, recipeId, customerId, createdAt}>` | Global default | List all bookmarks |
| GET | `/{recipeBookmarkId}` | path | `RecipeBookmarkResponse` | Global default | Get bookmark |
| GET | `/user/{customerId}` | path `customerId`; `Pageable` | `PageResponse<RecipeBookmarkResponse>` | Global default | Bookmarks by customer |
| GET | `/recipe/{recipeId}` | path `recipeId`; `Pageable` | `PageResponse<RecipeBookmarkResponse>` | Global default | Bookmarks for a recipe |
| POST | `` | `RecipeBookmarkRequest{recipeId}` | `RecipeBookmarkResponse` | Global default | Bookmark a recipe |
| PUT | `/{recipeBookmarkId}` | `RecipeBookmarkRequest{recipeId}` | `RecipeBookmarkResponse` | Global default | Update bookmark |
| DELETE | `/{recipeBookmarkId}` | — | `String` `"Deleted"` | Global default | Remove bookmark |

## 24. Recipe — `recipe/controller/MediaController.java`
Base path: `/api/v1/media`

| Method | Path | Request Body (multipart) | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `/images` | form field `file` (MultipartFile) | `MediaResponse{id, url, type(IMAGE\|VIDEO), status(TEMP\|ATTACHED)}` | Global default | Upload image, creates media record in TEMP status |
| POST | `/videos` | form field `file` (MultipartFile) | `MediaResponse` | Global default | Upload video |

## 25. Order — `order/controller/CustomerOrderController.java`
Base path: `/api/v1/orders`
Auth note: class-level `@PreAuthorize("hasRole('CUSTOMER')")` — this path is **not** covered by any `SecurityConfig` URL matcher, so it relies entirely on method security.

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `CreateOrderRequest{paymentMethod(COD\|PAYOS), note?, receiverName, receiverPhone, provinceCode, districtCode, wardCode, addressDetail, fullAddress, shippingRateId, items: OrderItemRequest[]{productUnitId, quantity(1-1000)}}` | `OrderResponse{id, code, customerId, createdBy, status(AWAITING_PAYMENT\|PENDING\|CONFIRMED\|READY_TO_SHIP\|SHIPPING\|DELIVERED\|CANCELLED), paymentMethod(COD\|PAYOS), productsPrice, shippingFee, discountPercent, discountAmount, total, revenue, note, receiverName, receiverPhone, provinceCode, districtCode, wardCode, addressDetail, fullAddress, cancelReason, carrierName, shippingService, cancelledAt, deliveredAt, receivedConfirmedAt, createdAt, items: OrderItemResponse[]{id, productId, productUnitId, productName, sku, unitName, quantity, unitPrice, lineTotal}}` | `hasRole('CUSTOMER')` (method-level) | Place order from checkout; snapshots the chosen shipping rate (`shippingRateId`) |
| GET | `/me` | query `status?`, `page=0`, `size=20` | `Page<OrderSummaryResponse{id, code, customerId, status, paymentMethod, total, receiverName, createdAt}>` (sorted createdAt desc) | same | List own orders |
| GET | `/{id}` | path `id` | `OrderResponse` | same | Get own order detail |
| GET | `/{id}/tracking` | path `id` | `OrderTrackingResponse{id, code, status, paymentMethod, createdAt, updatedAt, deliveredAt, cancelledAt}` | same | Lightweight tracking view |
| PATCH | `/{id}/cancel` | path `id`; `CancelOrderRequest{reason?}` (optional body) | `OrderResponse` | same | Cancel own order (only valid from `AWAITING_PAYMENT`/`PENDING`/`CONFIRMED`) |
| PATCH | `/{id}/confirm-received` | path `id` | `OrderResponse` | same | Customer confirms delivery received |

## 26. Order — `order/controller/StaffOrderController.java`
Base path: `/api/v1/staff/orders`
Auth note: class-level `@PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")` — also not covered by a `SecurityConfig` URL matcher.

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `StaffCreateOrderRequest{customerId, discountPercent?(0-100), order: CreateOrderRequest}` | `OrderResponse` | `hasAnyRole('MANAGER','STAFF')` | Staff-created order on behalf of a customer (e.g. phone/walk-in order), with optional manual discount |
| GET | `` | query `status?`, `paymentMethod?(COD\|PAYOS)`, `page=0`, `size=20` | `Page<OrderSummaryResponse>` (sorted createdAt desc) | same | List all orders |
| GET | `/revenue` | query `type(DAY\|WEEK\|MONTH)`, `from?(yyyy-MM-dd)`, `to?(yyyy-MM-dd)` (defaults to last 30 days ending today) | `RevenueReportResponse{type, from, to, totalRevenue, totalOrders, points: RevenuePoint[]{period, revenue, orderCount}}` | same | Revenue report, bucketed by day/week/month |
| GET | `/{id}` | path `id` | `OrderResponse` | same | Get any order detail |
| PATCH | `/{id}/status` | `UpdateOrderStatusRequest{status(AWAITING_PAYMENT\|PENDING\|CONFIRMED\|READY_TO_SHIP\|SHIPPING\|DELIVERED\|CANCELLED)}` | `OrderResponse` | same | Advance order status (server enforces the state-machine transitions in `OrderStatus`) |
| PATCH | `/{id}/cancel` | path `id`; `CancelOrderRequest{reason?}` | `OrderResponse` | same | Cancel order (only valid from `PENDING`/`CONFIRMED`/`READY_TO_SHIP`) |

## 27. Payment — `payment/controller/PaymentController.java`
Base path: `/api/v1/payments`
Auth note: **no `@PreAuthorize` at all**, class or method level — falls straight through to the `anyRequest().authenticated()` global default, so any authenticated role (including another customer) can request a PayOS link for an arbitrary `orderId` unless `PaymentService.createPayosLink` does its own ownership check internally.

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `/orders/{orderId}/payos-link` | path `orderId` | `PayosLinkResponse{paymentId, checkoutUrl}` | Global default (authenticated, no role restriction) | Create a PayOS checkout link for paying an order (distinct from the wallet-level `payment/WalletController`/`TopUpController`/`PayoutController` already documented in sections 10-13) |

## 28. Warehouse — `warehouse/controller/SupplierController.java`
Base path: `/api/suppliers` (note: **no `/v1` prefix**, and no `/api/v1/suppliers` alias either — inconsistent with the rest of the API, same pattern as `ProductController`/`CategoryController` in section 14-15)
Auth note: no `@PreAuthorize` anywhere in the class — Global default (authenticated, any role).

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `SupplierRequest{supplierName, phoneNumber?, email?, address?}` | `SupplierResponse{supplierId, supplierName, phoneNumber, email, address, createdAt}` (201) | Global default | Create supplier |
| PUT | `/{id}` | `SupplierRequest` | `SupplierResponse` | Global default | Update supplier |
| GET | `/{id}` | path `id` | `SupplierResponse` | Global default | Get supplier |
| GET | `` | query `keyword?`, `Pageable` (default `size=20, sort=supplierName ASC`) | `Page<SupplierResponse>` | Global default | List/search suppliers |
| DELETE | `/{id}` | — | `Void` | Global default | Delete supplier |

## 29. Warehouse — `warehouse/controller/InventoryController.java`
Base path: `/api/warehouse/inventory` (note: **no `/v1` prefix**)
Auth note: no `@PreAuthorize` — Global default (authenticated, any role).

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `/lots` | query `productId` | `List<StockLot>` — **raw JPA entity** (`lotId, productId, stockInNoteId, lotCode, initialQuantity, currentQuantity, unitCost, expiryDate, status(STORAGE\|EXPIRED\|CANCELED\|EMPTY), createdAt`), not a DTO | Global default | List stock lots for a product, ordered by expiry then created date |
| GET | `/low-stock` | — | `List<ProductResponse>` (same shape as section 15's `ProductResponse`) | Global default | List products currently below `minStock` |
| GET | `/expiring-lots` | query `withinDays=30` (0-3650) | `List<StockLot>` — raw entity | Global default | Lots with status `STORAGE` expiring within N days |

## 30. Warehouse — `warehouse/controller/StockInController.java`
Base path: `/api/warehouse/stock-in` (note: **no `/v1` prefix**)
Auth note: no `@PreAuthorize` — Global default (authenticated, any role).

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `StockInCreateRequest{supplierId, receiptDate?, paidAmount?(≥0, 2dp), note?, items: StockInItemRequest[]{productId, unitId, quantity(≥0.01, 2dp), unitPrice(≥0, 2dp), expiryDate?}}` | `StockInNote` — **raw JPA entity** (`noteId, code, supplierId, receiptDate, totalAmount, paidAmount, debtAmount, paymentStatus(UNPAID\|PARTIAL\|PAID), note, status(PENDING\|COMPLETED\|CANCELLED), updatedAt, createdAt`) (201) | Global default | Create stock-in note (status `PENDING`) |
| POST | `/{id}/complete` | path `id` | `StockInNote` (raw) | Global default | Complete stock-in note; generates `StockLot` rows |
| POST | `/{id}/cancel` | path `id` | `StockInNote` (raw) | Global default | Cancel stock-in note |
| GET | `` | query `status?(PENDING\|COMPLETED\|CANCELLED)`, `Pageable` (default `size=20, sort=createdAt DESC`) | `Page<StockInNote>` (raw) | Global default | List stock-in notes |
| GET | `/{id}` | path `id` | `StockInDetailResponse{note: StockInNote, items: List<StockInItem>}` — both nested types are **raw JPA entities** | Global default | Get stock-in note with its line items |

## 31. Warehouse — `warehouse/controller/StockOutController.java`
Base path: `/api/warehouse/stock-out` (note: **no `/v1` prefix**)
Auth note: no `@PreAuthorize` — Global default (authenticated, any role).

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `StockOutCreateRequest{type(SALE\|DISPOSAL), referenceOrderId?, note?, items: StockOutItemRequest[]{productId, unitId, lotId?, quantity(≥0.01, 2dp)}}` | `StockOutNote` — **raw entity** (`noteId, code, type, referenceOrderId, totalCost, note, status(PENDING\|COMPLETED\|CANCELLED), updatedAt, createdAt`) (201) | Global default | Create stock-out note (sale or disposal) |
| POST | `/{id}/complete` | path `id` | `StockOutNote` (raw) | Global default | Complete stock-out note; deducts lot quantities |
| POST | `/{id}/cancel` | path `id` | `StockOutNote` (raw) | Global default | Cancel stock-out note |
| GET | `` | query `status?`, `type?(SALE\|DISPOSAL)`, `Pageable` (default `size=20, sort=createdAt DESC`) | `Page<StockOutNote>` (raw) | Global default | List stock-out notes |
| GET | `/{id}` | path `id` | `StockOutDetailResponse{note: StockOutNote, items: List<StockOutItem>}` — raw entities | Global default | Get stock-out note with line items |

## 32. Warehouse — `warehouse/controller/StocktakeController.java`
Base path: `/api/warehouse/stocktakes` (note: **no `/v1` prefix**)
Auth note: no `@PreAuthorize` — Global default (authenticated, any role).

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `StocktakeCreateRequest{productIds: UUID[], handleBy?}` | `StocktakeDetailResponse{note: StocktakeNote(raw entity: noteId, code, stocktakeDate, handleBy, createdBy, status(DRAFT\|COMPLETED\|CANCELLED), updatedAt, createdAt), products: ProductLots[]{productId, lots: List<StocktakeItem>(raw entity)}}` (201) | Global default | Start a stocktake (DRAFT) snapshotting current lots for given products |
| PUT | `/{noteId}/counts` | `StocktakeCountRequest{products: [{productId, lots: [{itemId, actualQuantity(≥0, 2dp), reason?, note?}]}]}` | `StocktakeDetailResponse` | Global default | Record counted quantities per lot |
| POST | `/{noteId}/complete` | path `noteId` | `StocktakeDetailResponse` | Global default | Complete stocktake; applies quantity adjustments |
| POST | `/{noteId}/cancel` | path `noteId` | `StocktakeNote` (raw entity) | Global default | Cancel stocktake |
| GET | `` | query `status?(DRAFT\|COMPLETED\|CANCELLED)`, `Pageable` (default `size=20, sort=createdAt DESC`) | `Page<StocktakeNote>` (raw) | Global default | List stocktakes |
| GET | `/{noteId}` | path `noteId` | `StocktakeDetailResponse` | Global default | Get stocktake detail |

## 33. Receipt — `receipt/controller/ReceiptCategoryController.java`
Base path: `/api/v1/receipt-categories`

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `ReceiptCategoryRequest{name, type(SALE\|SETTLEMENT\|OTHER)}` | `ReceiptCategoryResponse{id, name, type, createdAt}` | `hasRole('MANAGER')` | Create receipt category |
| PUT | `/{id}` | `ReceiptCategoryRequest` | `ReceiptCategoryResponse` | `hasRole('MANAGER')` | Update category |
| DELETE | `/{id}` | — | `Void` | `hasRole('MANAGER')` | Delete category |
| GET | `/{id}` | path `id` | `ReceiptCategoryResponse` | `hasAnyRole('MANAGER','STAFF')` | Get category |
| GET | `` | — | `List<ReceiptCategoryResponse>` (no paging) | `hasAnyRole('MANAGER','STAFF')` | List all categories |

## 34. Receipt — `receipt/controller/ReceiptVoucherController.java`
Base path: `/api/v1/receipt-vouchers`

| Method | Path | Request Body / Query | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `CreateReceiptVoucherRequest{receiptCategoryId, amount(≥0.01), note?}` | `ReceiptVoucherResponse{id, code, receiptCategoryId, receiptCategoryName, receiptCategoryType(SALE\|SETTLEMENT\|OTHER), paymentId, amount, note, createdBy, createdAt}` | `hasAnyRole('MANAGER','STAFF')` | Create receipt (money-in) voucher |
| GET | `/{id}` | path `id` | `ReceiptVoucherResponse` | `hasAnyRole('MANAGER','STAFF')` | Get voucher |
| GET | `` | query `categoryId?`, `page=0`†, `size=10` | `Page<ReceiptVoucherResponse>` (sorted createdAt desc) | `hasAnyRole('MANAGER','STAFF')` | List vouchers, paged — †see anomaly 13, the source's `page` default literal has a leading space |

## 35. Shipment — `shipment/controller/ShippingController.java`
Base path: `/api/v1/shipping`
Auth note: class-level `@PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'STAFF')")` — note `BAKER_STAFF` and `ADMIN` are excluded.

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `/cities` | — | `List<AddressOptionResponse{code, name}>` | `hasAnyRole('CUSTOMER','MANAGER','STAFF')` | List Goship cities for address pickers |
| GET | `/cities/{cityId}/districts` | path `cityId` | `List<AddressOptionResponse>` | same | List districts in a city |
| GET | `/districts/{districtId}/wards` | path `districtId` | `List<AddressOptionResponse>` | same | List wards in a district |
| POST | `/rates` | `ShippingRatesRequest{paymentMethod(COD\|PAYOS), provinceCode, districtCode, wardCode, items: OrderItemRequest[]{productUnitId, quantity(1-1000)}}` | `List<ShippingRateResponse{id, carrierName, carrierLogo, service, expected, codFee, totalFee, shippingFee}>` | same | Preview shipping rate quotes for a prospective order before checkout |

## 36. Shipment — `shipment/controller/ShipmentWebhookController.java`
Base path: `/api/v1/webhooks/goship`
Auth note: **no `@PreAuthorize` anywhere, and this path is absent from the `SecurityConfig` `permitAll()` matcher list** (only `/api/v1/payos/webhook` is listed there) — see anomaly 9.

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| POST | `` | `ShipmentWebhookRequest{externalId, statusCode}` | `ApiResponse<Void>` (wrapped, unlike `PayOSWebhookController`'s raw `"OK"` string) | Global default (`anyRequest().authenticated()`) — **likely unreachable by Goship's actual server-to-server callback**, which can't supply a Bearer JWT | Goship shipment-status webhook callback |

## 37. Shipment — `shipment/controller/StaffShipmentController.java`
Base path: `/api/v1/staff/orders/{orderId}/shipment`
Auth note: class-level `@PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")`.

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | path `orderId` | `ShipmentResponse{id, orderId, provider, carrier, trackingCode, status(PENDING\|PICKING\|IN_TRANSIT\|DELIVERED\|FAILED\|RETURNED\|CANCELLED), shippingFee, codAmount, weightGram, shippedAt, deliveredAt, createdAt}` | `hasAnyRole('MANAGER','STAFF')` | Get the shipment record tied to an order |

## 38. Customer — `customer/controller/CustomerAddressController.java`
Base path: `/api/v1/customers/me/addresses`
Auth note: class-level `@PreAuthorize("hasRole('CUSTOMER')")`, redundant with (but consistent with) `SecurityConfig`'s `/api/v1/customers/me/**` → `hasRole('CUSTOMER')` matcher.

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | — | `List<CustomerAddressResponse{id, receiverName, receiverPhone, provinceCode, districtCode, wardCode, addressDetail, fullAddress, defaultAddress, createdAt}>` (no paging) | `hasRole('CUSTOMER')` | List own saved addresses |
| POST | `` | `CustomerAddressRequest{receiverName, receiverPhone(pattern ^(0\|+84)\d{9,10}$), provinceCode, districtCode, wardCode, addressDetail, fullAddress, defaultAddress?}` | `CustomerAddressResponse` | same | Add address |
| PUT | `/{id}` | `CustomerAddressRequest` | `CustomerAddressResponse` | same | Update address |
| DELETE | `/{id}` | — | `Void` | same | Delete address |
| PATCH | `/{id}/default` | path `id` | `CustomerAddressResponse` | same | Set this address as the default |

## 39. Customer — `customer/controller/StaffCustomerAddressController.java`
Base path: `/api/v1/staff/customers/{customerId}/addresses`
Auth note: class-level `@PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")`.

| Method | Path | Request Body | Response Body | Auth | Purpose |
|---|---|---|---|---|---|
| GET | `` | path `customerId` | `List<CustomerAddressResponse>` | `hasAnyRole('MANAGER','STAFF')` | Staff view of a given customer's saved addresses (read-only; no create/update/delete exposed to staff) |

---

## Notes / anomalies to flag for frontend planning

1. **Inconsistent prefixes**: `product/controller/CategoryController` uses `/api/product-categories` and `product/controller/ProductController` uses `/api/products` — both missing the `/v1` segment that every other module uses. Confirm with backend before wiring these up, in case it's a bug that gets fixed.
2. **PayoutController returns raw `PayoutResponse`**, not wrapped in `ApiResponse<T>` — different shape from almost everything else in the API. Same for `ProductController.comboStock` (`BigDecimal` raw) and `setComboItems` (echoes the request type, not a dedicated response DTO).
3. **PayoutController approve/reject have `@PreAuthorize("hasRole('ADMIN')")` commented out** with a `// TODO` — currently reachable by any authenticated user. Worth confirming before building the admin payout UI around it, since behavior may change.
4. **PayOSWebhookController** (`/api/v1/payos/webhook`) is not in the public-endpoint allowlist in `SecurityConfig`, meaning the global `anyRequest().authenticated()` rule would apply and PayOS's own server call would be rejected without a Bearer token — likely a bug, not something for FE to call directly regardless (it's a payment-gateway-to-backend callback).
5. **`/api/v1/notifications/**`** is `permitAll()` at the Spring Security filter-chain level, but every controller method is separately annotated `@PreAuthorize("isAuthenticated()")`, so in practice the frontend still needs a valid Bearer token to get anything but a 401/403 from the method-security layer.
6. **Pagination**: most `recipe/*` list endpoints use a custom `PageResponse<T>{content, totalElements, totalPages, currentPage}`. Other modules (expense, notification, product) use Spring's native `Page<T>` (has more fields: `content, totalElements, totalPages, number, size, ...`). Frontend typing should distinguish these two page-wrapper shapes per endpoint.
7. Delete endpoints across the `recipe/*` module return a bare `String` `"Deleted"` wrapped in `ApiResponse<String>`, not `Void`.
8. **`PayOSWebhookController`'s gap appears fixed**: the current `SecurityConfig` *does* list `.requestMatchers("/api/v1/payos/webhook").permitAll()`, which contradicts anomaly 4 above (written when it was missing). Treat anomaly 4 as resolved/stale and confirm with backend before relying on either version — this also means anomaly 9 below (the Goship webhook) is the live instance of that same class of bug, not PayOS.
9. **`ShipmentWebhookController` (`POST /api/v1/webhooks/goship`) is a PayOS-style external gap, currently unfixed**: it has no `@PreAuthorize` and is **not** in `SecurityConfig`'s `permitAll()` matcher list, so it falls under `anyRequest().authenticated()`. Goship's own server-to-server callback almost certainly cannot supply a Bearer JWT, meaning this webhook is likely unreachable in production exactly like the old `PayOSWebhookController` bug — flag for backend before building anything that depends on shipment-status push updates.
10. **The entire `warehouse/*` module (`SupplierController`, `InventoryController`, `StockInController`, `StockOutController`, `StocktakeController`) has zero `@PreAuthorize` annotations**, class- or method-level, and none of their base paths (`/api/suppliers`, `/api/warehouse/inventory`, `/api/warehouse/stock-in`, `/api/warehouse/stock-out`, `/api/warehouse/stocktakes`) appear in `SecurityConfig`'s matcher list. Every endpoint there is reachable by **any authenticated role**, including `CUSTOMER` — unlike the analogous `expense/*` and `receipt/*` modules which restrict to `MANAGER`/`STAFF`. Confirm whether this is intentional before exposing warehouse screens to non-staff roles.
11. **Inconsistent prefixes, part 2**: `SupplierController` (`/api/suppliers`) and all four warehouse note/inventory controllers (`/api/warehouse/**`) omit the `/v1` segment, joining `ProductController`/`CategoryController` (anomaly 1) in that inconsistency.
12. **Warehouse responses leak raw JPA entities** instead of DTOs: `InventoryController` returns `List<StockLot>` directly; `StockInController`/`StockOutController`/`StocktakeController` return `StockInNote`/`StockOutNote`/`StocktakeNote` (and their nested `StockInItem`/`StockOutItem`/`StocktakeItem`) directly, wrapped only in `ApiResponse<T>`. These are Hibernate `@Entity` classes with JPA annotations and lazy-loaded fields — serializing them risks leaking internal column names/DB enum types and `LazyInitializationException`s depending on fetch strategy. Frontend should treat their field shapes as unstable/implementation-detail compared to the hand-written DTOs used everywhere else in this doc.
13. **`ReceiptVoucherController.getAll` has a malformed default**: `@RequestParam(defaultValue = " 0") int page` — the default literal is the string `" 0"` (leading space), not `"0"`. Spring's `Integer` conversion via `Integer.valueOf`/`parseInt` rejects leading whitespace, so a request that omits the `page` query parameter entirely will likely throw a 400/500 instead of defaulting to page 0. Frontend should always pass `page` explicitly on this endpoint as a workaround until fixed.
14. **No blanket `/api/v1/staff/**` rule exists in `SecurityConfig`.** Staff-role protection for `StaffOrderController` (`/api/v1/staff/orders`), `StaffShipmentController` (`/api/v1/staff/orders/{orderId}/shipment`), and `StaffCustomerAddressController` (`/api/v1/staff/customers/{customerId}/addresses`) relies entirely on each controller's own class-level `@PreAuthorize("hasAnyRole('MANAGER','STAFF')")`, not a URL-pattern matcher. Any future endpoint added under `/api/v1/staff/**` without its own `@PreAuthorize` would silently fall through to `anyRequest().authenticated()` — reachable by any authenticated role, not just staff/manager.
15. **`PaymentController` (`POST /api/v1/payments/orders/{orderId}/payos-link`) has no `@PreAuthorize` at all** (class or method level) and its base path isn't in the `SecurityConfig` matcher list either, so it's reachable by any authenticated user regardless of role; whether the caller actually owns `orderId` is enforced (if at all) only inside `PaymentService.createPayosLink`, not at the security layer. Contrast with `CustomerOrderController`/`StaffOrderController`, which both gate by role at the controller level.
16. **Pagination shape split, part 2**: the new `order/*` controllers (`CustomerOrderController`, `StaffOrderController`) build `PageRequest` manually from raw `page`/`size` int query params (not a bound `Pageable`) and return Spring's native `Page<T>` — consistent with the `Page<T>` family noted in existing anomaly 6, as do `warehouse/*` and `receipt/receipt-vouchers`. None of the newly documented controllers use the `recipe/*` module's custom `PageResponse<T>` shape.
