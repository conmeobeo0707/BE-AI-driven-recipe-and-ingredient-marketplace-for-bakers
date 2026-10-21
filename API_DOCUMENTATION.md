# API module Product và Warehouse

Tài liệu này mô tả các endpoint hiện có trong code của hai module. Tất cả request/response dùng JSON trừ query string. Các ID là UUID. Các endpoint dưới đây cần access token hợp lệ; cấu hình hiện tại chưa đặt giới hạn role riêng cho từng endpoint. Gửi token bằng header `Authorization: Bearer <access-token>`.

## Quy ước response và lỗi

Response thành công theo `ApiResponse`:

```json
{"success":true,"data":{}}
```

Endpoint trả về `void` có dạng `{"success":true}`. Response lỗi:

```json
{"success":false,"message":"Mô tả lỗi"}
```

Các lỗi chung:

| HTTP | Trường hợp | Response message |
|---|---|---|
| 400 | Body không hợp lệ, thiếu/tham số sai, vi phạm validation | Message validation hoặc `Request body is invalid or contains an unsupported value` |
| 401 | Thiếu token hoặc token không hợp lệ/hết hạn | `Authentication is required or the access token is invalid or expired` |
| 403 | Không được phép truy cập | `Access denied` |
| 404 | Không tìm thấy tài nguyên | Tùy lỗi nghiệp vụ |
| 409 | Xung đột nghiệp vụ, tồn kho không đủ hoặc dữ liệu bị cập nhật đồng thời | Tùy lỗi nghiệp vụ |
| 500 | Lỗi ngoài dự kiến | `Something went wrong.` |

Validation thường trả HTTP 400; lỗi nghiệp vụ của Product/Warehouse có thể trả 400, 404 hoặc 409. Các ví dụ lỗi bên dưới nêu nhóm lỗi có thể gặp, vì message cụ thể phụ thuộc tình huống.

## Product

### Sản phẩm — `/api/products`

| Method & path | Chức năng | Input | Thành công | Lỗi có thể gặp |
|---|---|---|---|---|
| `POST /api/products` | Tạo sản phẩm cùng danh sách unit nếu được gửi. | JSON `ProductUpsertRequest` xem bên dưới. | `201`, `ApiResponse<ProductResponse>` | `400` dữ liệu/đơn vị không hợp lệ; `404` category không tồn tại; `409` SKU trùng hoặc xung đột dữ liệu. |
| `PUT /api/products/{productId}` | Cập nhật sản phẩm và các unit. | Path `productId`; JSON cùng cấu trúc tạo. Danh sách `units`, khi được gửi, là toàn bộ danh sách mong muốn; unit cũ bị bỏ khỏi danh sách chuyển sang `INACTIVE`. | `200`, `ApiResponse<ProductResponse>` | `400` dữ liệu không hợp lệ; `404` sản phẩm/category không tồn tại; `409` SKU hoặc trạng thái unit xung đột. |
| `GET /api/products/{productId}` | Lấy một sản phẩm kèm unit. | Path `productId`. | `200`, `ApiResponse<ProductResponse>` | `404` không tìm thấy sản phẩm. |
| `GET /api/products` | Phân trang sản phẩm, lọc tùy chọn theo category và brand. | Query tùy chọn: `categoryId`, `brand`, cùng tham số Spring `page`, `size`, `sort` (ví dụ `?page=0&size=20&sort=name,asc`). | `200`, `ApiResponse<Page<ProductResponse>>` | `400` UUID hoặc tham số phân trang/sort không hợp lệ. |
| `DELETE /api/products/{productId}` | Ngừng kích hoạt sản phẩm (deactivate). | Path `productId`. | `200`, `{"success":true}` | `404` không tìm thấy; `409` không thể ngừng kích hoạt do ràng buộc nghiệp vụ. |
| `GET /api/products/{productId}/units/{unitId}/price-history` | Xem lịch sử giá bán của một unit. | Path `productId`, `unitId`. | `200`, `ApiResponse<List<UnitPriceHistoryResponse>>` | `404` sản phẩm/unit không tồn tại hoặc không thuộc sản phẩm. |
| `PUT /api/products/{productId}/combo-items` | Thay danh sách thành phần combo. Các sản phẩm con có thể dùng chung ở nhiều combo. | Path `productId`; JSON array `[{"productId":"uuid","quantity":2}]`. Số lượng phải lớn hơn 0. | `200`, `ApiResponse<List<ComboItemRequest>>` | `400` danh sách/thành phần không hợp lệ; `404` sản phẩm/combo/con không tồn tại; `409` quan hệ combo xung đột. |
| `GET /api/products/{productId}/combo-stock` | Tính tồn kho khả dụng của combo từ tồn các sản phẩm con. | Path `productId`. | `200`, `ApiResponse<BigDecimal>` | `404` sản phẩm không tồn tại; `400` sản phẩm không phải combo hoặc cấu hình combo không hợp lệ. |

