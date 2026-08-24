# Development Setup — Restaurant LAN System

> Hướng dẫn cài đặt môi trường phát triển đầy đủ trên Windows 10/11.
>
> **Yêu cầu tối thiểu:** JDK 21 + Maven 3.9+ + Android Studio (cho Android).

---

## 1. Java Development Kit (JDK) 21

Server (`server/`) yêu cầu Java 21 (xem `server/pom.xml`). Máy nhiều người chỉ có JRE 8 từ trước — cần cài JDK 21 riêng.

### Khuyến nghị: Eclipse Temurin 21 (Adoptium)

1. Tải `OpenJDK 21 (LTS)` — Windows x64 installer:
   - https://adoptium.net/temurin/releases/?version=21
   - Hoặc direct: `OpenJDK21U-jdk_x64_windows_hotspot_21.0.X.msi`
2. Double-click `.msi` → Next → Install (mặc định cài vào `C:\Program Files\Eclipse Adoptium\jdk-21.0.X.X-hotspot`).
3. Sau khi cài, **set JAVA_HOME** và cập nhật PATH.

### Set JAVA_HOME (PowerShell — chạy as Administrator)

```powershell
# Xem các JDK đã cài
Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory
Get-ChildItem 'C:\Program Files\Java' -Directory

# Set JAVA_HOME cho session hiện tại
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.X.X-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# Set vĩnh viễn (machine-wide)
[System.Environment]::SetEnvironmentVariable(
  'JAVA_HOME',
  'C:\Program Files\Eclipse Adoptium\jdk-21.0.X.X-hotspot',
  [System.EnvironmentVariableTarget]::Machine
)
[System.Environment]::SetEnvironmentVariable(
  'Path',
  "$([System.Environment]::GetEnvironmentVariable('Path','Machine'));$env:JAVA_HOME\bin",
  [System.EnvironmentVariableTarget]::Machine
)

# Verify
java -version
javac -version
```

### Alternative: Microsoft Build of OpenJDK 21

- https://learn.microsoft.com/en-us/java/openjdk/download
- Đặt tại `C:\Program Files\Microsoft\jdk-21.0.X.X`

### Alternative: dùng `jenv` (nếu đã có scoop)

```powershell
scoop bucket add java
scoop install temurin21-jdk
scoop reset temurin21-jdk
```

---

## 2. Apache Maven 3.9+

### Cách A: cài standalone

1. Tải binary zip: https://maven.apache.org/download.cgi → `apache-maven-3.9.X-bin.zip`
2. Giải nén vào `C:\Program Files\Apache\apache-maven-3.9.X\`.
3. Thêm `MAVEN_HOME` + PATH:

```powershell
$env:MAVEN_HOME = 'C:\Program Files\Apache\apache-maven-3.9.X'
[System.Environment]::SetEnvironmentVariable('MAVEN_HOME', $env:MAVEN_HOME, 'Machine')
[System.Environment]::SetEnvironmentVariable(
  'Path',
  "$([System.Environment]::GetEnvironmentVariable('Path','Machine'));$env:MAVEN_HOME\bin",
  'Machine'
)

