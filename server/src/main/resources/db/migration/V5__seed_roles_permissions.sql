-- V2.2 / V5: seed roles + permissions + role_permission matrix
-- Idempotent: uses INSERT OR IGNORE on the unique `code` columns so this
-- migration is safe to re-run after `flyway repair`. Also guarded by code
-- names that don't change between versions.
--
-- Role codes MUST match `users.role` values (after V6 migration). They are:
--   OWNER   - full system access (single user)
--   MANAGER - people + zones + reports (per-store)
--   ADMIN   - legacy content admin (menus, categories, store) -- preserved
--   STAFF   - front-line: check-in, checklist, view assigned zone

INSERT OR IGNORE INTO roles (code, description, sort_order) VALUES
    ('OWNER',   'Chủ sở hữu — toàn quyền',                1),
    ('MANAGER', 'Quản lý — nhân viên, khu vực, báo cáo',  2),
    ('ADMIN',   'Quản trị — thực đơn, danh mục, cửa hàng', 3),
    ('STAFF',   'Nhân viên — check-in, checklist',        4);

-- Permission catalog (V2.3 baseline; extend later without migration).
-- Each permission has vi + ko names for documentation; the runtime authority
-- string sent in JWT is `code` only.
INSERT OR IGNORE INTO permissions (code, name_vi, name_ko, category, sort_order) VALUES
    -- Employee management (MANAGER+)
    ('EMPLOYEE_VIEW',          'Xem nhân viên',             '직원 조회',             'EMPLOYEE',  1),
    ('EMPLOYEE_CREATE',        'Tạo nhân viên',             '직원 생성',             'EMPLOYEE',  2),
    ('EMPLOYEE_UPDATE',        'Cập nhật nhân viên',        '직원 수정',             'EMPLOYEE',  3),
    ('EMPLOYEE_DEACTIVATE',    'Vô hiệu hóa nhân viên',     '직원 비활성화',         'EMPLOYEE',  4),
    ('EMPLOYEE_ASSIGN_ROLE',   'Gán vai trò',               '역할 할당',             'EMPLOYEE',  5),
    ('EMPLOYEE_RESET_PASSWORD','Đặt lại mật khẩu',         '비밀번호 재설정',        'EMPLOYEE',  6),

    -- Zone management (MANAGER+)
    ('ZONE_VIEW',              'Xem khu vực',               '구역 조회',             'ZONE',      1),
    ('ZONE_CREATE',            'Tạo khu vực',               '구역 생성',             'ZONE',      2),
    ('ZONE_UPDATE',            'Cập nhật khu vực',          '구역 수정',             'ZONE',      3),
    ('ZONE_DISABLE',           'Vô hiệu hóa khu vực',       '구역 비활성화',         'ZONE',      4),
    ('ZONE_QR_REGENERATE',     'Tạo lại QR khu vực',        '구역 QR 재생성',         'ZONE',      5),

    -- Check-in / check-out (STAFF can check self; MANAGER+ views all)
    ('CHECKIN_SELF',           'Tự check-in',               '본인 체크인',           'CHECKIN',   1),
    ('CHECKIN_VIEW_OWN',       'Xem lịch sử cá nhân',       '본인 기록 조회',         'CHECKIN',   2),
    ('CHECKIN_VIEW_ALL',       'Xem tất cả check-in',       '전체 체크인 조회',       'CHECKIN',   3),
    ('CHECKIN_OVERRIDE',       'Can thiệp check-in nhân viên','직원 체크인 개입',     'CHECKIN',   4),

    -- Zone assignment / transfer (MANAGER+)
    ('ASSIGNMENT_VIEW',        'Xem phân công',             '배정 조회',             'ASSIGNMENT', 1),
    ('ASSIGNMENT_CREATE',      'Phân công nhân viên',        '직원 배정',             'ASSIGNMENT', 2),
    ('ASSIGNMENT_TRANSFER',    'Điều chuyển nhân viên',     '직원 이동',             'ASSIGNMENT', 3),

    -- Checklist (MANAGER+ to manage; STAFF to view/complete)
    ('CHECKLIST_VIEW_OWN',     'Xem checklist cá nhân',     '본인 체크리스트 조회',   'CHECKLIST',  1),
    ('CHECKLIST_VIEW_ALL',     'Xem tất cả checklist',      '전체 체크리스트 조회',   'CHECKLIST',  2),
    ('CHECKLIST_COMPLETE',     'Hoàn thành checklist',      '체크리스트 완료',       'CHECKLIST',  3),
    ('CHECKLIST_UPLOAD_PHOTO', 'Tải ảnh checklist',         '체크리스트 사진 업로드', 'CHECKLIST',  4),
    ('CHECKLIST_MANAGE',       'Quản lý checklist',         '체크리스트 관리',       'CHECKLIST',  5),

    -- Shift (MANAGER+)
    ('SHIFT_VIEW_OWN',         'Xem ca cá nhân',            '본인 근무 조회',        'SHIFT',      1),
    ('SHIFT_VIEW_ALL',         'Xem tất cả ca',             '전체 근무 조회',        'SHIFT',      2),
    ('SHIFT_MANAGE',           'Quản lý ca làm việc',       '근무 관리',             'SHIFT',      3),

    -- Manager dashboard + Reports
    ('DASHBOARD_MANAGER',      'Xem bảng quản lý',          '관리 대시보드',          'DASHBOARD',  1),
    ('REPORT_VIEW',            'Xem báo cáo',               '보고서 조회',           'REPORT',     1),
    ('REPORT_EXPORT',          'Xuất báo cáo',              '보고서 내보내기',        'REPORT',     2),

    -- Notifications
    ('NOTIFICATION_RECEIVE',   'Nhận thông báo',            '알림 수신',             'NOTIFICATION', 1),
    ('NOTIFICATION_BROADCAST', 'Gửi thông báo',             '알림 발송',             'NOTIFICATION', 2),

    -- Backup + Logs + Settings + User mgmt (OWNER only)
    ('BACKUP_CREATE',          'Tạo bản sao lưu',           '백업 생성',             'BACKUP',     1),
    ('BACKUP_RESTORE',         'Khôi phục bản sao lưu',     '백업 복원',             'BACKUP',     2),
    ('BACKUP_DOWNLOAD',        'Tải bản sao lưu',           '백업 다운로드',         'BACKUP',     3),
    ('ACTIVITY_LOG_VIEW',      'Xem nhật ký hoạt động',     '활동 로그 조회',         'LOG',        1),
    ('SETTINGS_VIEW',          'Xem cài đặt',               '설정 조회',             'SETTINGS',   1),
    ('SETTINGS_UPDATE',        'Cập nhật cài đặt',          '설정 변경',             'SETTINGS',   2),
    ('USER_MANAGEMENT',        'Quản lý tài khoản',         '계정 관리',             'SETTINGS',   3);

