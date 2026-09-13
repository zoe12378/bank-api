# Bank API

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

1. Day 2 的帳戶與交易資料表 SQL。
2. [database/user_schema.sql](database/user_schema.sql)
3. 註冊 `huang_demo` 等測試使用者後，再執行 [database/assign_demo_account.sql](database/assign_demo_account.sql) 將 A001 指派給該使用者。

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

## 下一步

- 使用 Testcontainers 建立真正連 MySQL 的整合測試。
- 加入 refresh token、帳戶建立流程與管理者權限。
- 建立前端或 OpenAPI / Swagger 文件。
