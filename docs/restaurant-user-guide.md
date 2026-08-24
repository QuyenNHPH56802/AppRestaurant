# Restaurant LAN System — User & Admin Guide

> Phiên bản: **1.0.0** — Tài liệu đi kèm `RestaurantServerSetup.exe` và `RestaurantStaff.apk`.

Hệ thống gồm **hai phần mềm** chạy trên cùng mạng Wi-Fi/LAN:

1. **Restaurant Server** — chạy trên máy tính thu ngân / quầy, lưu toàn bộ dữ liệu (món ăn, danh mục, nhân viên, đơn hàng). Đóng gói dưới dạng `RestaurantServerSetup.exe` (đã bao gồm Java JRE).
2. **Restaurant Staff** — ứng dụng Android cài cho từng nhân viên. Kết nối tới Server qua mạng nội bộ. Đóng gói dưới dạng `RestaurantStaff.apk`.

---

## 1. Cài đặt Server (Windows)

### Yêu cầu
- Windows 10/11 (64-bit)
- Quyền Administrator (để mở firewall cho phép máy trong mạng LAN kết nối tới port 8080)
- Ít nhất 500 MB dung lượng trống

### Bước thực hiện
1. Chạy `RestaurantServerSetup.exe`
2. Chọn đường dẫn cài đặt (mặc định `C:\Program Files\Restaurant\RestaurantServer`)
3. Chọn **Install** — chương trình sẽ:
   - Tạo shortcut trên Desktop trỏ tới Dashboard
   - Mở Windows Firewall cho phép kết nối vào port 8080 (chỉ trên mạng Private/Wi-Fi)
4. **Khởi động Server**: chạy `RestaurantServer.exe` từ Start Menu hoặc từ thư mục cài đặt.

### Dữ liệu
Mọi dữ liệu người dùng nằm ở thư mục:
```
%USERPROFILE%\RestaurantServer\
├── data\         # restaurant.db (SQLite database)
├── uploads\      # ảnh món ăn
├── backups\      # file backup tự động (mỗi ngày)
├── logs\         # server.log
└── config\       # server.properties (chỉnh sửa để thay đổi JWT, port, v.v.)
```

### Mở Dashboard
Trong trình duyệt, truy cập:
- **Dashboard quản lý món ăn + nhân viên**: <http://localhost:8080/admin/app/>
- **Dashboard server (IP, QR, backup)**: <http://localhost:8080/admin/server/>
- **Từ thiết bị khác trong LAN**: thay `localhost` bằng IP LAN (xem ở dashboard server). Ví dụ: `http://192.168.1.10:8080/admin/server/`.

### Đăng nhập mặc định
- Admin: `admin / admin123`  ← **đổi ngay sau khi cài đặt**
- Nhân viên: `nhanvien01 / staff123`

---

## 2. Cài đặt ứng dụng Android (Restaurant Staff)

### Yêu cầu
- Android 8.0 (API 26) trở lên
- Cùng mạng Wi-Fi/LAN với Server

### Bước thực hiện
1. Copy `RestaurantStaff.apk` sang điện thoại (qua USB, email, hoặc link download).
2. Trong **Settings → Security → Install unknown apps**, cho phép trình cài đặt được dùng.
3. Mở file `.apk`, bấm **Install**.
4. Mở ứng dụng **Restaurant Staff**.

### Quyền thông báo (Android 13 trở lên)
Lần đầu mở app, hệ thống sẽ hỏi **Cho phép gửi thông báo**. Bấm **Cho phép** để nhận push về ca làm, đổi khu vực và các thông báo quan trọng từ quản lý. Nếu từ chối, app vẫn hoạt động bình thường nhưng sẽ không rung/kêu khi có push — bạn vẫn xem được thông báo trong màn hình **🔔 Thông báo** của app.

Nếu đã từ chối trước đó và muốn bật lại: **Settings điện thoại → Apps → Restaurant Staff → Notifications → bật**.

