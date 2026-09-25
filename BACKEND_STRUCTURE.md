# Hướng dẫn cấu trúc backend

Tài liệu dành cho các thành viên phát triển backend của dự án **AI-driven recipe and ingredient marketplace for bakers**.

Backend sử dụng **Java 21, Spring Boot, PostgreSQL, Spring Data JPA và Flyway**. Kiến trúc định hướng là **Modular Monolith**: một ứng dụng triển khai chung, chia code theo nghiệp vụ để dễ phát triển và bổ sung AI.

> Đây là cấu trúc mục tiêu, không phải danh sách tính năng đã hoàn thành. Chỉ tạo package, class và bảng khi triển khai chức năng tương ứng. Quy trình Git và migration tiếp tục tuân theo [RULE.md](RULE.md).

## 1. Kiến trúc tổng thể

```text
Web Admin / Staff / Store        Mobile App
             │                     │
             └──────────┬──────────┘
                        ▼
               Spring Boot REST API
               Các module nghiệp vụ
                  │           │
                  ▼           ▼
             PostgreSQL   Object Storage / CDN
                          Ảnh và video

Module nghiệp vụ → Module tích hợp AI → AI API / Python service
                                      (bổ sung khi cần)
```

- Ban đầu dùng một Maven project và một PostgreSQL database.
- Chia package theo nghiệp vụ; các tầng controller/service/repository nằm bên trong từng module.
- Không tạo microservice, message broker hay vector database riêng chỉ để dự phòng.
- AI là thành phần hỗ trợ nghiệp vụ. Quyền truy cập, trạng thái xuất bản và quyết định nghiệp vụ vẫn được kiểm soát trong backend.

## 2. Cấu trúc thư mục

Package gốc: `com.manhnv.capstonebakerai`.

```text
src/
├── main/
│   ├── java/com/manhnv/capstonebakerai/
│   │   ├── CapstoneBakerAiApplication.java
│   │   ├── config/
│   │   ├── common/
│   │   │   └── exception/
│   │   ├── identity/
│   │   ├── recipe/
│   │   ├── ingredient/
│   │   ├── store/
│   │   ├── advertising/
│   │   ├── community/
│   │   ├── moderation/
│   │   ├── support/
│   │   ├── analytics/
│   │   ├── media/
│   │   └── ai/
│   └── resources/
│       ├── application.properties
│       ├── application-local.properties     # Tạo khi cần profile local
│       ├── application-prod.properties      # Tạo khi cần profile production
│       └── db/migration/
│           └── V1__create_users_table.sql
└── test/
    └── java/com/manhnv/capstonebakerai/
        └── ...                              # Theo package của code được test
```

Không gom mọi entity của hệ thống vào một package `entity`, hoặc mọi service vào một package `service` ở cấp gốc. Code liên quan cùng nghiệp vụ cần nằm gần nhau.

## 3. Trách nhiệm của từng module

| Module | Trách nhiệm | Dữ liệu tiêu biểu |
| --- | --- | --- |
| `identity` | Xác thực, tài khoản, vai trò, hồ sơ | User, Role, UserProfile |
| `recipe` | Công thức, các bước làm, danh mục, xuất bản | Recipe, RecipeStep, RecipeIngredient |
| `ingredient` | Danh mục nguyên liệu chuẩn, tên tương đương, đơn vị | Ingredient, IngredientAlias, Unit |
| `store` | Cửa hàng và sản phẩm nguyên liệu | Store, Product |
| `advertising` | Chiến dịch quảng cáo, hiển thị và click | Campaign, AdImpression, AdClick |
| `community` | Bình luận, bookmark, chia sẻ ảnh thành phẩm | Comment, Bookmark, CompletionPost |
| `moderation` | Report, kiểm duyệt, lịch sử quyết định | ContentReport, ModerationDecision |
| `support` | Ticket, hội thoại hỗ trợ, chuyển tiếp cho nhân viên | SupportTicket, SupportMessage |
| `analytics` | Thu thập hành vi cần thiết và thống kê | SearchEvent, RecipeViewEvent |
| `media` | Upload, metadata và trạng thái xử lý ảnh/video | MediaAsset |
| `ai` | Gọi model, quản lý prompt, chuyển đổi kết quả AI | AiJob khi có tác vụ dài |