-- Role-Permission matrix
-- OWNER  : every permission
-- MANAGER: everything except BACKUP_* and USER_MANAGEMENT (OWNER's domain)
-- ADMIN  : legacy content admin + sees logs
-- STAFF  : SELF-only operations
--
-- We do this via INSERT OR IGNORE so re-running is harmless.

INSERT OR IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'OWNER';

INSERT OR IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'MANAGER'
  AND p.code NOT IN ('BACKUP_CREATE','BACKUP_RESTORE','BACKUP_DOWNLOAD',
                     'SETTINGS_UPDATE','USER_MANAGEMENT','EMPLOYEE_ASSIGN_ROLE');

-- ADMIN: content management + read-only access to zones/employees
INSERT OR IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN ('ZONE_VIEW','EMPLOYEE_VIEW','CHECKIN_VIEW_ALL',
                 'ACTIVITY_LOG_VIEW','REPORT_VIEW');

-- STAFF: self check-in, own checklist, own shift, own notifications
INSERT OR IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'STAFF'
  AND p.code IN ('CHECKIN_SELF','CHECKIN_VIEW_OWN',
                 'CHECKLIST_VIEW_OWN','CHECKLIST_COMPLETE',
                 'SHIFT_VIEW_OWN','NOTIFICATION_RECEIVE');