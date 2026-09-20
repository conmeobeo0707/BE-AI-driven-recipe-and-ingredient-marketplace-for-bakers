# RULE.md — Quy tắc làm việc nhóm

## Các quy tắc vàng

| #  | Quy tắc                                                                                  |
|:---|:-----------------------------------------------------------------------------------------|
| 1  | Không push trực tiếp lên `master` và `dev`                                               |
| 2  | Mọi thay đổi qua Pull Request                                                            |
| 3  | Tạo nhánh `feature/*` từ `dev`, merge lại vào `dev`                                      |
| 4  | Chỉ merge `dev` → `master` khi deploy, không merge lẻ tẻ                                 |
| 5  | Đặt tên nhánh theo format `feature/`, `bugfix/`, `docs/`, `refactor/`, `test/`, `db/`... |
| 6  | Commit message theo Conventional Commits                                                 |
| 7  | Không sửa/xóa migration đã merge — luôn tạo file mới                                     |
| 8  | Dùng number cho version migration để tránh trùng                                         |
| 9  | Không commit file `.env` — chỉ commit `.env.sample`                                      |
| 10 | `git pull` code mới nhất trước khi bắt đầu làm                                           |
| 11 | Giao tiếp rõ ràng khi có thay đổi database                                               |
| 12 | Được dùng AI hỗ trợ, nhưng **phải hiểu code mình viết**                                  |

## Commit message format

`<loại>(<phạm vi>): <mô tả ngắn>`

Các loại: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `db`

Ví dụ:
- `feat(user): add login API`
- `fix(product): handle null price`
- `db: create products table`

## Migration naming

`V<number-version>__<mô-tả>.sql`

Ví dụ: `V1__create_users_table.sql`