Các tên entity trong bảng là ví dụ định hướng, chưa phải ERD đã chốt.

### Phân biệt nguyên liệu và sản phẩm

- `Ingredient`: nguyên liệu chuẩn, ví dụ “Bột mì đa dụng”.
- `RecipeIngredient`: công thức cần nguyên liệu nào, số lượng và đơn vị, ví dụ 250 g bột mì đa dụng.
- `Product`: mặt hàng cụ thể của cửa hàng, ví dụ bột mì nhãn X, gói 1 kg.

Việc tách này giúp tìm công thức theo nguyên liệu và ghép sản phẩm cửa hàng với nhu cầu của công thức. Không so khớp chỉ bằng tên sản phẩm tự do nếu đã có liên kết với nguyên liệu chuẩn.

### Phạm vi marketplace

Tài liệu hiện mô tả rõ quảng cáo và ghép sản phẩm với công thức. Chưa mặc định xây dựng `cart`, `order`, `payment`, `inventory`; bổ sung khi nhóm chốt nghiệp vụ mua bán trực tiếp.

## 4. Cấu trúc bên trong một module

Ví dụ module `recipe`:

```text
recipe/
├── controller/
│   └── RecipeController.java
├── service/
│   └── RecipeService.java
├── repository/
│   └── RecipeRepository.java
├── entity/
│   ├── Recipe.java
│   ├── RecipeStep.java
│   └── RecipeIngredient.java
├── dto/
│   ├── CreateRecipeRequest.java
│   ├── UpdateRecipeRequest.java
│   └── RecipeResponse.java
└── mapper/
    └── RecipeMapper.java
```

Chỉ tạo `mapper`, `exception`, `validation` hoặc package phụ khác khi có nhu cầu thực tế. Validation riêng của module đặt trong module đó.

| Thành phần | Nên làm | Không nên làm |
| --- | --- | --- |
| Controller | Nhận request, kích hoạt validation, gọi service, trả HTTP response | Chứa quy tắc nghiệp vụ hoặc gọi repository trực tiếp |
| Service | Xử lý use case, quyền sở hữu, transaction và phối hợp module | Phụ thuộc vào HTTP request/response để thực hiện nghiệp vụ |
| Repository | Truy vấn và lưu dữ liệu | Quyết định quyền truy cập hoặc trạng thái nghiệp vụ |
| Entity | Mô hình lưu trữ và quan hệ JPA | Được trả trực tiếp làm response API |
| DTO | Mô tả dữ liệu đầu vào/đầu ra của API | Cho client tự gán quyền, chủ sở hữu hay trạng thái kiểm duyệt |
| Mapper | Chuyển entity sang DTO và ngược lại trong phạm vi cho phép | Gọi database hoặc thực hiện nghiệp vụ |

Luồng thông thường:

```text
Request → Controller → Service → Repository → PostgreSQL
                          │
                          └── Mapper → Response DTO
```

Dùng constructor injection. Không bắt buộc tạo cặp `Service`/`ServiceImpl` khi chỉ có một implementation. Repository tiếp tục dùng interface của Spring Data JPA.

## 5. Giao tiếp giữa các module

1. Module sở hữu dữ liệu chịu trách nhiệm thay đổi dữ liệu đó.
2. Module khác gọi service công khai của module sở hữu, không truy cập repository của nó trực tiếp.
3. Ưu tiên trao đổi ID và DTO nhỏ; hạn chế truyền JPA entity qua ranh giới module.
4. Tránh phụ thuộc vòng. Nếu hai module gọi ngược nhau, xem lại nơi điều phối use case hoặc tách một truy vấn phù hợp.
5. Chỉ đưa vào `common` những thành phần kỹ thuật thực sự dùng chung. Không đặt `RecipeService`, `UserService` hay quy tắc kiểm duyệt trong `common`.

