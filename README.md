# 📖 HƯỚNG DẪN SỬ DỤNG HỆ THỐNG NHÀ HÀNG

> **Phiên bản 1.0.0** — Dành cho chủ nhà hàng, quản lý và nhân viên.  
> Hướng dẫn này giả định bạn **không biết gì về máy tính** — chỉ cần làm theo từng bước.

---

## 🎯 Hệ thống này là gì?

Hệ thống gồm **2 phần mềm** chạy cùng nhau trong nhà hàng của bạn:

| Phần mềm | Cài ở đâu | Để làm gì |
|----------|-----------|-----------|
| 🖥️ **Restaurant Server** | 1 máy tính ở quầy thu ngân | Lưu tất cả dữ liệu (món ăn, nhân viên, hình ảnh). Đây là "bộ não" trung tâm. |
| 📱 **Restaurant Staff** | Mỗi điện thoại/tablet của nhân viên | Nhân viên dùng để xem menu, tra cứu món, xem giá tiền. |

**Điều kiện duy nhất**: máy tính và điện thoại phải **cùng mạng Wi-Fi** với nhau.

```
[SERVER - máy tính quầy]  ◄──Wi-Fi──►  [Điện thoại nhân viên 1]
        │                            ◄──Wi-Fi──►  [Điện thoại nhân viên 2]
        │                            ◄──Wi-Fi──►  [Điện thoại nhân viên 3]
   (lưu dữ liệu)                              (chỉ xem)
```

---

## 📋 MỤC LỤC

