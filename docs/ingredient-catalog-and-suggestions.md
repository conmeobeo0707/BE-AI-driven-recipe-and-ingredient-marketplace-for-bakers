# Ingredient catalog, alias và recipe suggestion

## Phạm vi

Đã có quản lý catalog/alias, duyệt backfill, chọn catalog ID, cảnh báo input chưa khớp và gợi ý tên gần đúng. Không sinh alias bằng AI và không lưu alias khi Customer confirm.

Ảnh vẫn đi qua Groq. Các API catalog, fuzzy, backfill và confirm không gọi AI.

## Chuẩn bị database

Migration mới: `V40__guard_ingredient_catalog_alias_names.sql`. Không sửa V39 đã apply. V40 kiểm tra tên chuẩn hóa của catalog có trỏ tới alias thuộc catalog khác không; có xung đột sẽ dừng để sửa dữ liệu trước. Migration không xóa hoặc tự gộp dữ liệu.

Kiểm tra xung đột trước khi chạy:

```sql
SELECT c.id AS catalog_id, c.canonical_name, a.id AS alias_id,
       a.alias, a.ingredient_id AS alias_catalog_id
FROM ingredient_catalog c
JOIN ingredient_aliases a ON a.normalized_alias = c.normalized_name
WHERE a.ingredient_id <> c.id;
```

V40 thêm trigger chống tên trùng giữa hai bảng. App và trigger dùng cùng advisory lock trong transaction để tuần tự hóa việc sửa namespace. Các unique constraint trong từng bảng của V39 vẫn được giữ.

Không apply migration hoặc backfill trực tiếp lên Neon trong quá trình thay đổi code này. Chạy migration theo quy trình deploy của team; API backfill chỉ ghi sau khi có danh sách mappings được duyệt.

## Quyền

Tất cả API cần `Authorization: Bearer <accessToken>`.

| Nhóm API | Role |
| --- | --- |
| `/api/v1/management/ingredient-catalog/**` | ADMIN, BAKER_STAFF |
| `GET /api/v1/ingredient-catalog` | CUSTOMER, ADMIN, BAKER_STAFF |
| `/api/v1/recipe-suggestions/**` | CUSTOMER |

Base URL ví dụ: `http://localhost:8080`.

## 1. Quản lý catalog và alias

### Danh sách / chi tiết

```http
GET /api/v1/management/ingredient-catalog
GET /api/v1/management/ingredient-catalog?active=true
GET /api/v1/management/ingredient-catalog?active=false
GET /api/v1/management/ingredient-catalog/{catalogId}
```

Bỏ `active` lấy cả hai trạng thái. Catalog mới active, có alias tự thân. Không xóa cứng catalog.

### Tạo

```http
POST /api/v1/management/ingredient-catalog
Content-Type: application/json
```

```json
{
  "canonicalName": "Khoai tây"
}
```

`canonicalName` bắt buộc, tối đa 150 ký tự và phải còn chữ/số sau chuẩn hóa. Lưu `data.id` làm `catalogId` trong Postman. Nếu tên đã tồn tại, dùng ID đang có thay vì tạo thêm.

### Sửa tên

```http
PUT /api/v1/management/ingredient-catalog/{catalogId}
Content-Type: application/json
```

```json
{
  "canonicalName": "Khoai tây (Potato)"
}
```

Giữ nguyên ID; tên chuẩn trước đó được giữ làm alias. Đây là đổi tên cùng nguyên liệu, không dùng để chuyển một loại nguyên liệu thành loại khác.

### Bật / tắt

```http
PATCH /api/v1/management/ingredient-catalog/{catalogId}/active
Content-Type: application/json
```

```json
{
  "active": false
}
```

`DELETE /api/v1/management/ingredient-catalog/{catalogId}` cũng đặt active=false. Catalog đang được recipe PUBLISHED dùng sẽ bị chặn bằng HTTP 409. Không xóa các liên kết recipe hoặc alias. Bật lại bằng `active: true`.

### Alias

```http
GET /api/v1/management/ingredient-catalog/{catalogId}/aliases
POST /api/v1/management/ingredient-catalog/{catalogId}/aliases
PUT /api/v1/management/ingredient-catalog/{catalogId}/aliases/{aliasId}
DELETE /api/v1/management/ingredient-catalog/{catalogId}/aliases/{aliasId}
```

Body POST / PUT:

```json
{
  "alias": "Potato",
  "languageCode": "en"
}
```

Alias bắt buộc, tối đa 150 ký tự; languageCode có thể bỏ/null, tối đa 10 ký tự, ví dụ vi, en, en-US. Trùng sau chuẩn hóa trả 409. Không được xóa/đổi alias tự thân sang tên chuẩn hóa khác. Đổi/xóa alias chỉ ảnh hưởng resolve tên trong tương lai; ID đã gắn vào recipe không đổi.