**ProductUpsertRequest** (các trường không ghi bắt buộc là tùy chọn):

```json
{
  "sku":"FLOUR-001",
  "name":"Bột mì",
  "description":"Bột mì làm bánh",
  "categoryId":"uuid",
  "imgUrl":"https://example/image.jpg",
  "discountPercent":0,
  "storageGuide":"Nơi khô ráo",
  "minStock":5,
  "maxStock":100,
  "status":"ACTIVE",
  "brand":"ABC",
  "combo":false,
  "units":[
    {"unitName":"kg","conversionFactor":1,"base":true,"sale":true,"sellingPrice":30000,"status":"ACTIVE","weightGram":1000}
  ]
}
```

`sku` (tối đa 100 ký tự) và `name` (tối đa 255 ký tự) bắt buộc. `discountPercent` từ 0 đến 100; `minStock`/`maxStock` không âm. Mỗi unit có `unitName`, `conversionFactor` (>0), `base`, `sellingPrice` (không âm); `sale`, `status`, `weightGram` tùy chọn. Mỗi unit thuộc riêng sản phẩm. Khi tạo/cập nhật, backend áp dụng các quy tắc nhất quán về unit cơ sở, tên unit và lịch sử giá.

### Danh mục — `/api/product-categories`

| Method & path | Chức năng | Input | Thành công | Lỗi có thể gặp |
|---|---|---|---|---|
| `POST /api/product-categories` | Tạo danh mục. | JSON `{"categoryName":"Nguyên liệu"}`; bắt buộc, tối đa 255 ký tự. | `201`, `ApiResponse<CategoryResponse>` | `400` tên trống/quá dài; `409` tên trùng (nếu bị ràng buộc). |
| `PUT /api/product-categories/{id}` | Đổi tên danh mục. | Path `id`; cùng body tạo. | `200`, `ApiResponse<CategoryResponse>` | `400` body không hợp lệ; `404` không tìm thấy; `409` tên bị trùng hoặc đang bị ràng buộc. |
| `GET /api/product-categories` | Lấy danh sách danh mục. | Không có. | `200`, `ApiResponse<List<CategoryResponse>>` | `500` lỗi ngoài dự kiến. |
| `DELETE /api/product-categories/{id}` | Xóa danh mục. | Path `id`. | `200`, `{"success":true}` | `404` không tìm thấy; `409` không thể xóa do sản phẩm đang tham chiếu. |

## Warehouse

### Tồn kho — `/api/warehouse/inventory`

