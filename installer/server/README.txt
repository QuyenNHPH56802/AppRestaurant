Restaurant Server v1.0.0 - Lightweight Installer
====================================================

YEU CAU
-------
- Windows 10 hoac moi hon (64-bit)
- Quyen Administrator (de copy vao C:\Restaurant va mo firewall)

CAI DAT
-------
1. Giai nen folder RestaurantServer-Setup vao mot vi tri bat ky.
2. Click phai vao install.bat chon "Run as administrator".
3. Cho qua trinh cai dat chay (~30 giay).
4. Server tu dong khoi dong va mo trinh duyet den dashboard.

CAI DAT NHANH (khong can admin)
--------------------------------
Neu khong co quyen admin, ban co the chay truc tiep:
  > RestaurantServer.exe
Server se chay o cong 18080. Truy cap: http://localhost:18080/admin/

TAI KHOAN MAC DINH
------------------
- admin / admin123   (quan tri vien)
- nhanvien01 / staff123 (nhan vien)
- nhanvien02..06 / staff123

THONGTIN KY THUAT
-----------------
- Port: 18080
- Data dir: %USERPROFILE%\RestaurantServer\data
- Log file: %USERPROFILE%\RestaurantServer\restaurant.log
- QR server: http://localhost:18080/api/server/qr.png
- Zone QRs:  http://localhost:18080/api/zones/{CODE}/qr.png

GỠ CÀI ĐẶT
-----------
- Chay uninstall.bat voi quyen Administrator
- Hoac thu cong: xoa C:\Restaurant\RestaurantServer

FIREWALL
--------
- Install.bat tu dong mo port 18080 tren profile Private (LAN).
- Neu that bai, them rule thu cong:
  > netsh advfirewall firewall add rule name="Restaurant Server 18080" dir=in action=allow protocol=TCP localport=18080 profile=private