Tên thuộc catalog inactive vẫn được giữ chỗ, không tái sử dụng cho catalog khác. Không tự dịch, tự phân loại hoặc tự gộp biến thể màu/loại.

## 2. Rà soát và backfill dữ liệu cũ

```http
GET /api/v1/management/ingredient-catalog/backfill/preview?limit=100
```

`limit` từ 1 đến 200. Response `data` có `totalUnmapped` và `items`:

```json
{
  "ingredientId": "11111111-1111-4111-8111-111111111111",
  "recipeId": "22222222-2222-4222-8222-222222222222",
  "ingredientName": "Potato",
  "proposedCatalogIngredientId": "33333333-3333-4333-8333-333333333333",
  "proposedCanonicalName": "Khoai tây",
  "candidates": []
}
```

Proposed ID chỉ có khi exact resolve thành công. Nếu chưa exact, candidates có thể trả gợi ý để nhân viên chọn. Không tự gán fuzzy.

Sau khi duyệt, gửi ID và tên lấy từ preview:

```http
POST /api/v1/management/ingredient-catalog/backfill
Content-Type: application/json
```

```json
{
  "mappings": [
    {
      "ingredientId": "11111111-1111-4111-8111-111111111111",
      "catalogIngredientId": "33333333-3333-4333-8333-333333333333",
      "expectedName": "Potato"
    }
  ]
}
```

Thay UUID ví dụ bằng ID thật. Mỗi lô 1–200 dòng, không lặp ingredientId. `expectedName` phải giống nguyên văn `ingredientName` ở preview. Dòng đã mapping/đổi tên/xóa trả 409 và rollback cả lô. Catalog thiếu/inactive trả 400. Không sửa tên, lượng hoặc đơn vị. Thành công trả `data.updatedCount`. Tiếp tục preview để xử lý lô sau; các dòng `string`, `a` cần review, không tự gán đại.

## 3. Tra cứu và fuzzy matching

```http
GET /api/v1/ingredient-catalog?keyword=potato&limit=10
GET /api/v1/ingredient-catalog?keyword=potatoo&limit=10
GET /api/v1/ingredient-catalog?keyword=yellow%20bell%20pepper&limit=10
GET /api/v1/ingredient-catalog?limit=10
```

Chỉ catalog active. Keyword tối đa 150 ký tự; bỏ/blank để xem danh sách. Limit mặc định 10, tối đa 20.

Response là danh sách candidate, mỗi catalog một lần:

```json
{
  "catalogIngredientId": "33333333-3333-4333-8333-333333333333",
  "canonicalName": "Khoai tây",
  "matchedName": "Potato",
  "similarity": 0.857,
  "matchType": "FUZZY"
}
```

Ưu tiên EXACT → PARTIAL → FUZZY. Điểm fuzzy là normalized Levenshtein, ngưỡng 0.75; chỉ áp dụng tên dài ít nhất 3 ký tự. Không phải xác suất nhận diện đúng và không tìm đồng nghĩa xuyên ngôn ngữ nếu chưa có alias. Partial có thể tìm chuỗi con hoặc tên nguyên liệu nằm trọn theo từ trong input dài hơn. Người dùng phải chọn; candidate không thay đổi `matched=false` và không tham gia recipe matching.

Bản đầu dùng từ điển active trong bộ nhớ của từng request (đọc catalog/alias theo lô); không cần extension/dependency mới. Khi danh mục lớn cần chuyển truy vấn fuzzy và giới hạn ứng viên xuống database.

## 4. Nhập text và xử lý input chưa khớp

```http
POST /api/v1/recipe-suggestions/ingredients
Content-Type: application/json
```

```json
{
  "ingredients": ["Salt", "potatoo"],
  "limit": 3,
  "allowPartial": false
}
```

Giới hạn như trước: 1–20 tên, mỗi tên 120 ký tự; limit mặc định 3, tối đa 5. Field allowPartial mới có thể bỏ, mặc định false.

Nếu còn unmatched và allowPartial=false: HTTP 200, `requiresResolution=true`, `recipes=[]`. Frontend hiển thị candidates và cho tìm catalog, xóa mục nhận diện sai hoặc tiếp tục với phần đã khớp bằng allowPartial=true. Nếu không có ID nào khớp thì vẫn không tìm recipe, kể cả allowPartial=true.

Các field mới trong response:

