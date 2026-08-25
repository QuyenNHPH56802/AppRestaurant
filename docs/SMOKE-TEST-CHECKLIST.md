# Smoke Test Checklist — 2026-08-25

> **Port**: Server chạy trên **port 8080** (mặc định).
> Chạy smoke test tự động: `python tools/smoke-tests.py`

## A. Chuẩn bị (10 phút trước demo)

- [ ] Server đang chạy trên laptop demo (`http://localhost:8080`)
- [ ] Android app đã cài và đăng nhập bằng `admin / admin123` (lần đầu)
- [ ] Đã in QR zones (4 QR: Bếp phở, Bún chả, Phục vụ, Kho) và dán ở khu vực tương ứng
- [ ] Đã scan QR server trên thiết bị để kết nối (`http://192.168.x.x:8080`)
- [ ] Data đã seed (7 users, 4 zones, 2 shifts, 25 foods, 4 checklists)

## B. Login & kết nối (5 phút)

- [ ] Login `admin / admin123` thành công → màn hình Home
- [ ] Home hiển thị tên nhà hàng + featured foods
- [ ] Chuyển ngôn ngữ VI → KO: tất cả labels dịch sang tiếng Hàn
- [ ] Logout thành công, login lại bằng `nhanvien01 / staff123`

## C. Quản lý ca làm việc (10 phút)

- [ ] Vào **Lịch ca của tôi** (`/api/me/shifts`): thấy ca sáng hôm nay + ngày mai
- [ ] (Admin) Vào **Admin > Shifts**: tạo ca mới "Ca đặc biệt 22:00-02:00" → thấy trong danh sách
- [ ] (Admin) Gán user `nhanvien06` vào ca mới → user thấy trong app
- [ ] (Admin) Sửa ca: đổi `start_time` thành 21:00 → cập nhật OK

## D. Quản lý khu vực (10 phút)

- [ ] (Admin) Vào **Admin > Zones**: thấy 4 zones (Bếp phở, Bún chả, Phục vụ, Kho)
- [ ] (Admin) Click **Regenerate QR** ở zone Bếp phở → token mới được tạo
- [ ] Mở URL `/api/zones/BEP_PHO/qr.png` trong trình duyệt → PNG hiển thị đúng
- [ ] (Staff) Vào **Khu vực của tôi**: thấy "Bếp phở" + tên zone tiếng Việt/Hàn
- [ ] (Staff) Bấm **Chuyển sang** zone khác → cập nhật thành công, hiển thị zone mới

## E. Check-in bằng QR (5 phút)

- [ ] (Staff) Mở app → bấm nút QR scanner → camera mở
- [ ] Quét QR zone "Bếp phở" đã in → app nhận diện thành công
- [ ] Chọn **Check-in** → server ghi log + gửi thông báo (nếu có FCM)
- [ ] (Admin) Vào **Admin > Check-ins**: thấy log check-in mới

## F. Checklist (10 phút)

- [ ] (Staff) Vào **Checklist** → chọn zone hiện tại
- [ ] Thấy 10 tasks: một số required (đỏ), một số optional (xám)
- [ ] Đánh dấu 5 tasks hoàn thành + 1 skip (optional) → lưu thành công
- [ ] (Admin) Vào **Admin > Checklists**: thấy completion stats

## G. Thông báo (5 phút)

- [ ] (Admin) Tạo notification mới trong **Admin > Notifications**: tiêu đề "Họp lúc 14h"
- [ ] Gửi tới role STAFF → tất cả staff nhận được
- [ ] (Staff) Mở app → thấy notification trong danh sách
- [ ] (Staff) Đánh dấu đã đọc → badge cập nhật

## H. Đa ngôn ngữ (5 phút)

- [ ] (Staff) Profile > Đổi ngôn ngữ sang 한국어
- [ ] Tất cả labels, foods, zones dịch sang Hàn
- [ ] Đổi lại tiếng Việt → restore

## I. Kiosk mode (chỉ thiết bị được setup) (5 phút)

- [ ] (Admin) Bật kiosk mode cho device → app chiếm toàn màn hình
- [ ] Bấm nút back 5 lần → thoát kiosk (admin auth)
- [ ] Disable kiosk mode → app trở về bình thường

## J. Backup & restore (5 phút)

- [ ] (Admin) **Admin > Backup** → bấm **Tạo backup** → file .zip tải về
- [ ] Tạo zone mới, sau đó **Restore** từ backup vừa tạo → zone mới biến mất, data về trạng thái cũ
- [ ] Verify: kiểm tra số zones, users, shifts trở về đúng như trước khi tạo zone mới

## K. Tổng kết (5 phút)

- [ ] Review metrics: tổng users, zones, foods, shifts
- [ ] Check audit log: đầy đủ entry cho mọi thao tác admin
- [ ] Q&A

---

## Tài khoản demo

| Username | Password | Role | Lang |
|---|---|---|---|
| `admin` | `admin123` | ADMIN | vi |
| `nhanvien01` | `staff123` | STAFF | vi |
| `nhanvien02` | `staff123` | STAFF | vi |
| `nhanvien03` | `staff123` | STAFF | vi |
| `nhanvien04` | `staff123` | STAFF | ko |
| `nhanvien05` | `staff123` | STAFF | vi |
| `nhanvien06` | `staff123` | STAFF | vi |

## URLs quan trọng

| Mục | URL |
|------|-----|
| Health | `http://localhost:8080/api/health` |
| Server info | `http://localhost:8080/api/server/info` |
| Server QR | `http://localhost:8080/api/server/qr.png` |
| Zone QR | `http://localhost:8080/api/zones/{CODE}/qr.png` (CODE: BEP_PHO, BUN_CHA, PHUC_VU, KHO) |
| Admin SPA | `http://localhost:8080/admin/` |
| Server Dashboard | `http://localhost:8080/admin/server/` |

## Auto smoke test

```bash
python tools/smoke-tests.py
# Expected: Total: 23 | PASS: 23 | FAIL: 0
```

## Pass criteria

All checkboxes + auto smoke test 23/23 must pass before delivery.