| Method & path | Chức năng | Input | Thành công | Lỗi có thể gặp |
|---|---|---|---|---|
| `GET /api/warehouse/inventory/available?productId={uuid}` | Xem tồn thực có thể bán, số lượng đang giữ và tồn khả dụng. | Query bắt buộc `productId`. | `200`, `ApiResponse<AvailableInventoryResponse>` gồm `productId`, `saleableOnHandQuantity`, `reservedQuantity`, `availableQuantity`. | `400` thiếu/sai UUID; `404` không tìm thấy sản phẩm. |
| `GET /api/warehouse/inventory/lots?productId={uuid}` | Xem các lô của sản phẩm, sắp theo hạn dùng rồi thời điểm tạo. | Query bắt buộc `productId`. | `200`, `ApiResponse<List<StockLot>>` | `400` thiếu/sai UUID; `404` sản phẩm không tồn tại. |
| `GET /api/warehouse/inventory/low-stock` | Danh sách sản phẩm dưới ngưỡng tồn tối thiểu. | Không có. | `200`, `ApiResponse<List<ProductResponse>>` | `500` lỗi ngoài dự kiến. |
| `GET /api/warehouse/inventory/expiring-lots?withinDays=30` | Tìm các lô sắp hết hạn trong số ngày chỉ định (mặc định 30). | Query tùy chọn `withinDays`, từ 0 đến 3650. | `200`, `ApiResponse<List<StockLot>>` | `400` giá trị ngoài khoảng hoặc không phải số. |

### Phiếu nhập — `/api/warehouse/stock-in`

| Method & path | Chức năng | Input | Thành công | Lỗi có thể gặp |
|---|---|---|---|---|
| `POST /api/warehouse/stock-in` | Tạo phiếu nhập ở trạng thái chờ xử lý. | Body `StockInCreateRequest` bên dưới. | `201`, `ApiResponse<StockInNote>` | `400` body/chi tiết không hợp lệ; `404` supplier, product hoặc unit không tồn tại; `409` trạng thái/ràng buộc nghiệp vụ xung đột. |
| `POST /api/warehouse/stock-in/{id}/complete` | Hoàn tất phiếu; tạo lô và cộng tồn theo các dòng nhập. | Path `id`. | `200`, `ApiResponse<StockInNote>` | `404` phiếu không tồn tại; `409` phiếu không ở trạng thái cho phép hoàn tất hoặc dữ liệu đã được xử lý. |
| `POST /api/warehouse/stock-in/{id}/cancel` | Hủy phiếu nhập chưa hoàn tất. | Path `id`. | `200`, `ApiResponse<StockInNote>` | `404` không tìm thấy; `409` phiếu đã hoàn tất/không thể hủy. |
| `GET /api/warehouse/stock-in` | Danh sách phiếu nhập có phân trang, tùy chọn lọc trạng thái. | Query `status` (`PENDING`, `COMPLETED`, `CANCELLED`), `page`, `size`, `sort`; mặc định size 20, `createdAt DESC`. | `200`, `ApiResponse<Page<StockInNote>>` | `400` status hoặc tham số phân trang không hợp lệ. |
| `GET /api/warehouse/stock-in/{id}` | Chi tiết phiếu nhập và các dòng hàng. | Path `id`. | `200`, `ApiResponse<StockInDetailResponse>` | `404` không tìm thấy phiếu. |

Body tạo phiếu nhập:

```json
{
  "supplierId":"uuid",
  "receiptDate":"2026-10-21T10:30:00",
  "paidAmount":500000,
  "note":"Nhập hàng đợt 1",
  "items":[{"productId":"uuid","unitId":"uuid","quantity":10,"unitPrice":50000,"expiryDate":"2027-10-21"}]
}
```

`supplierId` và ít nhất một `items` bắt buộc. Mỗi dòng cần `productId`, `unitId`, `quantity` > 0 và `unitPrice` không âm; `expiryDate` có thể bỏ trống với hàng không hạn sử dụng. `receiptDate`, `paidAmount`, `note` tùy chọn.

### Phiếu xuất — `/api/warehouse/stock-out`