Ví dụ tạo công thức:

```text
RecipeController
    → RecipeService
        → Kiểm tra người dùng và quyền tạo công thức
        → IngredientService: kiểm tra nguyên liệu hợp lệ
        → MediaService: kiểm tra quyền dùng file đã upload
        → RecipeRepository: lưu công thức và các bước
```

Chưa cần tạo event cho mọi thao tác. Với xử lý nền quan trọng, event chỉ nằm trong bộ nhớ không thay thế được job lưu bền vững.

## 6. API, phân quyền và xử lý lỗi

### API

- Thống nhất tiền tố `/api/v1` cho API mới.
- Đặt đường dẫn theo tài nguyên: `/api/v1/recipes`, `/api/v1/ingredients`.
- API danh sách cần phân trang; thống nhất giới hạn kích thước trang và trường sort được hỗ trợ.
- Dùng request/response DTO riêng khi nội dung khác nhau.
- Validate dữ liệu đầu vào ở biên API; kiểm tra điều kiện nghiệp vụ trong service.

Ví dụ endpoint định hướng:

```text
GET    /api/v1/recipes
GET    /api/v1/recipes/{id}
POST   /api/v1/recipes
PATCH  /api/v1/recipes/{id}
POST   /api/v1/recipes/{id}/reports
```

### Phân quyền

- Spring Security kiểm soát xác thực và quyền truy cập endpoint khi được triển khai.
- Service kiểm tra quyền trên từng tài nguyên: Baker chỉ sửa công thức của mình; Store chỉ sửa sản phẩm của mình.
- Lấy danh tính người đang thao tác từ phiên/token đã xác thực, không tin `userId` hoặc `storeId` do client gửi để xác định chủ sở hữu.
- Kiểm tra quyền trên job AI, file và hội thoại tương tự các tài nguyên khác.

### Lỗi

Đặt `GlobalExceptionHandler` tại `common/exception`, sử dụng cơ chế xử lý lỗi tập trung của Spring. Thống nhất response lỗi có mã lỗi ổn định, thông báo và chi tiết validation khi cần.

- `400`: dữ liệu đầu vào không hợp lệ.
- `401`: chưa xác thực hoặc thông tin xác thực không hợp lệ.
- `403`: không đủ quyền.
- `404`: tài nguyên không tồn tại hoặc không được phép tiết lộ sự tồn tại.
- `409`: xung đột trạng thái hoặc ràng buộc nghiệp vụ.
- `500`: lỗi ngoài dự kiến; không trả stack trace, SQL hay secret cho client.

## 7. PostgreSQL và migration

- Flyway là nguồn quản lý thay đổi schema. Repository hiện đã có `V1__create_users_table.sql`; phải kiểm tra migration hiện hữu trước khi đặt version tiếp theo.
- Giữ `spring.jpa.hibernate.ddl-auto=validate`; không dùng Hibernate tự cập nhật schema ở môi trường triển khai.
- Theo [RULE.md](RULE.md), không sửa/xóa migration đã merge; tạo migration mới để thay đổi hoặc sửa lỗi.
- Đặt tên `V<number>__<description>.sql`; phối hợp version giữa các nhánh để tránh trùng.
- Dùng foreign key, unique constraint và check constraint phù hợp, không chỉ dựa vào kiểm tra trong Java.
- Dùng bảng quan hệ cho dữ liệu lõi. JSONB phù hợp với metadata linh hoạt hoặc kết quả AI, không thay thế toàn bộ schema.
- Thêm index theo truy vấn thực tế, đặc biệt các cột lọc, join và sắp xếp thường dùng.
- Đặt transaction ở service cho một nghiệp vụ cần tính nguyên tử. Không giữ transaction mở trong lúc chờ upload, gọi AI hoặc API bên ngoài.

