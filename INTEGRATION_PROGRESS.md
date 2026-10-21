# Frontend–Backend Integration: Progress Report

_Scope: `FA26SE275_Frontend` (web app) wired to the Spring Boot backend at `BE-AI-driven-recipe-and-ingredient-marketplace-for-bakers`. Last updated 2026-10-17._

## 1. Executive Summary

**Correction to the 2026-10-15 version of this report**: that version undercounted the backend (claimed 24 controllers; actual count is 35) and wrongly marked Order, order-level Payment, Supplier, and Inventory as "backend doesn't exist at all." A verification pass against the live BE source on 2026-10-17 found all four modules fully implemented server-side (plus Receipt Voucher, Shipping, and Customer Address — modules the original report didn't mention). The previously claimed `walletService`/`topUpService`/`payoutService` backend controllers do not exist in the BE at all — that part of the original report had it backwards.

On 2026-10-17, FE service/type layers were built for every one of these newly-confirmed modules: **Order** (customer + staff), **Payment** (PayOS link creation), **Supplier**, **Inventory** (StockIn/StockOut/Stocktake), **Receipt Voucher** (+ categories), **Shipping** (rates/address lookup), and **Customer Address** (self-service + staff read-only). These are service/type files only — **no UI page has been rewired to use them yet**; the mock pages (`OrderManagementPage.tsx`, `PaymentManagement.tsx`, `CashFlowPage.tsx`, `SupplierManagementPage.tsx`, `InventoryPage.tsx`, `DamagedGoodsPage.tsx`, and their staff equivalents) still run on inline `MOCK_*` arrays or the old mock type files (`types/order.ts`, `types/payment.ts`, `types/inventory.ts`, `types/supplier.ts`), which remain untouched and are still imported by those pages. `tsc -b` and `eslint` are clean on every new file. No backend was reachable in this environment, so none of this has been verified against live HTTP responses.

Genuinely still absent from the backend (confirmed against the live controller inventory, not just the old report's guess): a dedicated **Reports** controller, **AI-moderation-log** controller, and **Store-management** controller. The **admin-accounts gap is also confirmed real**: `AdminAccountController` only accepts/lists `STAFF|MANAGER|BAKER_STAFF`; no endpoint lists `ADMIN` or `CUSTOMER` accounts.

Also still true from the prior session: three recipe sub-resource services (`ingredientService`, `toolService`, `recipeStepService`) are built but unused — the recipe editor manages ingredients/steps/tools as embedded fields on the recipe payload, not via standalone CRUD. Several other built services still have no consuming page: `cartService`, `hobbyService`/`adminHobbyService`, `customerProfileService` — plus, as of this session, every one of the 14 newly-built Order/Payment/Inventory/Supplier/Receipt/Shipping/Address service files (listed in §5).

**Cross-reference**: `API_REFERENCE.md` now documents all 15 of these newly-confirmed controllers in full (sections 25-39, bringing the doc's total to 39 documented controllers) plus 9 new anomaly bullets (8-16). Two are worth calling out here because they directly affect the services this session built but hasn't exercised against a live server yet:
- **`receiptVoucherService.ts`'s `list()` should always pass `page` explicitly** — the backend's `ReceiptVoucherController.getAll` has a malformed `@RequestParam(defaultValue = " 0")` (leading space), which will likely reject an omitted `page` param with a 400/500 instead of defaulting to 0 (API_REFERENCE.md anomaly 13).
- **The entire `warehouse/*` module has no role gating at all** (no `@PreAuthorize`, no matching `SecurityConfig` matcher) — any authenticated role, including a hypothetical CUSTOMER session, can call `inventoryService`/`stockInService`/`stockOutService`/`stocktakeService`/`supplierApiService` endpoints. Worth confirming with backend before this UI ships to non-staff builds (API_REFERENCE.md anomaly 10).
- A **`ShipmentWebhookController` (`/api/v1/webhooks/goship`) gap** mirrors the old PayOS webhook bug: no `@PreAuthorize` and not in the `permitAll()` matcher list, so Goship's server-to-server status callback likely can't authenticate. Not consumed by any service built this session, but relevant once shipment-status push updates are wired into `staffShipmentService`/`OrderManagementPage.tsx` (API_REFERENCE.md anomaly 9).

## 2. Integration Status by Functional Group

| Group | Status | Notes |
|---|---|---|
| **Auth** | ✅ Integrated | login, register, verify-email, forgot/reset password, refresh-on-401 interceptor, forced-logout event all real. `loginWithGoogle`/`completeOnboarding` stay mocked — no backend endpoint. |
| **Profile (user)** | ✅ Integrated | `GET/PUT /users/me`, avatar upload/remove — via `AuthContext.updateProfile/uploadAvatar/removeAvatar`, consumed by `EditProfile.tsx`. |
| **Profile (customer)** | ⬜ N/A | `customerProfileService` built, zero consumers — this web app has no `CUSTOMER` role in its UI. |
| **Recipes (public)** | ✅ Integrated | `Recipes.tsx`, `RecipeDetails.tsx` via `recipeApiService`/`recipeCommentService`/`recipeRatingService`/`recipeCategoryService`. |
| **Recipe authoring (bakerstaff)** | ✅ Integrated | `MyRecipesPage.tsx`, `RecipeEditorPage.tsx`, `RecipeDetailPage.tsx`, `CommentManagement.tsx`, `RatingOverview.tsx`, `AIVideoGeneration.tsx`, `BakerstaffDashboard.tsx`. Ingredients/steps/tools go through the recipe's own create/update payload, not the dedicated `ingredientService`/`toolService`/`recipeStepService` (those three are built, unused). |
| **Hobbies** | ⬜ N/A | `hobbyService`/`adminHobbyService` built, zero consumers — no hobby-management page exists anywhere in the app. |
| **Cart** | ⬜ N/A | `cartService` built, zero consumers — no cart/checkout page exists. |
| **Product/catalog** | ✅ Integrated | `ProductManagementPage.tsx` via `productApiService`/`productCategoryService`/`mediaService`. Cost price and manual stock-qty inputs kept local-only (no backend field for either). |
| **Expense vouchers** | ✅ Integrated | `ExpenseVoucherPage.tsx` via `expenseVoucherService`/`expenseCategoryService`. Draft/approve workflow dropped (backend has only ACTIVE/CANCELLED). |
| **Wallet/top-up/payout** | ❌ Does not exist | Backend has no Wallet/TopUp/Payout controller at all — the prior report's claim that these were built-but-unused was wrong; nothing to build against. |
| **Orders** | 🟡 Service layer built, UI not wired | `orderService.ts` (customer `/api/v1/orders`) + `staffOrderService.ts` (staff `/api/v1/staff/orders`, incl. `revenueReport`) built 2026-10-17. `OrderManagementPage.tsx` + staff equivalent still on `MOCK_*` data. |
| **Order payments** | 🟡 Service layer built, UI not wired | `paymentService.ts` (`POST /api/v1/payments/orders/{orderId}/payos-link`) built 2026-10-17. `PaymentManagement.tsx` still mocked. Note: BE exposes no payment *listing/history* endpoint, only PayOS link creation — a payment-history UI would need an Order-side payment status instead. |
| **Cash flow (receipt vouchers)** | 🟡 Service layer built, UI not wired | `receiptVoucherService.ts` + `receiptCategoryService.ts` (`/api/v1/receipt-vouchers`, `/api/v1/receipt-categories`) built 2026-10-17 — the income-side mirror of expense vouchers. `CashFlowPage.tsx` still mocked; a real cash-flow view would combine this with `expenseVoucherService`. |
| **Suppliers** | 🟡 Service layer built, UI not wired | `supplierApiService.ts` (`/api/suppliers`) built 2026-10-17. `SupplierManagementPage.tsx` + staff equivalent still mocked. Real `ApiSupplier` has no `isActive`/`contactName` fields, unlike the mock type. |
| **Inventory (stock in/out/stocktake)** | 🟡 Service layer built, UI not wired | `inventoryService.ts`, `stockInService.ts`, `stockOutService.ts`, `stocktakeService.ts` (`/api/warehouse/*`) built 2026-10-17. `InventoryPage.tsx`, `DamagedGoodsPage.tsx` + staff equivalents still mocked. None of these 5 warehouse controllers have `@PreAuthorize` — no role gating server-side. |
| **Shipping / Customer address** | 🟡 Service layer built, no page at all | `shippingService.ts`, `staffShipmentService.ts`, `customerAddressApiService.ts`, `staffCustomerAddressService.ts` built 2026-10-17. No FE page references shipping rates or customer addresses yet (this app has no CUSTOMER-role UI) — built ahead of a page because they're needed once `OrderManagementPage.tsx`/checkout work starts (order creation requires a `shippingRateId`). |
| **Admin accounts (staff)** | ✅ Integrated | `StaffManagementPage.tsx` via `adminAccountService` (STAFF/MANAGER/BAKER_STAFF only). |
| **Admin accounts (all roles)** | 🚫 Blocked | `UserManagementPage.tsx` left mocked — backend's admin-accounts endpoint excludes ADMIN and CUSTOMER entirely; no endpoint lists either. Confirmed still true (`AdminAccountServiceImpl.MANAGED_ROLES`). |
| **Notifications** | ✅ Integrated | `NotificationButton.tsx`, `dashboard/Notifications.tsx` via `notificationService`, including working mark-read/mark-all-read (previously inert). |
| **Reports** | 🚫 Blocked | `ReportsManagement.tsx` still uses `reportService`/`mockAdminDashboard` — no dedicated Reports controller exists. (BE does have `RevenueReportResponse` via `staffOrderService.revenueReport` and expense/receipt-voucher report endpoints — a reports page could be assembled from those, just not from one unified endpoint.) |
| **AI Moderation logs / Store management** | 🚫 Blocked | Confirmed no moderation-log or store-management controller anywhere in the backend. |

## 3. Files Created and Modified, Grouped by Area

### 2026-10-15 session (unchanged, kept for history)
**New service files (21):** `pageParams.ts`, `customerProfileService.ts`, `hobbyService.ts`, `adminAccountService.ts`, `cartService.ts`, `expenseCategoryService.ts`, `expenseVoucherService.ts`, `notificationService.ts`, `walletService.ts`, `topUpService.ts`, `payoutService.ts`, `productCategoryService.ts`, `productApiService.ts`, `recipeApiService.ts`, `recipeCategoryService.ts`, `ingredientService.ts`, `recipeStepService.ts`, `toolService.ts`, `recipeCommentService.ts`, `recipeRatingService.ts`, `recipeBookmarkService.ts`, `mediaService.ts`

**Extended existing services:** `api.ts` (response interceptor, `withCredentials`), `authService.ts` (password-reset flow, refresh/logout, `clearLocalSession`/`updateStoredUser`), `userService.ts` (`updateMe`/`uploadAvatar`/`removeAvatar`)

**New type files (10):** `cart.ts`, `hobby.ts`, `customerProfile.ts`, `adminAccount.ts`, `expenseApi.ts`, `notificationApi.ts`, `wallet.ts`, `media.ts`, `catalogApi.ts`, `recipeApi.ts` — plus `common.ts` extended (`ApiEnvelope`, `ApiPageResponse`, `SpringPage`, `PageParams`) and `auth.ts` extended (`UpdateProfilePayload`, context methods)

**New adapter:** `src/lib/recipeAdapters.ts`

**Pages rewired to real services:** `ForgotPassword.tsx`, `ResetPassword.tsx` (rebuilt from a disabled placeholder), `EditProfile.tsx`, `Recipes.tsx`, `RecipeDetails.tsx`, `CommentManagement.tsx`, `RatingOverview.tsx`, `BakerstaffDashboard.tsx`, `AIVideoGeneration.tsx`, `MyRecipesPage.tsx`, `RecipeEditorPage.tsx`, `RecipeDetailPage.tsx`, `NotificationButton.tsx`, `dashboard/Notifications.tsx`, `ProductManagementPage.tsx`, `ExpenseVoucherPage.tsx`, `StaffManagementPage.tsx`

**Routing/infra fixes made along the way:** `ProtectedRoute.tsx` (removed leftover git-conflict markers in a comment), `routes.ts`/`AppRouter.tsx` (added missing `ROUTES.resetPassword`, re-enabled its route), `AuthContext.tsx` (forced-logout listener, new profile methods)

**Unrelated pre-existing changes in the working tree (not from this session):** `config/navigation.ts`, `mockdata/dashboard.ts`, `mockdata/users.ts`, `pages/dashboard/Dashboard.tsx`, `services/adminService.ts` — all part of an uncommitted role-casing change (`"admin"` → `"ADMIN"` etc.) that predates this integration work.

### 2026-10-17 session (this update)

**New type files (7), all `Api*`-prefixed to sit alongside the old mock types without colliding:**
`orderApi.ts`, `paymentApi.ts`, `inventoryApi.ts`, `supplierApi.ts`, `receiptApi.ts`, `shippingApi.ts`, `addressApi.ts` — registered in `types/index.ts`'s barrel export.

**New service files (14):**
`orderService.ts`, `staffOrderService.ts`, `paymentService.ts`, `inventoryService.ts`, `stockInService.ts`, `stockOutService.ts`, `stocktakeService.ts`, `supplierApiService.ts`, `receiptCategoryService.ts`, `receiptVoucherService.ts`, `shippingService.ts`, `staffShipmentService.ts`, `customerAddressApiService.ts`, `staffCustomerAddressService.ts`

**Pages touched:** none. This was deliberately scoped to service/type layer only, group by group, for review before any UI rewiring.

**Old mock type files left untouched (still consumed by mock pages):** `types/order.ts`, `types/payment.ts`, `types/inventory.ts`, `types/supplier.ts`.

## 4. Remaining Mock Pages and Reasons

| Page | Reason |
|---|---|
| `UserManagementPage.tsx` | Backend admin-accounts endpoint can't list ADMIN or CUSTOMER — confirmed, no change |
| `PaymentManagement.tsx` | Service layer now exists (`paymentService.ts`); UI not yet rewired. BE only offers PayOS link creation, no payment history/listing endpoint |
| `CashFlowPage.tsx` | Service layer now exists (`receiptVoucherService.ts` + `expenseVoucherService.ts`); UI not yet rewired, and no single BE aggregate endpoint — would need to combine both client-side |
| `ReportsManagement.tsx` | No dedicated Reports endpoint; partial data available via `staffOrderService.revenueReport` + voucher report endpoints, not wired |
| `AIModerationLogs.tsx`, `StoreManagement.tsx` | No moderation-log or store-management endpoint — confirmed absent |
| `SupplierManagementPage.tsx` + staff equivalent | Service layer now exists (`supplierApiService.ts`); UI not yet rewired |
| `InventoryPage.tsx`, `DamagedGoodsPage.tsx` + staff equivalents | Service layer now exists (`inventoryService.ts`, `stockInService.ts`, `stockOutService.ts`, `stocktakeService.ts`); UI not yet rewired |
| `OrderManagementPage.tsx` + staff equivalent | Service layer now exists (`orderService.ts`, `staffOrderService.ts`); UI not yet rewired |
| `AdminDashboard.tsx`, `StaffDashboard.tsx`, `StoreDashboard.tsx` | Depend on the same aggregates above; still mocked |

## 5. Unused Services and Why

- `customerProfileService.ts` — no `CUSTOMER` role in this web app's UI
- `hobbyService.ts` / `adminHobbyService` (same file) — no hobby-management page built
- `cartService.ts` — no cart/checkout page built
- `ingredientService.ts`, `toolService.ts`, `recipeStepService.ts` — recipe editor sends these embedded in the recipe payload instead of via standalone CRUD
- **New as of 2026-10-17 — built but not yet wired to any page:** `orderService.ts`, `staffOrderService.ts`, `paymentService.ts`, `inventoryService.ts`, `stockInService.ts`, `stockOutService.ts`, `stocktakeService.ts`, `supplierApiService.ts`, `receiptCategoryService.ts`, `receiptVoucherService.ts`, `shippingService.ts`, `staffShipmentService.ts`, `customerAddressApiService.ts`, `staffCustomerAddressService.ts` — all target real, confirmed-existing BE endpoints; the corresponding pages (§4) just haven't been rewired yet.

**Correction:** `walletService.ts`/`topUpService.ts`/`payoutService.ts` were previously listed here as "built, no consumer." They remain unconsumed, but it's now confirmed the backend has **no matching controller at all** — these three services call endpoints that don't exist server-side. They should probably be deleted rather than left waiting for a consumer, unless the wallet/top-up/payout feature is actually planned.

## 6. Backend Gaps and Prioritized Recommendations

| Priority | Gap | Affected page(s) | Evidence | Status |
|---|---|---|---|---|
| High | Admin accounts endpoint excludes ADMIN/CUSTOMER | `UserManagementPage.tsx` | `AdminAccountServiceImpl.MANAGED_ROLES = {STAFF, MANAGER, BAKER_STAFF}` | Still open — backend scope |
| Medium | No reports/moderation-log/store-management controller | `ReportsManagement.tsx`, `AIModerationLogs.tsx`, `StoreManagement.tsx` | No matching controller in 35-controller inventory | Still open — backend scope. Note a reports page could partially assemble from `staffOrderService.revenueReport` + voucher reports without a new endpoint |
| Medium | No order-payment history/listing endpoint | `PaymentManagement.tsx` | `PaymentController` only has `POST .../payos-link`; no GET for payment records | Still open — would need a new BE endpoint, or derive history from `OrderResponse`/webhook-driven status |
| ~~High~~ Resolved | ~~No Order entity/endpoint~~ | `OrderManagementPage.tsx` | `CustomerOrderController`/`StaffOrderController` exist | FE service layer built 2026-10-17 (`orderService.ts`/`staffOrderService.ts`); UI rewiring pending |
| ~~High~~ Resolved | ~~No cash-flow aggregate~~ | `CashFlowPage.tsx` | `ReceiptVoucherController` (income) pairs with `ExpenseVoucherController` (outflow) | FE service layer built 2026-10-17 (`receiptVoucherService.ts`); UI rewiring pending, still needs client-side combination of both |
| ~~Medium~~ Resolved | ~~No Supplier/Inventory controllers~~ | `SupplierManagementPage.tsx`, `InventoryPage.tsx`, `DamagedGoodsPage.tsx` | `SupplierController` + full `warehouse/*` module (Inventory/StockIn/StockOut/Stocktake) exist | FE service layer built 2026-10-17; UI rewiring pending |
| Low | Dashboard aggregates | `AdminDashboard.tsx`, `StaffDashboard.tsx`, `StoreDashboard.tsx` | Depend on Order/Payment/Reports wiring | Downstream — revisit once the pages above are rewired |

## 7. Validation and Testing Results

- **TypeScript (`npx tsc -b --noEmit`)**: only the pre-existing `StoreDashboard.tsx` errors remain (predate all integration work, unrelated). Every file touched in both the 2026-10-15 and 2026-10-17 sessions compiles clean.
- **ESLint**: clean on every new/touched file in both sessions.
- **Automated tests**: none configured (`package.json` has no `test` script).
- **Live backend**: still not available in this environment. Nothing in §2 marked 🟡 has been called against a real server — field names/shapes were derived by reading the BE controller and DTO source directly, not from a live response. Priority items to verify once a backend is reachable: `SpringPage` vs plain-array response shape per endpoint (mixed across the new modules — see each service file's comments), the `LocalDateTime`/`LocalDate` string format actually returned by Jackson, and the no-`@PreAuthorize` warehouse endpoints (confirm there isn't gateway-level auth not visible in the controller source).

## 8. Recommended Next Steps

1. **Wire the 14 new services into their mock pages**, group by group (Order+Payment → Inventory+Supplier → Receipt/cash-flow), replacing `MOCK_*` arrays and the old `types/order.ts`/`payment.ts`/`inventory.ts`/`supplier.ts` imports.
2. **Run against a live backend** once available, to confirm response shapes assumed from source reading (see §7) — especially the mixed `SpringPage`/plain-array pagination across the new warehouse/order/receipt endpoints.
3. **Decide the fate of `walletService`/`topUpService`/`payoutService`** — delete, since no backend controller backs them, unless the feature is still planned.
4. **Decide the fate of `UserManagementPage.tsx`** — either narrow it to match `StaffManagementPage.tsx`'s real scope or wait for a broader admin-accounts endpoint.
5. Resolve the pre-existing uncommitted role-casing change in `navigation.ts`/`mockdata/*`/`Dashboard.tsx`/`adminService.ts` separately — unrelated to this integration.