| Method & path | Chức năng | Input | Thành công | Lỗi có thể gặp |
|---|---|---|---|---|
| `POST /api/warehouse/stock-out` | Tạo phiếu xuất chờ xử lý. Với SALE, lô được phân bổ FEFO khi hoàn tất; với DISPOSAL, chỉ định lô ở từng dòng. | Body `StockOutCreateRequest` bên dưới. | `201`, `ApiResponse<StockOutNote>` | `400` body sai; `404` sản phẩm/unit/lô không tồn tại; `409` loại phiếu/dữ liệu xung đột. |
| `POST /api/warehouse/stock-out/{id}/complete` | Hoàn tất phiếu và trừ tồn theo lô. | Path `id`. | `200`, `ApiResponse<StockOutNote>` | `404` không tìm thấy; `409` tồn không đủ hoặc trạng thái không cho phép. |
| `POST /api/warehouse/stock-out/{id}/cancel` | Hủy phiếu xuất chưa hoàn tất. | Path `id`. | `200`, `ApiResponse<StockOutNote>` | `404` không tìm thấy; `409` phiếu đã hoàn tất/không thể hủy. |
| `GET /api/warehouse/stock-out` | Danh sách phiếu xuất, lọc tùy chọn theo trạng thái/loại. | Query `status` (`PENDING`, `COMPLETED`, `CANCELLED`), `type` (`SALE`, `DISPOSAL`), `page`, `size`, `sort`; mặc định size 20, `createdAt DESC`. | `200`, `ApiResponse<Page<StockOutNote>>` | `400` enum hoặc tham số phân trang không hợp lệ. |
| `GET /api/warehouse/stock-out/{id}` | Chi tiết phiếu xuất và các dòng hàng. | Path `id`. | `200`, `ApiResponse<StockOutDetailResponse>` | `404` không tìm thấy phiếu. |

Body tạo phiếu xuất:

```json
{
  "type":"SALE",
  "referenceOrderId":"uuid",
  "note":"Xuất giao đơn hàng",
  "items":[{"productId":"uuid","unitId":"uuid","quantity":2}]
}
```

`type` và ít nhất một `items` bắt buộc. Mỗi dòng cần `productId`, `unitId`, `quantity` > 0. Với `DISPOSAL`, cần gửi `lotId` để chỉ rõ lô; với `SALE`, việc phân bổ lô chạy ở bước hoàn tất. `referenceOrderId` và `note` tùy chọn.

### Giữ hàng cho đơn — `/api/warehouse/reservations`

| Method & path | Chức năng | Input | Thành công | Lỗi có thể gặp |
|---|---|---|---|---|
| `POST /api/warehouse/reservations` | Tạo reservation, giữ tồn cho một order đến `expiresAt`. | Body bên dưới. | `201`, `ApiResponse<StockReservationResponse>` | `400` thiếu/sai trường hoặc `expiresAt` không ở tương lai; `404` product/unit không tồn tại; `409` tồn khả dụng không đủ hoặc đã có reservation xung đột. |
| `GET /api/warehouse/reservations/order/{orderId}` | Xem reservation theo order. | Path `orderId`. | `200`, `ApiResponse<StockReservationResponse>` | `404` chưa có reservation/không tìm thấy. |
| `POST /api/warehouse/reservations/order/{orderId}/release` | Giải phóng số lượng đã giữ cho order bị hủy. | Path `orderId`. | `200`, `ApiResponse<StockReservationResponse>` | `404` không tìm thấy; `409` reservation ở trạng thái không thể release. |
| `POST /api/warehouse/reservations/order/{orderId}/consume` | Dùng reservation để tạo và hoàn tất phiếu xuất SALE. | Path `orderId`. | `200`, `ApiResponse<StockOutNote>` | `404` reservation/order không tồn tại; `409` reservation hết hạn/không còn hiệu lực hoặc lỗi tồn kho. |

```json
{
  "orderId":"uuid",
  "expiresAt":"2026-10-21T12:30:00",
  "items":[{"productId":"uuid","unitId":"uuid","quantity":2}]
}
```

`orderId`, thời điểm hết hạn tương lai và ít nhất một dòng hàng là bắt buộc. Số lượng phải lớn hơn 0. Các endpoint này hiện được cung cấp ở Warehouse; module Order chưa tự động gọi chúng khi tạo/hủy/giao đơn.