### Ghép nối với Server (Pairing)
Khi mở lần đầu, ứng dụng yêu cầu ghép nối với Server. Hai cách:

- **Quét QR**: bấm **Quét mã QR** → camera sẽ mở → quét mã QR hiển thị trên **Server Dashboard** (`/admin/server/`).
- **Nhập thủ công**: bấm **Nhập thủ công** → nhập `IP:Port` của server (ví dụ `192.168.1.10:8080`).

Sau khi ghép nối thành công, ứng dụng chuyển sang màn hình **Đăng nhập**.

### Đăng nhập
Nhập tài khoản được cấp. Sau 5 lần đăng nhập sai trong 15 phút, tài khoản bị khóa tạm thời.

### Sử dụng hằng ngày
- **Home**: món nổi bật, món phổ biến, thông tin cửa hàng.
- **Menu**: danh sách toàn bộ món, lọc theo danh mục, tìm kiếm.
- **Chi tiết món**: hình ảnh, giá, mô tả (vi + ko).
- **Profile**: thông tin cá nhân, đăng xuất.
- **Settings**: đổi ngôn ngữ (Tiếng Việt ↔ Tiếng Hàn), xem thông tin server, ghép nối lại.

### Ca làm việc (Shifts)
- Mở **Settings → Ca làm việc** để xem các ca được phân công.
- Mỗi ca hiển thị: ngày, giờ bắt đầu / kết thúc, ca mẫu.
- Nhấn **Đồng ý** / **Từ chối** / **Yêu cầu đổi** để phản hồi. Quản lý sẽ nhận được thông báo ngay khi bạn phản hồi.
- Ca đã **Đồng ý** không thể chuyển trạng thái nữa — nếu cần đổi, liên hệ quản lý.

### Khu vực làm việc (Zones)
- Mở **Settings → Khu vực** để xem danh sách khu vực trong nhà hàng.
- Khu vực hiện tại của bạn được đánh dấu **Hiện tại**.
- Nhấn **Chọn khu vực này** để tự chuyển sang khu vực khác (chỉ áp dụng cho chính bạn).
- Xem lịch sử khu vực ở cuối trang.

### Checklist công việc
- Mở **Settings → Checklist** để xem các checklist đang hoạt động (theo ca / khu vực bạn phụ trách).
- Nhấn **Hoàn thành** cho mỗi mục khi xong; một số mục **bắt buộc** không thể bỏ qua.
- Mục **tuỳ chọn** có thể nhấn **Bỏ qua** nếu không áp dụng.
- Trạng thái đã hoàn thành / bỏ qua hiển thị ngay trên thẻ.

### Check-in / Check-out
- Mở **Settings → Check-in** để ghi nhận bạn đang làm việc tại khu vực nào.
- Chọn khu vực, nhấn **Vào ca** / **Tan ca**.
- Hệ thống ghi nhận lịch sử check-in/out ở cuối trang để bạn kiểm tra lại.
- Không thể **Vào ca** hai lần liên tiếp — phải **Tan ca** trước.

### Thông báo đẩy (Push notifications)
- Khi quản lý phân công ca mới, đổi khu vực, hoặc gửi thông báo quan trọng, bạn sẽ nhận thông báo đẩy trên điện thoại.
- Bấm vào thông báo để mở thẳng màn hình liên quan (Ca làm / Khu vực / Checklist).
- Bạn có thể bật / tắt thông báo đẩy trong **Settings → Thông báo**.

### Màn hình Thông báo (in-app feed)

Mở tab **🔔 Thông báo** ở thanh dưới cùng của ứng dụng để xem danh sách tất cả thông báo (cả đã đọc lẫn chưa đọc).

**Các tính năng chính:**