mvn --version   # verify
```

### Cách B: dùng wrapper

Repo này chưa có `mvnw`/`.mvn/wrapper/` — cần tạo trước nếu muốn dùng wrapper:

```cmd
cd C:\AppRestaurant\server
mvn -N io.takari:maven:wrapper
```

Sau đó dùng `./mvnw clean package`.

### Cách C: Scoop (nhanh nhất)

```powershell
scoop install maven
mvn --version
```

---

## 3. Android Studio + SDK

Để build Android APK:

1. Tải Android Studio: https://developer.android.com/studio (Koala 2024.1.1+ hoặc mới hơn).
2. Trong lần đầu chạy, Setup Wizard sẽ tự cài Android SDK + platform 34.
3. Đảm bảo các package:
   - Android SDK 34 (compileSdk, targetSdk)
   - Android Build Tools 34.0.0+
   - JDK 17 hoặc 21 (Android Studio bundled JBR đã OK)
   - Kotlin 1.9+ plugin (đã có sẵn trong Android Studio mới)

### Verify setup

```cmd
cd C:\AppRestaurant\android
gradlew.bat --version
gradlew.bat tasks
```

---

## 4. Verification script (chạy sau khi cài xong)

```powershell
# Save as C:\AppRestaurant\verify-setup.ps1
$ok = $true
foreach ($cmd in @(
    @{n='java';   a='-version'},
    @{n='javac';  a='-version'},
    @{n='mvn';    a='--version'},
    @{n='git';    a='--version'}
)) {
    try {
        $output = (& $cmd.n $cmd.a 2>&1 | Out-String).Trim()
        if ($output) {
            Write-Host "[OK] $($cmd.n): $($output.Split("`n")[0])"
        } else {
            Write-Host "[FAIL] $($cmd.n) returns no output"; $ok = $false
        }
    } catch {
        Write-Host "[FAIL] $($cmd.n) not found: $_"; $ok = $false
    }
}
# Android: optional
if (Get-Command gradlew -ErrorAction SilentlyContinue) {
    Write-Host "[INFO] gradle wrapper at C:\AppRestaurant\android\gradlew.bat"
} else {
    Write-Host "[INFO] gradle wrapper missing — will be added later"
}
if ($ok) { Write-Host "`nAll required tools installed." } else { Write-Host "`nMissing some tools. See above." }
```

```powershell
.\verify-setup.ps1
```

---

## 5. Build smoke test

Sau khi cài JDK 21 + Maven:

```cmd
cd C:\AppRestaurant\server
mvn -DskipTests package
# output: target\restaurant-server-1.0.0.jar

java -jar target\restaurant-server-1.0.0.jar
# → http://localhost:8080/actuator/health returns {"status":"UP"}
```

Android (cần Android Studio hoặc SDK + JDK 17/21):

```cmd
cd C:\AppRestaurant\android
gradlew.bat :app:assembleRelease
# output: app\build\outputs\apk\release\app-release.apk
```

---

## 6. Troubleshooting

### ❌ `mvn: not recognized` sau khi cài Maven

→ Mở **cửa sổ PowerShell mới** (env var mới set chỉ apply cho session mới) hoặc chạy trong cmd:
```cmd
refreshenv   # nếu có Chocolatey
```

### ❌ `error: invalid target release: 21`

→ `mvn -v` cho thấy JDK đang dùng không phải 21. Sửa `$JAVA_HOME` hoặc gỡ JDK cũ khỏi PATH.

### ❌ Maven download dependencies chậm / timeout

→ Cấu hình mirror:
```xml
<!-- C:\Users\QUYEN\.m2\settings.xml -->
<settings>
  <mirrors>
    <mirror>
      <id>google-maven-central</id>
      <mirrorOf>central</mirrorOf>
      <name>GCS Maven Central mirror</name>
      <url>https://maven-central.storage-download.googleapis.com/maven2/</url>
    </mirror>
  </mirrors>
</settings>
```

### ❌ Android build fail ở signing

→ `gradle.properties.local` cần chứa:
```
RELEASE_STORE_FILE=release.keystore
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```
(Nội dung này do team dev cung cấp qua secure channel — **KHÔNG commit vào git**.)

---

## 7. Tooling checklist

| Tool | Required version | Why |
|------|------------------|-----|
| JDK | **21 LTS** | server/pom.xml java.version=21 |
| Maven | **3.9.x** | Spring Boot 3.3.4 cần Maven 3.6.3+, recommended 3.9+ |
| Android Studio | **Koala 2024.1.1+** hoặc mới hơn | match SDK 34 + Compose BOM |
| Git | 2.40+ | đã có |
| PowerShell | 7+ (hoặc 5.1 default OK) | dev script |

---

## 8. Recommended IDE setup

- **IntelliJ IDEA Ultimate** (nếu có) — mở root `c:\AppRestaurant` là detect cả 2 module.
- Hoặc **VS Code** với extensions: Java Extension Pack, Spring Boot Extension Pack, Kotlin Language, Android SDK Manager (for Android folder).
- **Cursor** (current) — dùng OK cho documentation/script work; cần chuyển sang IntelliJ/Android Studio cho Java/Kotlin code editing.

---

**Tạo bởi session ngày 2026-08-24 • Bước tiếp theo: cài JDK 21 + Maven rồi chạy `mvn clean package`.**