# Bank API

[![Backend CI](https://github.com/zoe12378/bank-api/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/zoe12378/bank-api/actions/workflows/backend-ci.yml)

以 Java、Spring Boot 與 MySQL 實作的銀行帳戶後端練習專案。重點不是畫面，而是把金融系統常見的資料一致性、權限與稽核規則落實在 API。

## 功能

- 使用者註冊、BCrypt 密碼雜湊與 JWT 登入。
- 僅能查看自己擁有的帳戶與交易紀錄。
- 帳戶間轉帳：扣款、入帳與兩筆交易紀錄在同一個資料庫交易中完成。
- 轉帳時使用悲觀鎖，降低同一帳戶同時扣款造成餘額不一致的風險。
- 交易紀錄支援分頁與日期範圍查詢。
- 統一的 JSON 錯誤格式與轉帳商業規則單元測試。

## 技術

- Java 21
- Spring Boot 4 / Spring MVC / Spring Data JPA / Spring Security
- MySQL 8
- Maven
- JUnit 5、Mockito
- JJWT

## 架構概念

```text
Client
  -> JWT filter
  -> Controller (HTTP / input validation)
  -> Service (transfer rules / transaction boundary)
  -> Repository (JPA / MySQL)
```

資料表關係：一個 `app_users` 使用者可擁有多個 `accounts`；每個帳戶可有多筆 `account_transactions`。

## 本機啟動

### 1. 建立資料庫

先依序在 MySQL Workbench 執行：

1. [database/schema.sql](database/schema.sql)（建立 `bank_demo`、帳戶、交易紀錄與本機範例資料）。
2. [database/user_schema.sql](database/user_schema.sql)
3. [database/refresh_token_schema.sql](database/refresh_token_schema.sql)
4. 註冊 `huang_demo` 等測試使用者後，再執行 [database/assign_demo_account.sql](database/assign_demo_account.sql) 將 A001 指派給該使用者。

### 2. 設定環境變數並啟動

在 PowerShell 的專案根目錄執行。不要把 MySQL 密碼或 JWT 密鑰寫進 Git：

```powershell
$securePassword = Read-Host "MySQL root password" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $securePassword).Password

$randomBytes = New-Object byte[] 32
$rng = New-Object System.Security.Cryptography.RNGCryptoServiceProvider
$rng.GetBytes($randomBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($randomBytes)

.\mvnw.cmd spring-boot:run
```

啟動成功後，服務位於 `http://localhost:8080`。

## API 摘要

| 方法 | 路徑 | 說明 | 是否需 JWT |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | 註冊使用者 | 否 |
| POST | `/api/auth/login` | 登入並取得 Token | 否 |
| GET | `/api/accounts` | 取得自己的帳戶 | 是 |
| GET | `/api/accounts/{accountNumber}` | 取得自己的單一帳戶 | 是 |
| GET | `/api/accounts/{accountNumber}/transactions?page=0&size=20&from=YYYY-MM-DD&to=YYYY-MM-DD` | 取得自己的交易紀錄 | 是 |
| POST | `/api/transfers` | 從自己的帳戶轉帳 | 是 |

受保護 API 需傳送：

```text
Authorization: Bearer <accessToken>
```

### 轉帳範例

```json
{
  "fromAccountNumber": "A001",
  "toAccountNumber": "B001",
  "amount": 10.00
}
```

## 安全與一致性設計

- 密碼只存 BCrypt 雜湊，絕不存明文。
- 所有帳戶／交易查詢以登入使用者過濾；不屬於自己的帳戶回傳 404。
- 轉帳前再次驗證轉出帳戶所有權，避免僅靠前端限制。
- `@Transactional` 讓扣款、入帳、交易紀錄成功或失敗時一起提交或回滾。
- `PESSIMISTIC_WRITE` 鎖定參與轉帳的帳戶，並以固定順序取得鎖，降低併發衝突與死鎖風險。

## 測試

執行不需連 MySQL 的單元測試：

```powershell
.\mvnw.cmd test
```

目前涵蓋：合法轉帳、未擁有帳戶的轉帳、餘額不足與鎖定查詢使用。

另外提供本機手動驗證腳本：

- [scripts/test-transfer-permission.ps1](scripts/test-transfer-permission.ps1)：驗證不可從他人帳戶扣款。
- [scripts/test-owned-transfer.ps1](scripts/test-owned-transfer.ps1)：驗證自己的帳戶可轉帳。
- [scripts/test-my-transactions.ps1](scripts/test-my-transactions.ps1)：查看自己的交易紀錄分頁。

## 後續可擴充項目

- 使用 Testcontainers 建立真正連 MySQL 的整合測試。
- 加入 refresh token、帳戶建立流程與管理者權限。
- 建立 OpenAPI / Swagger 文件。

## API 文件（Swagger UI）

應用程式啟動後，開啟 [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)。

使用順序：

1. 先執行 `POST /api/auth/login` 取得 `accessToken` 與 `refreshToken`。
2. 點選右上角 **Authorize**，貼上 token（只貼 token 本身，不要加上 `Bearer `）。
3. 即可在 Swagger UI 測試帳戶、交易紀錄與轉帳 API。

## Token 安全設計

- Access token 預設有效 30 分鐘，只用於受保護 API。
- Refresh token 預設有效 7 天，資料庫僅保存 SHA-256 雜湊值。
- `POST /api/auth/refresh` 會撤銷舊 refresh token 並發出一組新 token（token rotation）。
- `POST /api/auth/logout` 會撤銷目前的 refresh token。
- 正式環境應將 refresh token 放在 `HttpOnly`、`Secure` Cookie；本專案前端為方便展示，暫存在瀏覽器分頁記憶體中。

## 管理者帳戶建立

- `POST /api/admin/accounts` 僅限 JWT 角色為 `ROLE_ADMIN` 的使用者呼叫。
- 建立帳戶會同時寫入一筆 `OPEN_ACCOUNT` 稽核紀錄，避免帳戶餘額沒有來源。
- 本機展示可執行 [database/promote_huang_demo_admin.sql](database/promote_huang_demo_admin.sql) 將 `huang_demo` 升為管理者；執行後需要重新登入，讓新 JWT 帶入 `ROLE_ADMIN`。

## 測試

```powershell
.\mvnw.cmd test
```

- `TransferServiceTest`：不啟動 Spring 或 MySQL 的單元測試，快速驗證轉帳規則。
- `TransferPersistenceIntegrationTest`：透過 Testcontainers 啟動暫時的 MySQL 8.0 容器，驗證 JPA 寫入、MySQL 與轉帳稽核紀錄能一起運作；不會使用或修改本機的 `bank_demo`。

執行整合測試前，請先開啟 Docker Desktop 並確認 `docker info` 可正常執行。第一次測試會下載 MySQL 映像檔，因此花較久是正常的。