| Tính năng | Cách dùng |
|-----------|-----------|
| **Bộ lọc** | Chạm vào chip **Tất cả** hoặc **Chưa đọc** ở đầu trang để lọc danh sách. |
| **Đánh dấu đã đọc** | Chạm vào thẻ thông báo — nó sẽ chuyển sang màu xám và badge trên tab giảm 1. |
| **Đánh dấu tất cả đã đọc** | Chạm vào nút **Đánh dấu tất cả đã đọc** trên thanh trên cùng. |
| **Kéo để làm mới** | Kéo danh sách xuống từ trên — danh sách sẽ tải lại từ đầu. |
| **Tải thêm** | Cuộn xuống cuối danh sách → chạm nút **Tải thêm…** để xem các thông báo cũ hơn. |
| **Xem lịch sử gửi** | Chạm vào biểu tượng 🕒 trên mỗi thẻ để xem chi tiết các lần server đã gửi thông báo (gửi thành công, thử lại, lỗi). |
| **Phản hồi ca** | Với thông báo phân ca (`SHIFT_ASSIGNED`), chạm **Nhận ca** / **Từ chối** ngay trên thẻ. Quản lý sẽ nhận được phản hồi ngay. |

**Trạng thái phản hồi:** Sau khi chạm **Nhận ca** / **Từ chối**, nút sẽ chuyển sang trạng thái **Đã nhận** / **Đã từ chối** và không thể thay đổi từ app. Liên hệ quản lý nếu cần đổi.

**Phím tắt màn hình chính:** Nhấn giữ biểu tượng app **Restaurant Staff** trên màn hình chính → chọn **Mở thông báo** để vào thẳng màn hình này.

---

## 3. Quản lý món ăn & danh mục (Admin)

Truy cập <http://localhost:8080/admin/app/> đăng nhập với tài khoản **ADMIN**.

### Tổng quan
Hiển thị nhanh:
- Tổng số món
- Món đang bán
- Hết hàng / đã ẩn
- Số danh mục, số nhân viên

### Danh mục (Categories)
- Thêm danh mục mới: nhập tên (VI + KO) → Lưu
- Ẩn/hiện danh mục: bấm biểu tượng con mắt
- Sửa tên danh mục: bấm biểu tượng bút
- Danh mục bị ẩn sẽ không hiện trên ứng dụng nhân viên

### Món ăn (Foods)
- Thêm mới: tên (VI + KO), mô tả, giá, danh mục, upload ảnh
- Đổi trạng thái:
  - `AVAILABLE` (Đang bán) — hiện trên app
  - `SOLD_OUT` (Hết hàng) — vẫn hiện nhưng báo hết
  - `HIDDEN` (Ẩn) — không hiện
- Bật/tắt **Nổi bật** (featured) — sẽ hiện trong màn hình Home

### Nhân viên (Users)
- Thêm nhân viên: username, password, role (STAFF / ADMIN), họ tên
- Reset mật khẩu
- Vô hiệu hóa (DISABLED) — không thể đăng nhập

### Cửa hàng (Store)
- Tên cửa hàng (VI + KO)
- Địa chỉ
- Số điện thoại
- Giờ mở cửa
- Upload logo (tùy chọn)

---

## 3b. Quản lý ca làm việc & khu vực (Admin)

### Ca làm việc (Shifts)
- Truy cập **Dashboard quản trị → Ca làm việc**.
- Tạo ca mới với: tên ca, giờ bắt đầu, giờ kết thúc, vai trò.
- Phân công nhân viên cho từng ca trong tuần / ngày cụ thể.
- Nhân viên sẽ nhận thông báo đẩy ngay khi được phân công. Theo dõi trạng thái phản hồi (Đồng ý / Từ chối) theo thời gian thực.

### Khu vực (Zones)
- Truy cập **Dashboard quản trị → Khu vực**.
- Tạo khu vực với: tên (vi + ko), mô tả, màu hiển thị, thứ tự sắp xếp.
- Phân công nhân viên cho khu vực thông qua tab **Phân công khu vực**; nhân viên cũng có thể tự chọn khu vực trong app.
- Nhân viên được chuyển khu vực sẽ nhận thông báo đẩy ngay.