### Kiểm kê — `/api/warehouse/stocktakes`

| Method & path | Chức năng | Input | Thành công | Lỗi có thể gặp |
|---|---|---|---|---|
| `POST /api/warehouse/stocktakes` | Tạo phiếu kiểm kê theo danh sách sản phẩm và snapshot các lô cần đếm. | Body `{"productIds":["uuid"],"handleBy":"uuid"}`; `productIds` bắt buộc, `handleBy` tùy chọn. | `201`, `ApiResponse<StocktakeDetailResponse>` | `400` danh sách rỗng/sai; `404` product không tồn tại; `409` lô đã bị khóa bởi phiếu kiểm kê khác. |
| `PUT /api/warehouse/stocktakes/{noteId}/counts` | Gửi số đếm thực tế theo từng sản phẩm và từng dòng lô thuộc phiếu. | Path `noteId`; JSON mẫu bên dưới. | `200`, `ApiResponse<StocktakeDetailResponse>` | `400` số lượng âm/dữ liệu sai; `404` phiếu hoặc dòng kiểm kê không tồn tại; `409` phiếu không ở trạng thái cho phép cập nhật. |
| `POST /api/warehouse/stocktakes/{noteId}/complete` | Hoàn tất kiểm kê, áp dụng chênh lệch số lượng vào tồn kho lô/sản phẩm. | Path `noteId`. | `200`, `ApiResponse<StocktakeDetailResponse>` | `404` không tìm thấy; `409` phiếu chưa đủ số đếm, đã xử lý hoặc không thể hoàn tất. |
| `POST /api/warehouse/stocktakes/{noteId}/cancel` | Hủy phiếu kiểm kê nháp và giải phóng lô đang khóa. | Path `noteId`. | `200`, `ApiResponse<StocktakeNote>` | `404` không tìm thấy; `409` phiếu đã hoàn tất/không thể hủy. |
| `GET /api/warehouse/stocktakes` | Danh sách phiếu kiểm kê có phân trang, lọc status. | Query `status` (`DRAFT`, `COMPLETED`, `CANCELLED`), `page`, `size`, `sort`; mặc định size 20, `createdAt DESC`. | `200`, `ApiResponse<Page<StocktakeNote>>` | `400` enum hoặc tham số phân trang không hợp lệ. |
| `GET /api/warehouse/stocktakes/{noteId}` | Chi tiết phiếu, nhóm các dòng lô theo sản phẩm. | Path `noteId`. | `200`, `ApiResponse<StocktakeDetailResponse>` | `404` không tìm thấy phiếu. |

Mẫu cập nhật số đếm. `itemId` là ID dòng kiểm kê do API chi tiết trả về; API nhận product và các dòng lô, không nhận `lotId` làm khóa dòng:

```json
{
  "products":[{
    "productId":"uuid",
    "lots":[{"itemId":"uuid","actualQuantity":8,"reason":"Bao bì rách","note":"Đã kiểm lại"}]
  }]
}
```

`actualQuantity` không âm. `reason` tối đa 255 ký tự, `note` tối đa 5000 ký tự.

### Nhà cung cấp — `/api/suppliers`

| Method & path | Chức năng | Input | Thành công | Lỗi có thể gặp |
|---|---|---|---|---|
| `POST /api/suppliers` | Tạo nhà cung cấp để dùng trên phiếu nhập. | JSON bên dưới. | `201`, `ApiResponse<SupplierResponse>` | `400` tên/email sai định dạng; `409` xung đột dữ liệu. |
| `PUT /api/suppliers/{id}` | Cập nhật thông tin nhà cung cấp. | Path `id`; body như tạo. | `200`, `ApiResponse<SupplierResponse>` | `400` body không hợp lệ; `404` không tìm thấy; `409` xung đột dữ liệu. |
| `GET /api/suppliers/{id}` | Xem nhà cung cấp. | Path `id`. | `200`, `ApiResponse<SupplierResponse>` | `404` không tìm thấy. |
| `GET /api/suppliers` | Tìm kiếm/phân trang theo tên, điện thoại hoặc email tùy logic service. | Query tùy chọn `keyword`, `page`, `size`, `sort`; mặc định size 20, `supplierName ASC`. | `200`, `ApiResponse<Page<SupplierResponse>>` | `400` tham số phân trang không hợp lệ. |
| `DELETE /api/suppliers/{id}` | Xóa nhà cung cấp. | Path `id`. | `200`, `{"success":true}` | `404` không tìm thấy; `409` đang được phiếu nhập tham chiếu. |