1. [Lần đầu tiên: Cài đặt Server lên máy tính](#1-lần-đầu-tiên-cài-đặt-server-lên-máy-tính)
2. [Khởi động Server hằng ngày](#2-khởi-động-server-hằng-ngày)
3. [Quản lý món ăn & danh mục (trên máy tính)](#3-quản-lý-món-ăn--danh-mục-trên-máy-tính)
4. [Quản lý nhân viên (trên máy tính)](#4-quản-lý-nhân-viên-trên-máy-tính)
5. [Cài đặt ứng dụng lên điện thoại nhân viên](#5-cài-đặt-ứng-dụng-lên-điện-thoại-nhân-viên)
6. [Nhân viên sử dụng app mỗi ngày](#6-nhân-viên-sử-dụng-app-mỗi-ngày)
7. [Sao lưu & phục hồi dữ liệu](#7-sao-lưu--phục-hồi-dữ-liệu)
8. [Khi có sự cố](#8-khi-có-sự-cố)
9. [Bảo mật cơ bản](#9-bảo-mật-cơ-bản)

---

## 1. LẦN ĐẦU TIÊN: CÀI ĐẶT SERVER LÊN MÁY TÍNH

> ⏱️ **Thời gian**: khoảng 10 phút.  
> 🛠️ **Cần**: máy tính Windows 10/11, quyền Administrator, file `RestaurantServerSetup.exe` mà bạn đã nhận được.

### Bước 1.1 — Chuẩn bị

- Máy tính này sẽ là **máy quầy thu ngân**, bật 24/24.
- Cắm dây mạng LAN hoặc kết nối Wi-Fi ổn định.
- Bật máy, đăng nhập Windows bằng tài khoản có quyền **Admin** (thường là tài khoản chính của bạn).

### Bước 1.2 — Chạy file cài đặt

1. Tìm đến file `RestaurantServerSetup.exe` (thường nằm trong thư mục **Downloads** hoặc **Desktop**).
2. **Nhấp đúp chuột** vào file đó.
3. Nếu Windows hỏi "Cho phép ứng dụng này thay đổi thiết bị?" → bấm **Yes**.

### Bước 1.3 — Làm theo hướng dẫn trên màn hình

1. Màn hình chào mừng hiện ra → bấm **Next**.
2. Chọn nơi cài đặt:
   - **Mặc định** là `C:\Program Files\Restaurant\RestaurantServer`.  
   - Bạn **không cần đổi**, cứ để vậy rồi bấm **Next**.
3. Bấm **Install** — chờ 1–2 phút.
4. Sau khi xong, **đánh dấu** vào ô *"Launch Restaurant Server"* rồi bấm **Finish**.

### Bước 1.4 — Lần đầu chạy Server

Khi Server khởi động:
- Một **cửa sổ đen** (cmd) sẽ hiện ra — **đừng tắt** nó. Đó chính là Server đang chạy.
- Một **trình duyệt web** sẽ tự mở ra trang `http://localhost:8080/admin/` — đây là trang quản lý.

> 💡 **Mẹo**: cửa sổ đen phải luôn chạy thì mới có dữ liệu. Khi nào muốn tắt Server, **đóng cửa sổ đen** là xong.

### Bước 1.5 — Đăng nhập lần đầu

Tài khoản mặc định (khi mới cài):

| Tên đăng nhập | Mật khẩu | Quyền |
|---------------|----------|-------|
| `admin` | `admin123` | Quản trị viên (toàn quyền) |
| `nhanvien01` | `staff123` | Nhân viên (chỉ xem menu) |

> ⚠️ **RẤT QUAN TRỌNG**: hãy đổi mật khẩu `admin` ngay sau khi đăng nhập lần đầu. Xem [Bước 3.6](#bước-36--đổi-mật-khẩu-admin).

### Bước 1.6 — Ghi lại "IP của Server"

1. Mở trang **Server Dashboard**: <http://localhost:8080/admin/server/>
2. Nhìn vào dòng **"Địa chỉ IP LAN"** — sẽ có dạng `192.168.x.x`.  
   Ví dụ: `192.168.1.10`.
3. **Ghi số này ra giấy** và dán lên tường cạnh máy tính — nhân viên sẽ cần nhập số này khi cài app lên điện thoại.

> 💡 Trên cùng trang Server Dashboard có một **mã QR** — nhân viên dùng điện thoại quét mã này là tự động kết nối, không cần nhập tay.

---

## 2. KHỞI ĐỘNG SERVER HẰNG NGÀY

Server không tự khởi động cùng Windows (để tránh chiếm máy khi bạn không cần). Mỗi ngày làm theo 1 trong 2 cách:

### Cách A — Từ Desktop (dễ nhất)

1. Tìm biểu tượng **"Restaurant Server Dashboard"** trên Desktop.
2. **Nhấp đúp** vào đó → trình duyệt mở ra trang quản lý.
3. Nếu Server chưa chạy, mở Start Menu → gõ `Restaurant Server` → bấm vào kết quả đầu tiên.

### Cách B — Từ Start Menu

1. Bấm nút **Start** (góc trái dưới).
2. Gõ `Restaurant Server`.
3. Bấm vào **RestaurantServer** trong kết quả.
4. Chờ 3–5 giây → Server đã sẵn sàng.

> 💡 **Mẹo cho chủ quán**: nếu bạn muốn Server tự khởi động khi bật máy, nhờ kỹ thuật viên copy shortcut **RestaurantServer** vào thư mục `shell:startup` (chỉ cần 1 lần).

### Kiểm tra Server đang chạy

Mở trình duyệt → gõ `http://localhost:8080/admin/`:
- **Thấy trang đăng nhập** → Server OK. ✅
- **Không thấy gì / báo lỗi** → Server chưa chạy. Mở lại theo cách trên.

---

## 3. QUẢN LÝ MÓN ĂN & DANH MỤC (TRÊN MÁY TÍNH)

> Mở trang <http://localhost:8080/admin/app/> trên trình duyệt → đăng nhập bằng tài khoản `admin`.

### 3.1 — Trang tổng quan

Sau khi đăng nhập, bạn sẽ thấy bảng thống kê:
- **Tổng số món** — bao nhiêu món đang có trong menu.
- **Đang bán** — bao nhiêu món đang bán được.
- **Hết hàng** — bao nhiêu món tạm hết.
- **Số danh mục** — bao nhiêu nhóm món.
- **Số nhân viên** — bao nhiêu tài khoản nhân viên.

### 3.2 — Thêm danh mục mới

**Danh mục** là nhóm món (Phở, Cơm, Tráng miệng, Đồ uống...).

1. Bấm vào tab **"Danh mục"** ở thanh bên trái.
2. Bấm nút **"+ Thêm danh mục"** (góc trên bên phải).
3. Điền:
   - **Tên tiếng Việt**: ví dụ `Món chính`
   - **Tên tiếng Hàn**: ví dụ `주메뉴`
   - **Mô tả (tuỳ chọn)**: ví dụ `Cơm, phở, mì`
4. Bấm **Lưu**.

> 💡 Bạn có thể thêm danh mục **chỉ tiếng Việt** nếu không cần tiếng Hàn. Khi đó hệ thống tự hiển thị tiếng Việt cho cả hai ngôn ngữ.

### 3.3 — Thêm món ăn mới

1. Bấm tab **"Món ăn"**.
2. Bấm **"+ Thêm món"**.
3. Điền:
   - **Tên tiếng Việt** *(bắt buộc)* — ví dụ `Phở bò tái`.
   - **Tên tiếng Hàn** *(khuyến nghị)* — ví dụ `소고기 회국수`.
   - **Mô tả (VI)** — ví dụ `Phở bò với thịt tái chín tới`.
   - **Mô tả (KO)** — ví dụ `얹은 소고기를 얹은 쌀국수`.
   - **Giá** — nhập số, ví dụ `75000`.
   - **Danh mục** — chọn từ danh sách (đã tạo ở Bước 3.2).
   - **Hình ảnh** — bấm "Chọn ảnh" → chọn ảnh từ máy tính (JPG/PNG).
4. Bấm **Lưu**.

> 🎨 **Mẹo ảnh**: ảnh nên chụp ngang, kích thước khoảng **800×600 pixel**. Ảnh đẹp giúp nhân viên giới thiệu món dễ hơn.

### 3.4 — Sửa / ẩn / xóa món

Trong danh sách **"Món ăn"**, mỗi món có các biểu tượng:

| Biểu tượng | Ý nghĩa |
|------------|---------|
| ✏️ Bút | Sửa tên, giá, mô tả, ảnh |
| 👁️ Con mắt (mở) | Đang bán — hiện trên app nhân viên |
| 👁️‍🗨️ Con mắt (gạch) | Hết hàng — vẫn hiện nhưng ghi "Hết" |
| 🚫 Ẩn | Không hiện trên app nhân viên |
| ⭐ Sao | Đánh dấu **Nổi bật** — sẽ hiện ở trang Home |

### 3.5 — Đánh dấu món "Nổi bật" (Featured)

Món nổi bật sẽ hiện ở **trang Home** trên app nhân viên — là những món đặc trưng / mới / chủ lực.

1. Bấm **⭐ Sao** trên món muốn đánh dấu.
2. Sao chuyển sang màu vàng → đã thành công.

Khuyến nghị: chỉ nên đánh dấu **5–10 món** là nổi bật, đừng đánh dấu tất cả.

### 3.6 — Đổi mật khẩu admin

1. Tab **"Nhân viên"**.
2. Tìm dòng `admin` → bấm **🔑 Đổi mật khẩu**.
3. Nhập mật khẩu mới (ít nhất 8 ký tự, có chữ + số).
4. Bấm **Lưu**.

> ⚠️ **Ghi nhớ mật khẩu mới ra giấy và cất kỹ**. Nếu quên, xem [Bước 8.3](#quên-mật-khẩu-admin).

---

## 4. QUẢN LÝ NHÂN VIÊN (TRÊN MÁY TÍNH)

### 4.1 — Thêm nhân viên mới

1. Vào tab **"Nhân viên"**.
2. Bấm **"+ Thêm nhân viên"**.
3. Điền:
   - **Tên đăng nhập** *(bắt buộc)* — ví dụ `nhanvien02`.  
     **Lưu ý**: không dấu, không khoảng trắng, không trùng với người khác.
   - **Mật khẩu** — ví dụ `nv0203`. Cho nhân viên biết để họ đăng nhập lần đầu, rồi yêu cầu họ đổi sau.
   - **Họ và tên** *(khuyến nghị)* — ví dụ `Nguyễn Văn B`.
   - **Vai trò**:
     - `STAFF` — nhân viên thường (chỉ xem menu).
     - `ADMIN` — quản trị viên (toàn quyền, **chỉ dành cho chủ/quản lý**).
4. Bấm **Lưu**.

### 4.2 — Khoá tài khoản nhân viên (khi nghỉ việc)

1. Tìm tên nhân viên trong danh sách.
2. Bấm **🚫 Vô hiệu hoá**.
3. Nhân viên đó sẽ không đăng nhập được nữa.  
   Dữ liệu (lịch sử, v.v.) vẫn được giữ — không mất.

### 4.3 — Reset mật khẩu nhân viên

1. Tìm nhân viên → bấm **🔑 Reset mật khẩu**.
2. Nhập mật khẩu mới.
3. Bấm **Lưu**.
4. Báo cho nhân viên biết để họ đăng nhập lại.

---

## 5. CÀI ĐẶT ỨNG DỤNG LÊN ĐIỆN THOẠI NHÂN VIÊN

> ⏱️ **Thời gian**: khoảng 5 phút / điện thoại.

### 5.1 — Cho phép cài app từ file

Mặc định Android chặn app không tải từ Google Play. Bạn cần cho phép **1 lần**:

1. Trên điện thoại, vào **Settings** (Cài đặt).
2. Tìm **Security & Privacy** (Bảo mật & quyền riêng tư) → **Install unknown apps** (Cài ứng dụng không xác định).
3. Chọn trình duyệt / Files / Chrome (tuỳ cách bạn dùng để mở file).
4. Bật **Allow from this source** (Cho phép từ nguồn này).

### 5.2 — Cài file APK

1. Copy file `RestaurantStaff.apk` sang điện thoại (qua cáp USB, gửi email cho nhân viên, hoặc tải lên Google Drive rồi tải xuống).
2. Mở file APK → bấm **Install** → bấm **Open**.

### 5.3 — Ghép nối với Server

Khi mở app lần đầu, màn hình **"Kết nối máy chủ"** hiện ra.

**Cách A — Quét mã QR (dễ nhất):**

1. Trên máy tính quầy, mở <http://localhost:8080/admin/server/>.
2. Đưa điện thoại vào **quét mã QR** hiển thị trên trang đó.
3. App tự nhận diện → báo "Ghép nối thành công" → chuyển sang màn hình **Đăng nhập**.

**Cách B — Nhập thủ công:**

1. Bấm **"Nhập thủ công"** trên app.
2. Nhập `IP:Port` mà bạn đã ghi ra giấy ở Bước 1.6.  
   Ví dụ: `192.168.1.10:8080`.
3. Bấm **Kết nối**.

### 5.4 — Đăng nhập

Nhập:
- **Tên đăng nhập**: ví dụ `nhanvien02`.
- **Mật khẩu**: mật khẩu bạn đã cấp ở Bước 4.1.

> 💡 Nút **👁** cạnh ô mật khẩu để hiện / ẩn mật khẩu.

Sau khi đăng nhập thành công, app chuyển sang trang **Home**.

---

## 6. NHÂN VIÊN SỬ DỤNG APP MỖI NGÀY

### 6.1 — Màn hình Home

- Hiển thị **món nổi bật** (mà bạn đã đánh dấu sao).
- Hiển thị **thông tin nhà hàng** (tên, địa chỉ, giờ mở cửa).
- Bấm vào món → xem chi tiết.

### 6.2 — Tab "Menu"

- Hiển thị **tất cả món** trong nhà hàng.
- **Cuộn ngang** để lọc theo danh mục (Phở, Cơm, Đồ uống...).
- **Ô tìm kiếm** ở trên cùng: gõ tên món → danh sách tự lọc.  
  Ví dụ: gõ `phở` → chỉ hiện các món phở.

### 6.3 — Chi tiết món

Bấm vào món bất kỳ → hiện ra:
- **Hình ảnh** lớn.
- **Tên tiếng Việt + tiếng Hàn** (nếu có).
- **Mô tả chi tiết** (VI + KO).
- **Giá tiền**.
- **Danh mục**.
- **Trạng thái** (Đang bán / Hết hàng / Ẩn).

### 6.4 — Tab "Profile" (Hồ sơ)

Xem:
- Tên đăng nhập.
- Họ và tên.
- Vai trò (Admin / Staff).

Có 2 nút:
- **Cài đặt** — đổi ngôn ngữ (Việt ↔ Hàn), xem IP server, ghép nối lại.
- **Đăng xuất** — thoát khỏi app (cần đăng nhập lại lần sau).

### 6.5 — Đổi ngôn ngữ

1. Tab **Profile** → **Cài đặt**.
2. Bấm vào **Tiếng Việt** hoặc **한국어** (Tiếng Hàn).
3. App đổi ngôn ngữ **ngay lập tức**, không cần khởi động lại.

### 6.6 — Đăng xuất khi hết ca

1. Tab **Profile** → **Đăng xuất**.
2. Xác nhận.
3. Lần sau mở app sẽ phải đăng nhập lại.

> 💡 Nếu nhân viên quên đăng xuất mà bạn muốn bắt buộc đăng nhập lại: vào **tab Nhân viên** trên máy tính → **🔑 Reset mật khẩu** của nhân viên đó.

---

## 7. SAO LƯU & PHỤC HỒI DỮ LIỆU

> ⚠️ **Quan trọng**: máy tính có thể hỏng, virus có thể tấn công. Sao lưu là cách duy nhất để khôi phục dữ liệu.

### 7.1 — Sao lưu tự động

Hệ thống **tự động sao lưu mỗi ngày lúc 02:00 sáng**. Bạn không cần làm gì.

Các bản sao lưu cũ hơn 30 ngày sẽ tự động bị xoá để tiết kiệm dung lượng.

### 7.2 — Sao lưu thủ công (trước khi sửa lớn)

1. Mở <http://localhost:8080/admin/server/>.
2. Bấm **"💾 Sao lưu ngay"**.
3. Chờ 5–10 giây → bản sao lưu mới xuất hiện trong danh sách.

### 7.3 — Sao lưu ra USB (rất khuyến nghị)

1. Mở **File Explorer** trên Windows.
2. Gõ vào thanh địa chỉ: `%USERPROFILE%\RestaurantServer\backups` rồi Enter.
3. Bạn sẽ thấy danh sách file `backup_2025-01-15_020000.db`, `backup_2025-01-16_020000.db`, ...
4. **Copy các file này ra USB** hoặc lên Google Drive / OneDrive.
5. Làm **mỗi tuần 1 lần** là an toàn.

### 7.4 — Phục hồi từ bản sao lưu

> ⚠️ **Chỉ làm khi cần thiết** (sau khi mất dữ liệu). Phục hồi sẽ **ghi đè** toàn bộ dữ liệu hiện tại.

1. **Tắt Server**: đóng cửa sổ đen của Server (hoặc vào Task Manager → End task `RestaurantServer.exe`).
2. Mở File Explorer → vào `%USERPROFILE%\RestaurantServer\data\` → tìm file `restaurant.db`.
3. **Đổi tên** file này thành `restaurant.db.old` (để backup phòng hờ).
4. **Copy file backup** mà bạn muốn phục hồi (ví dụ `backup_2025-01-15_020000.db`) **vào thư mục `data\`**.
5. **Đổi tên** file vừa copy thành `restaurant.db`.
6. Khởi động lại Server (xem [Bước 2](#2-khởi-động-server-hằng-ngày)).
7. Kiểm tra dữ liệu đã đúng chưa.

---

## 8. KHI CÓ SỰ CỐ

### 8.1 — Điện thoại báo "Không kết nối được server"

Làm theo thứ tự:

1. ✅ **Kiểm tra Server còn chạy không** — mở `http://localhost:8080/admin/` trên máy tính. Nếu không được → khởi động lại Server (Bước 2).
2. ✅ **Cùng Wi-Fi chưa?** — điện thoại và máy tính phải cùng mạng. Tên Wi-Fi hiển thị ở góc phải thanh taskbar Windows và trong Settings điện thoại.
3. ✅ **Tắt VPN** trên điện thoại nếu có.
4. ✅ **Ghép nối lại**: vào **Profile → Cài đặt → Server → Ghép nối lại** trên app, quét lại QR hoặc nhập lại IP.
5. ❓ **Vẫn không được?** — gọi kỹ thuật viên, cung cấp file log ở `%USERPROFILE%\RestaurantServer\logs\server.log`.

### 8.2 — Server không hiện trên các máy khác trong LAN

1. Mở **Control Panel** → **Windows Defender Firewall** → **Advanced settings** → **Inbound Rules**.
2. Tìm rule **"Restaurant Server 8080 (Private)"**.
3. Đảm bảo nó được bật (Enable). Nếu không có → cài lại Server.

### 8.3 — Quên mật khẩu admin

**Cách A — Nhờ admin khác reset**:

1. Đăng nhập bằng một tài khoản admin khác.
2. Vào **tab Nhân viên** → tìm `admin` → **🔑 Reset mật khẩu**.

**Cách B — Reset thủ công (cần có kỹ thuật viên)**:

1. Tắt Server.
2. Mở File Explorer → vào `%USERPROFILE%\RestaurantServer\data\`.
3. Mở file `restaurant.db` bằng **DB Browser for SQLite** (phần mềm miễn phí, tải từ sqlitebrowser.org).
4. Mở bảng `users` → tìm dòng `admin` → **KHÔNG SỬA TRỰC TIẾP MẬT KHẨU** (vì đã mã hoá).
5. Cách an toàn: tạo user mới với role ADMIN, rồi login bằng user mới, reset mật khẩu admin qua UI.

> 💡 **Phòng tránh**: luôn có **ít nhất 2 tài khoản admin** trong hệ thống.

### 8.4 — Máy tính bị hỏng / mất cắp

1. Mua máy tính mới (cùng cấu hình hoặc cao hơn).
2. Cài lại Windows.
3. Cài lại **RestaurantServerSetup.exe**.
4. Phục hồi dữ liệu từ bản sao lưu USB (xem Bước 7.4).
5. Nhân viên quét lại QR mới → dùng tiếp.

### 8.5 — Điện thoại bị mất / hỏng

1. Mua điện thoại mới.
2. Cài lại app **RestaurantStaff.apk** (Bước 5).
3. Quét QR → đăng nhập lại bằng tài khoản nhân viên.  
   **Dữ liệu không bị mất** vì toàn bộ lưu trên Server.

### 8.6 — Món ăn hiển thị sai / không cập nhật

1. Trên app, **kéo xuống** ở đầu trang để làm mới.
2. Nếu vẫn sai → **đăng xuất → đăng nhập lại**.

---

## 9. BẢO MẬT CƠ BẢN

### 9.1 — Quy tắc mật khẩu

- Mật khẩu admin phải có **ít nhất 8 ký tự**, gồm chữ + số.
- **Không dùng** các mật khẩu như `12345678`, `password`, `admin`.
- **Đổi mật khẩu** mỗi 3 tháng.

### 9.2 — Bảo vệ máy tính Server

- Đặt mật khẩu Windows (đã làm chưa? Nếu chưa → vào Settings → Accounts → Sign-in options → Password).
- Không cho nhân viên dùng chung máy Server. Chỉ chủ/quản lý mới đăng nhập Windows.
- Không cắm USB lạ vào máy Server.

### 9.3 — Bảo vệ Wi-Fi nhà hàng

- Đổi mật khẩu Wi-Fi mặc định của router.
- Đặt mật khẩu **mạnh** (ít nhất 12 ký tự).
- Dùng chuẩn **WPA2 hoặc WPA3** (không dùng WEP).

### 9.4 — Đăng xuất khi không dùng

- Nhân viên: bấm **Đăng xuất** trên app khi hết ca.
- Chủ quán: khoá màn hình máy tính khi rời đi (phím `Windows + L`).

---

## 📞 LIÊN HỆ HỖ TRỢ

Khi cần hỗ trợ kỹ thuật, vui lòng cung cấp:

1. **Phiên bản Server** — xem ở trang <http://localhost:8080/admin/server/> (góc trên cùng).
2. **Mô tả lỗi** — bạn đang làm gì thì xảy ra lỗi? Lỗi hiện thông báo gì?
3. **Ảnh chụp màn hình** nếu có.
4. **File log** — đường dẫn `%USERPROFILE%\RestaurantServer\logs\server.log` (file này ghi lại hoạt động của hệ thống).

---

## 📚 THUẬT NGỮ DỄ HIỂU

| Thuật ngữ | Nghĩa |
|-----------|--------|
| **Server** | Máy tính ở quầy, lưu toàn bộ dữ liệu |
| **App** | Ứng dụng trên điện thoại |
| **Dashboard** | Trang quản lý trên trình duyệt web |
| **Món ăn (Food)** | Một món trong menu (Phở, Cơm gà, ...) |
| **Danh mục (Category)** | Nhóm món (Phở, Cơm, Đồ uống, ...) |
| **Ghép nối (Pairing)** | Kết nối điện thoại với Server lần đầu |
| **QR Code** | Mã vuông đen trắng, quét bằng camera điện thoại |
| **IP** | Địa chỉ của máy tính trong mạng (ví dụ `192.168.1.10`) |
| **Wi-Fi / LAN** | Mạng nội bộ của nhà hàng |
| **Sao lưu (Backup)** | Bản sao lưu dữ liệu để phòng khi máy hỏng |
| **Admin** | Quản trị viên (chủ/quản lý) |
| **Nhân viên (Staff)** | Người dùng app để xem menu |
| **Kiosk Mode** | Chế độ khoá điện thoại vào app (xem thêm ở file `kiosk-provisioning.md`) |
| **Thông báo đẩy (Push)** | Tin nhắn quản lý gửi đến điện thoại nhân viên (phân ca, đổi ca, tin nhắn nội bộ). Cần cấu hình Firebase — mặc định dùng REST feed trong app. |

---

> ✨ **Chúc bạn kinh doanh thuận lợi!**  
> Phiên bản tài liệu: **1.0.0** — cập nhật lần cuối: 2026.