### Checklist
- Truy cập **Dashboard quản trị → Checklist**.
- Tạo checklist mới (ví dụ: "Mở cửa", "Đóng cửa") với các mục (tasks) bên trong.
- Mỗi mục có thể đánh dấu **bắt buộc** (nhân viên không thể bỏ qua) hoặc **tuỳ chọn**.
- Theo dõi tiến độ hoàn thành checklist theo nhân viên / ca / khu vực.

### Check-in / Check-out
- Truy cập **Dashboard quản trị → Check-in log** để xem lịch sử vào / tan ca của tất cả nhân viên.
- Lọc theo khu vực, ngày, nhân viên.
- Xuất CSV để chấm công cuối kỳ.

### Activity log
- Mọi hành động quan trọng (tạo / sửa / xoá ca, khu vực, checklist, phân quyền, check-in/out) đều được ghi lại trong **Activity log**.
- Truy cập **Dashboard quản trị → Activity log** để tra cứu, lọc theo người dùng / loại hành động / khoảng thời gian.

### Thông báo đẩy (Push)
- Server tự động gửi thông báo đẩy qua FCM khi:
  - Phân công ca mới, thay đổi ca, huỷ ca.
  - Nhân viên phản hồi ca (đồng ý / từ chối / yêu cầu đổi).
  - Chuyển khu vực làm việc.
- Tất cả thông báo đều **idempotent** — nếu server khởi động lại hoặc retry, không có thông báo trùng lặp được gửi tới nhân viên.
- Kiểm tra trạng thái gửi trong **Activity log** hoặc bảng `notification_events`.

### Thông báo (Notifications)
- Truy cập **Dashboard quản trị → Thông báo** để xem tất cả thông báo trong hệ thống (của mọi nhân viên).
- Lọc theo **loại thông báo** (SHIFT_ASSIGNED, ZONE_CHANGED, …) hoặc **theo nhân viên**.
- Chạm **Xem lịch sử gửi** trên một hàng để mở dialog liệt kê từng lần server gửi/lỗi/thử lại. Hữu ích khi nhân viên báo "không nhận được push".

### Thiết bị (Devices)
- Truy cập **Dashboard quản trị → Thiết bị** để xem các điện thoại đã đăng ký FCM token với hệ thống.
- Mỗi dòng hiển thị: tên nhân viên, loại thiết bị (Android / iOS), mã thiết bị, phiên bản app, lần cuối online, **đang hoạt động** hay đã ngưng.
- **Lưu ý bảo mật:** màn hình này **không bao giờ** hiển thị nguyên FCM token — chỉ hiển thị 6 ký tự đầu (`tokenPreview`) để bạn nhận biết token khi debug. Token đầy đủ chỉ FCM của Google biết.
- Đếm nhanh: phần đầu trang hiển thị **top 6 nhân viên có nhiều thiết bị active nhất** (hữu ích để phát hiện tài khoản bị nhiều người dùng chung).

---

## 4. Sao lưu & Phục hồi

### Sao lưu thủ công
Vào **Server Dashboard** (`/admin/server/`) → bấm **Sao lưu ngay** → file backup sẽ xuất hiện trong danh sách.

### Sao lưu tự động
Mặc định: **mỗi ngày 02:00 sáng** (theo múi giờ của máy). Tối đa giữ:
- 30 bản gần nhất
- 12 bản theo tuần