```json
{"supplierName":"Nhà cung cấp ABC","phoneNumber":"0900000000","email":"sales@example.com","address":"Hà Nội"}
```

`supplierName` bắt buộc; `phoneNumber` tối đa 20, email hợp lệ tối đa 100, địa chỉ tối đa 5000 ký tự. Supplier response hiện không có trường đối soát công nợ.

### Thanh toán nhà cung cấp — `/api/suppliers/{supplierId}/payments`

| Method & path | Chức năng | Input | Thành công | Lỗi có thể gặp |
|---|---|---|---|---|
| `POST /api/suppliers/{supplierId}/payments` | Tạo phiếu thanh toán và phân bổ toàn bộ số tiền vào một hoặc nhiều phiếu nhập đã hoàn tất của nhà cung cấp. Cập nhật `paid_amount`, `debt_amount`, `payment_status` tương ứng. | Path `supplierId`; JSON mẫu bên dưới. | `201`, `ApiResponse<SupplierPaymentDetailResponse>` | `400` thiếu/sai dữ liệu, tổng chi tiết không bằng `amount`, hoặc lặp phiếu nhập; `404` nhà cung cấp/phiếu nhập không tìm thấy hoặc phiếu không thuộc nhà cung cấp; `409` một khoản phân bổ lớn hơn dư nợ còn lại. |
| `GET /api/suppliers/{supplierId}/payments` | Danh sách phiếu thanh toán của nhà cung cấp. | Path `supplierId`; `page`, `size`, `sort`; mặc định size 20, `createdAt DESC`. | `200`, `ApiResponse<Page<SupplierPaymentResponse>>` | `400` tham số phân trang/sort sai; `404` nhà cung cấp không tồn tại. |
| `GET /api/suppliers/{supplierId}/payments/{paymentId}` | Chi tiết phiếu thanh toán và các phiếu nhập được phân bổ. | Path `supplierId`, `paymentId`. | `200`, `ApiResponse<SupplierPaymentDetailResponse>` | `404` không tìm thấy phiếu thanh toán thuộc nhà cung cấp này. |

Body tạo thanh toán:

```json
{
  "amount": 150000,
  "details": [
    {"stockInNoteId":"uuid-phiếu-nhập-1","amount":100000},
    {"stockInNoteId":"uuid-phiếu-nhập-2","amount":50000}
  ]
}
```

`amount` và từng khoản phân bổ phải lớn hơn 0, tối đa 2 chữ số thập phân. Tổng `details.amount` phải bằng chính xác `amount`; không hỗ trợ tiền chưa phân bổ. Mỗi phiếu nhập chỉ được xuất hiện một lần trong một request, phải ở trạng thái `COMPLETED`, thuộc đúng nhà cung cấp và số phân bổ không được vượt `debt_amount` còn lại. Khi `debt_amount` về 0, trạng thái công nợ chuyển thành `PAID`; nếu còn dư nợ, chuyển thành `PARTIAL`.

## Ghi chú về phân trang

Các API list dùng Spring `Pageable`. Ví dụ `?page=0&size=10&sort=createdAt,desc`; page bắt đầu từ 0. `sort` dùng tên thuộc tính entity được hỗ trợ bởi endpoint. Cấu trúc `Page` được serialize theo phiên bản Spring đang chạy.