Đối với công thức, thứ tự các bước, số lượng nguyên liệu và đơn vị cần được lưu rõ ràng. Không mặc định đổi thể tích sang khối lượng nếu chưa có quy tắc theo từng nguyên liệu.

## 8. Ảnh và video

- Lưu file trên Cloudflare R2; database chỉ lưu object key, loại file, dung lượng, chủ sở hữu và trạng thái xử lý.
- Kiểm tra kích thước, loại nội dung và quyền sử dụng file.
- Không lưu toàn bộ video dưới dạng binary trong bảng công thức.
- Video dài cần phương án phân phối như CDN/streaming và đo kiểm thực tế để đáp ứng yêu cầu buffering.
- Job tóm tắt video chỉ được chạy với file người dùng có quyền truy cập.

### Cloudflare R2

Hệ thống dùng hai bucket với chung một R2 account, API token, `S3Client` và `S3Presigner`:

```text
baker-public
├── avatars/
├── recipes/
├── products/
└── completion-posts/

baker-private
├── recipe-drafts/
├── moderation-reports/
└── support-attachments/
```
- Trên đây chỉ là ví dụ, chỉnh sửa thêm về sau khi cần

- Bucket public chứa nội dung mà bất kỳ người nào biết URL đều được phép xem. Development có thể dùng `r2.dev`; production dùng custom domain để có cache và các tính năng bảo vệ của Cloudflare.
- Bucket private không bật public access. Backend kiểm tra quyền và tạo presigned GET URL có thời hạn khi người dùng cần xem file.
- Prefix chỉ giúp tổ chức object key, không tạo ranh giới bảo mật. Không đặt file riêng tư dưới prefix `private/` của bucket public.
- Một R2 API token có quyền `Object Read & Write` được giới hạn vào đúng hai bucket trên.
- Backend tự tạo object key; không dùng trực tiếp tên file hoặc object key do client tự quyết định.
- Không lưu presigned URL trong database vì URL sẽ hết hạn. Chỉ lưu object key, ví dụ `recipes/42/images/<uuid>.png`.

Cấu hình môi trường:

```properties
R2_ACCOUNT_ID=
R2_ACCESS_KEY_ID=
R2_SECRET_ACCESS_KEY=
R2_PUBLIC_BUCKET=baker-public
R2_PRIVATE_BUCKET=baker-private
R2_PUBLIC_URL=https://media.example.com
```

`R2_PUBLIC_URL` có thể để trống nếu chưa cấu hình public URL. Các secret chỉ nằm trong `.env` hoặc secret manager của môi trường triển khai; `.env.sample` không chứa giá trị thật.

Khi gọi `R2StorageService`, feature phải chỉ rõ loại bucket:

```java
r2StorageService.upload(
        R2BucketType.PUBLIC,
        objectKey,
        content,
        contentType
);

URL privateUrl = r2StorageService.createDownloadUrl(
        R2BucketType.PRIVATE,
        objectKey
);
```

Các thao tác được cung cấp gồm upload, delete, kiểm tra object tồn tại, tạo presigned upload/download URL và tạo URL public. Feature sở hữu nghiệp vụ phải quyết định bucket phù hợp và kiểm tra quyền trước khi gọi storage service.