### Phục hồi thủ công
1. **Dừng server** (đóng `RestaurantServer.exe` hoặc vào Task Manager → End task).
2. Sao chép file backup từ `%USERPROFILE%\RestaurantServer\backups\` sang `%USERPROFILE%\RestaurantServer\data\restaurant.db`
3. Khởi động lại server.

---

## 5. Bảo mật

- Mật khẩu được mã hóa bằng BCrypt
- API xác thực bằng JWT (HS256), thời hạn 24 giờ
- Sau 5 lần đăng nhập sai trong 15 phút → khóa tạm
- Mỗi nhân viên có role riêng (`ADMIN` / `STAFF`); STAFF không thể truy cập các API quản trị
- Server chỉ bind `0.0.0.0:8080` và chỉ mở firewall trên profile **Private** — không lộ ra Internet
- Bạn **nên** đổi mật khẩu admin mặc định ngay sau khi cài đặt

---

## 6. Khắc phục sự cố

### Ứng dụng Android báo "Không kết nối được server"
- Kiểm tra Server đang chạy (`RestaurantServer.exe` đang mở và dashboard truy cập được từ trình duyệt)
- Kiểm tra điện thoại và máy chủ **cùng mạng Wi-Fi**
- Tắt VPN trên điện thoại
- Vào **Settings → Server** trong app, bấm **Ghép nối lại**

### Server không hiển thị trên LAN
- Mở Windows Firewall → Inbound Rules → kiểm tra rule **Restaurant Server 8080 (Private)** đã bật
- Tắt phần mềm diệt virus có thể đang chặn

### Quên mật khẩu admin
- Trên máy chủ, mở `%USERPROFILE%\RestaurantServer\config\server.properties`
- Hoặc dùng tài khoản admin khác để reset (qua **Admin Dashboard → Nhân viên**)

### Ứng dụng Android báo "Phiên đăng nhập hết hạn"
- JWT hết hạn sau 24 giờ — đăng nhập lại.

### Nhân viên không nhận được thông báo đẩy (push)
- Kiểm tra điện thoại đã bật **thông báo** cho app Restaurant Staff (Settings → Apps → Restaurant Staff → Notifications).
- Kiểm tra kết nối Internet (push FCM cần Internet — không dùng được khi chỉ ở LAN).
- Đăng xuất khỏi app, đăng nhập lại để đăng ký lại FCM token.
- Nếu vẫn không nhận được, vào **Activity log** trên Dashboard quản trị để kiểm tra `notification_events` — tìm mục `status = FAILED` hoặc `status = SKIPPED_DEDUP`.
- Hoặc mở màn hình **Thông báo** trên app → chạm biểu tượng 🕒 trên một thông báo để xem lịch sử gửi (server đã thử mấy lần, lỗi gì).

### Badge "🔔" trên tab Thông báo hiển thị số cũ
- Badge trên tab dưới cùng được cập nhật mỗi 30 giây. Mở màn hình **Thông báo** rồi quay lại để badge tự cập nhật ngay.
- Hoặc kéo-làm-mới trên màn hình Thông báo.

### Không vào được màn hình Thông báo bằng phím tắt (long-press app icon)
- Một số launcher Android cũ không hỗ trợ static shortcut. Cập nhật launcher lên phiên bản mới, hoặc mở app và chạm tab **Thông báo** ở thanh dưới cùng.

### Nhân viên không thể chuyển trạng thái ca (Đồng ý / Từ chối)
- Ca đã ở trạng thái cuối (`Đồng ý`, `Từ chối`, `Hoàn thành`, `Huỷ`, `Đã đổi`) — không thể phản hồi thêm.
- Liên hệ quản lý để cập nhật thủ công nếu cần thiết.

### Báo lỗi "ALREADY_CHECKED_IN" / "NO_CHECK_IN"
- Nhân viên đã vào ca trước đó mà chưa tan ca, hoặc ngược lại.
- Mở **Settings → Check-in** để xem trạng thái hiện tại, hoặc liên hệ quản lý để điều chỉnh.

### Lỗi "CANNOT_SKIP" khi hoàn thành checklist
- Mục đang cố bỏ qua là **bắt buộc**. Nhấn **Hoàn thành** thay vì **Bỏ qua**.

### Báo lỗi "INVALID_TRANSITION" khi quản lý cập nhật ca
- Ca đang ở trạng thái cuối (`Hoàn thành`, `Huỷ`, `Đã đổi`).
- Tạo ca mới thay vì sửa ca đã hoàn tất.

---

## 7. Liên hệ hỗ trợ

Khi cần hỗ trợ, vui lòng cung cấp:
- Phiên bản Server (xem trên `/admin/server/`)
- File `%USERPROFILE%\RestaurantServer\logs\server.log` (nếu có lỗi)