| Field | Ý nghĩa |
| --- | --- |
| `inputIngredients[].candidates` | Gợi ý cho tên chưa resolve; không tự chọn |
| `totalInputCount` | Số input đang được xét |
| `matchedInputCount` | Số input đã resolve; có thể nhiều input cùng một catalog |
| `resolvedIngredientCount` | Số catalog ID khác nhau thực sự dùng để matching |
| `requiresResolution` | Cần sửa/chọn lại trước khi trả recipe |
| `coverageBasis` | Luôn INGREDIENT_TYPES_ONLY |
| `warnings` | Thông báo phần chưa được dùng, việc cho phép partial và giới hạn coverage |

Mỗi recipe giữ `matchedIngredients`, `missingIngredients`, số loại khớp/tổng số loại và coverage từ 0 đến 1. Hiển thị “Khớp 3/5 loại nguyên liệu”; không diễn đạt “đủ nguyên liệu để làm” vì chưa kiểm tra lượng. Chỉ recipe PUBLISHED với tất cả mapping active mới được trả.

## 5. Nhận diện ảnh và confirm bằng ID

```http
POST /api/v1/recipe-suggestions/images/recognize
```

Body form-data, field `file` kiểu File, JPEG/PNG tối đa 5 MB. Item chưa matched có candidates. Người dùng xem ảnh, sửa/xóa/bổ sung và chọn catalog ID từ kết quả exact hoặc API lookup.

**Thay đổi request:** `/images/confirm` không nhận `ingredients: ["Salt"]` nữa. Gửi ID đã chọn:

```http
POST /api/v1/recipe-suggestions/images/confirm
Content-Type: application/json
```

```json
{
  "catalogIngredientIds": ["33333333-3333-4333-8333-333333333333"],
  "unmatchedIngredients": ["unrecognized ingredient"],
  "allowPartial": false,
  "limit": 3
}
```

`catalogIngredientIds` bắt buộc, 1–20 UUID; backend xác nhận từng ID tồn tại và active. ID lặp được gộp. `unmatchedIngredients` tùy chọn/null/[] là những tên còn chưa chọn, mỗi tên tối đa 120 ký tự. Tổng số ID và tên không quá 20. Những tên này chỉ được hiển thị/cảnh báo, không resolve lại hoặc tự gán.

Để tìm chỉ bằng các ID đã chọn dù còn unmatched, Customer phải chấp nhận rồi gửi `allowPartial: true`. Khi đã xử lý hết, gửi `unmatchedIngredients: []`.

`POST /api/v1/recipe-suggestions/ingredients/confirm` nhận cùng body để xác nhận ID sau input text; khác ở inputMode=TEXT. Cả hai endpoint không gọi Groq và không tạo alias. Backend không lưu phiên ảnh hay bằng chứng người dùng đã xem ảnh; đây là bước xác nhận trên UI, dữ liệu ID vẫn được validate độc lập.

## Kiểm tra thủ công

1. ADMIN/BAKER_STAFF tạo catalog và alias; CUSTOMER gọi management phải bị 403.
2. Thử tạo catalog/alias thuộc catalog khác với cùng normalized name: 409. Thử alias tự thân: không tạo trùng/xóa/đổi sang tên khác.
3. Đổi tên chuẩn: tên cũ và alias vẫn resolve cùng ID.
4. Preview không ghi dữ liệu; apply chỉ sửa các dòng được chọn. Dòng đã thay đổi phải trả 409 và rollback lô.
5. Lookup typo trả FUZZY nếu đạt ngưỡng, nhưng input vẫn unmatched cho đến khi người dùng chọn ID.
6. Input gồm Salt và tên chưa biết, allowPartial=false: recipes rỗng và requiresResolution=true. Chỉ khi true mới tìm bằng Salt.
7. Confirm ID inactive/không tồn tại: 400. Confirm hợp lệ giữ ID, không resolve lại theo tên.
8. Catalog được recipe PUBLISHED sử dụng không thể tắt; catalog inactive không xuất hiện trong lookup hoặc candidates.
9. Cùng nguyên liệu lặp nhiều dòng trong recipe chỉ tính một loại trong coverage.

Không tạo file test trong source và không gọi thêm AI để sinh alias.

## Xác minh khi triển khai code

- Compile toàn project bằng Java 21 thành công.
- Chạy 11 kiểm tra logic trong bộ nhớ bằng JShell và repository giả: exact/partial/fuzzy, khử trùng candidate, loại inactive, limit, chặn unmatched, cho phép partial có cảnh báo, coverage không đếm trùng và không trả recipe khi chưa có catalog ID.
- Apply V39/V40 thành công trên PostgreSQL 17 tạm riêng, kiểm tra xung đột khi insert/update catalog và alias.
- Kiểm tra hai transaction ghi đồng thời cùng tên vào catalog/alias khác nhau: một bên bị chặn đúng bằng unique violation.
- Không chạy backfill hoặc migration trên Neon; API với tài khoản và dữ liệu thật vẫn cần kiểm tra theo các bước thủ công phía trên.