Free tier R2 được tính tổng trên account, không tính riêng cho từng bucket. Theo bảng giá tại thời điểm cập nhật tài liệu, Standard Storage có 10 GB-month, 1 triệu Class A operations và 10 triệu Class B operations miễn phí mỗi tháng; egress ra Internet miễn phí. Hạn mức này có thể thay đổi, vì vậy cần kiểm tra [Cloudflare R2 Pricing](https://developers.cloudflare.com/r2/pricing/) trước khi ước tính chi phí. Free tier không áp dụng cho Infrequent Access.

Tài liệu vận hành tham khảo:

- [R2 Authentication](https://developers.cloudflare.com/r2/api/tokens/)
- [R2 public buckets](https://developers.cloudflare.com/r2/buckets/public-buckets/)
- [R2 presigned URLs](https://developers.cloudflare.com/r2/api/s3/presigned-urls/)
- [R2 CORS](https://developers.cloudflare.com/r2/buckets/cors/)

## 9. Tích hợp AI theo từng giai đoạn

### Ranh giới trách nhiệm

Module `ai` xử lý việc gọi model, prompt, timeout và chuẩn hóa kết quả. Module nghiệp vụ kiểm tra kết quả và quyết định lưu/xuất bản/thay đổi trạng thái.

| Tính năng | Cách phối hợp |
| --- | --- |
| Gợi ý công thức | Dùng dữ liệu recipe/ingredient và hành vi; có gợi ý thông thường khi AI lỗi |
| Nhận diện nguyên liệu | Trả danh sách ứng viên để người dùng xác nhận |
| Kiểm duyệt | AI gắn cờ; moderation lưu kết quả và cho QA/Admin xem xét |
| Dự báo nhu cầu | Chạy định kỳ trên dữ liệu tổng hợp từ analytics |
| Chatbot | Dùng tài liệu hỗ trợ; chuyển ticket cho nhân viên khi cần |
| Tóm tắt video | Chạy nền và trả bản nháp để tác giả duyệt |
| Xác minh thành phẩm | Trả đánh giá; quy tắc ghi nhận hoàn thành/phần thưởng thuộc module nghiệp vụ |

Cấu trúc tham khảo khi bắt đầu làm AI:

```text
ai/
├── recommendation/
├── recognition/
├── summarization/
├── client/          # Tích hợp nhà cung cấp AI hoặc Python service
└── job/             # Tác vụ dài, tạo khi cần
```

Không cần tạo toàn bộ cây này ngay. Nếu chỉ gọi AI API, có thể gọi từ Spring Boot. Python service chỉ cần khi chạy model riêng hoặc phụ thuộc thư viện Python.

### Tác vụ dài

```text
Client gửi yêu cầu
    → Backend lưu job và trả 202 + jobId
    → Worker nhận job
    → Gọi AI ngoài transaction dài
    → Lưu kết quả và trạng thái
    → Client đọc trạng thái/kết quả bằng jobId
```

- Lưu job bền vững trong PostgreSQL với các trạng thái như `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED`.
- Khi có worker, cần cơ chế nhận job nguyên tử, giới hạn retry và xử lý job bị kẹt sau khi tiến trình dừng.
- Việc ghi kết quả phải tránh tạo bản ghi hoặc trao thưởng lặp khi job được chạy lại.
- Đặt timeout, giới hạn tài nguyên/chi phí và lưu thông tin model cần cho việc truy vết. Không log secret hoặc toàn bộ dữ liệu cá nhân trong prompt.
- Xem nội dung người dùng/tài liệu truy xuất là dữ liệu không đáng tin cậy. AI không được bỏ qua kiểm tra quyền hay tự thực hiện thao tác ghi dữ liệu tùy ý.

### Dữ liệu cho AI

Thu thập những sự kiện thực sự cần cho tính năng đã chọn, ví dụ tìm kiếm, xem/lưu công thức hoặc click quảng cáo. Xác định thời gian lưu và hạn chế dữ liệu cá nhân.

Chỉ thêm embedding/pgvector khi cần tìm kiếm ngữ nghĩa hoặc chatbot tra cứu tài liệu. Spring AI là một lựa chọn tích hợp; kiểm tra tương thích với phiên bản Spring Boot trong `pom.xml` trước khi thêm dependency.

## 10. Cấu hình và secret

Repository hiện đọc `.env` qua `spring.config.import`. PostgreSQL dùng `NEON_HOSTNAME`, `NEON_DBNAME`, `NEON_USER`, `NEON_PASSWORD`; Cloudflare R2 dùng account ID, access key, secret key, hai bucket và public URL được mô tả ở phần 8.

- Dùng `.env.sample` làm mẫu; không commit `.env` hoặc thông tin truy cập thật.
- API key AI, khóa storage và secret xác thực phải lấy từ cấu hình môi trường.
- `application-*.properties` chỉ chứa cấu hình không nhạy cảm hoặc tham chiếu biến môi trường.
- Tách cấu hình local/test/production khi phát sinh khác biệt thực tế.
- Khi triển khai, rà soát cấu hình log SQL và log request để tránh lộ dữ liệu nhạy cảm.

## 11. Quy trình thêm một tính năng

1. Xác định module sở hữu nghiệp vụ và đọc code/migration hiện có.
2. Thống nhất hợp đồng API, validation, quyền truy cập và trường hợp lỗi.
3. Thêm migration nếu cần thay đổi schema; phối hợp version với nhóm.
4. Triển khai entity/repository, service, DTO/mapper và controller cần thiết.
5. Kiểm tra luồng thành công và rủi ro chính: thiếu quyền, dữ liệu sai, xung đột hoặc thao tác lặp.
6. Chạy kiểm tra phù hợp rồi tạo PR theo `RULE.md`.

Trên Windows, có thể chạy bộ test bằng Maven Wrapper:

```powershell
.\mvnw.cmd test
```

Test tải Spring context hoặc kết nối database cần cấu hình môi trường test tương ứng. Không chạy test có ghi dữ liệu trên database production. Với tích hợp AI, dùng kết quả giả lập trong test thông thường để tránh chi phí và kết quả không ổn định.

Checklist review:

- Code nằm đúng module và không truy cập repository xuyên module?
- Có kiểm tra role, chủ sở hữu và dữ liệu đầu vào?
- API dùng DTO, không lộ entity hoặc trường nhạy cảm?
- Migration có tương thích với dữ liệu hiện hữu?
- Transaction có tránh gọi dịch vụ bên ngoài quá lâu?
- Có kiểm tra hành vi quan trọng và xử lý lỗi phù hợp?
- Có thêm abstraction/dependency chưa cần thiết không?

## 12. Áp dụng vào repository hiện tại

Tại thời điểm viết tài liệu, repository chủ yếu là phần khởi tạo: đã có JPA, Flyway, PostgreSQL, MapStruct; `User.java` đang comment và `GlobalExceptionHandler` còn rỗng.

Thứ tự triển khai đề xuất:

1. Tổ chức tài khoản vào `identity`, lỗi chung vào `common/exception`; hoàn thiện xác thực, phân quyền và validation.
2. Xây dựng `ingredient`, `recipe`, `media` để có luồng tạo và xem công thức hoàn chỉnh.
3. Bổ sung store/advertising, community, moderation và support theo phạm vi đã chốt.
4. Thu thập dữ liệu cần thiết và triển khai từng tính năng AI trên các luồng nghiệp vụ đã ổn định.

Tài liệu này không tự thay đổi package hoặc triển khai các chức năng trên. Khi nhóm thay đổi ranh giới module hoặc quy ước, cập nhật tài liệu trong cùng PR.

## 13. Login, refresh và logout

- Login trả access token trong `ApiResponse.data`; refresh token nằm trong cookie `refresh_token` có `HttpOnly` và path `/api/v1/auth`.
- Access token có TTL 15 phút; refresh token có TTL 30 ngày tính từ mỗi lần phát hành. Khi triển khai HTTPS, đặt `REFRESH_COOKIE_SECURE=true`. Chỉ dùng `SameSite=None` khi cần gửi cookie cross-site và luôn đi kèm `Secure=true`.
- Refresh khóa token trong database, kiểm tra hạn sử dụng và trạng thái user, revoke token cũ rồi tạo token mới trong cùng transaction. `replaced_by_id` liên kết token cũ với token mới.
- Logout khóa và lần theo chuỗi token thay thế để revoke cả token mới nếu refresh vừa hoàn tất. Các phiên đăng nhập khác không bị revoke. Token không tồn tại hoặc đã logout vẫn trả success để thao tác có thể gọi lặp lại.
- JWT access token đã phát vẫn có hiệu lực đến khi hết hạn. Client phải xóa access token khi logout và không cập nhật lại trạng thái đăng nhập từ response refresh đến muộn. Chỉ cho một refresh chạy tại một thời điểm; chặn refresh mới và chờ refresh đang chạy hoàn tất trước khi gọi logout.
- Login/refresh/logout không kiểm tra Bearer token gửi kèm vì chúng xác thực bằng mật khẩu hoặc refresh cookie. Những API khác vẫn xác thực JWT. Lỗi xác thực/phân quyền của Security dùng `common/exception/SecurityExceptionHandler` và trả `ApiResponse` giống lỗi nghiệp vụ.

### Header `X-Auth-Request: 1`

Hai API POST `/api/v1/auth/refresh` và `/api/v1/auth/logout` bắt buộc header này. `X-Auth-Request` là tên do project quy định; `1` là giá trị cố định, không phải secret hay JWT.

Cookie có thể được trình duyệt tự gửi kèm request. Vì vậy API dùng cookie cần chống CSRF: trang web khác không được lợi dụng trình duyệt của người dùng để gọi API ngoài ý muốn. Form HTML thông thường không đặt được custom header; JavaScript gọi cross-origin với header này phải qua CORS preflight. Backend chỉ cho phép `FRONTEND_ORIGIN` đã cấu hình. Không mở rộng CORS cho origin không tin cậy.

Giữ header này khi CSRF mặc định của Spring đang tắt. Có thể thay bằng cơ chế CSRF chuẩn khác, nhưng không chỉ xóa kiểm tra header. `HttpOnly` ngăn JavaScript đọc cookie, không ngăn trình duyệt gửi cookie; `Secure` chỉ giới hạn cookie qua HTTPS.

```javascript
const response = await fetch(`${API_URL}/api/v1/auth/refresh`, {
  method: "POST",
  credentials: "include",
  headers: { "X-Auth-Request": "1" }
});
```

Logout dùng cùng cấu hình request, đổi URL thành `/api/v1/auth/logout`. Không cần request body hoặc Authorization cho hai API này. Postman cần thêm header và lưu cookie từ response login.

### Migration và kiểm thử

`V11__link_refresh_token_rotation.sql` thêm `replaced_by_id` và khóa ngoại tự tham chiếu. Không sửa các migration đã áp dụng. Nếu database của team có V9, phải lấy đúng file V9 của team về trước khi chạy Flyway; không dùng repair để bỏ qua file bị thiếu.

Các token hiện hữu vẫn dùng được; liên kết chỉ được ghi cho những lần refresh sau khi triển khai bản mới. Không thể suy ra chuỗi của các lần refresh trước đó. Khi triển khai, dừng instance bản cũ để tránh nó tiếp tục rotate mà không ghi liên kết.

Chưa có job dọn token. Nếu bổ sung sau này, không xóa riêng một mắt xích trong chuỗi còn hoạt động: logout bằng token cũ cần chuỗi đó để tìm token hiện hành. Chính sách giới hạn thời gian tối đa của phiên và giới hạn tần suất login cần được chốt riêng.

Test tập trung: `AuthenticationServiceTest` kiểm tra rotation, reuse, logout theo chuỗi và các điều kiện từ chối; `AuthSecurityTest` kiểm tra filter Security thật bằng MockMvc, cookie, header và CORS. Các test này không kết nối Neon; kiểm thử tranh chấp transaction thực tế cần PostgreSQL test riêng.

## Tài liệu tham khảo

- [Spring Modulith — tổ chức module trong Spring Boot](https://docs.spring.io/spring-modulith/reference/fundamentals.html)
- [Spring AI](https://docs.spring.io/spring-ai/reference/api/)
- [Spring AI với PGvector](https://docs.spring.io/spring-ai/reference/api/vectordbs/pgvector.html)
