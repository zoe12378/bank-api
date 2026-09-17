# Bank API：新手從零學習的銀行帳戶系統筆記

[![Backend CI](https://github.com/zoe12378/bank-api/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/zoe12378/bank-api/actions/workflows/backend-ci.yml)

這是一個以 **Java 21、Spring Boot 4、MySQL 與 React** 製作的全端銀行帳戶展示專案。README 的前半段用第一次學後端的角度撰寫；不需要先理解所有名詞，跟著閱讀與操作即可。

專案的重點不是「新增、查詢、修改、刪除」本身，而是練習一個金融系統最需要的幾件事：**登入身分、帳戶所有權、交易一致性、稽核紀錄、角色權限、測試與部署**。

## 線上體驗

- [前端操作介面（Vercel）](https://bank-web-git-main-zoe12378s-projects.vercel.app/)
- [API 文件（Railway Swagger UI）](https://bank-api-production-41c0.up.railway.app/swagger-ui/index.html)
- [前端原始碼：bank-web](https://github.com/zoe12378/bank-web)
- [本機整合環境：bank-stack](https://github.com/zoe12378/bank-stack)

線上資料庫與本機資料庫彼此獨立。請先從前端註冊測試帳號；新使用者尚未被指派帳戶時，帳戶清單會是空的，這是目前的權限設計。

---

## 0. 第一次學習，先看這裡

### 這個專案到底是什麼？

你可以把它想成一個簡化版網路銀行：

```text
你在網頁上按「登入」或「轉帳」
        ↓
前端把請求送給後端
        ↓
後端檢查你是誰、能不能做這件事、餘額夠不夠
        ↓
後端把結果寫進資料庫
        ↓
前端把結果顯示在畫面上
```

這個專案不是要模擬真正銀行的全部功能，而是用「帳戶與轉帳」這個情境，練習後端最重要的基礎。

### 先認識 8 個名詞

| 名詞 | 新手版說明 |
| --- | --- |
| 前端（Frontend） | 使用者看得到、可以按按鈕的網頁；本專案是 React。 |
| 後端（Backend） | 接收請求、判斷規則、讀寫資料的程式；本專案是 Spring Boot。 |
| API | 前端和後端溝通的「固定網址與規則」，例如 `POST /api/auth/login`。 |
| 資料庫（Database） | 長期保存使用者、帳戶、交易紀錄的地方；本專案是 MySQL。 |
| Controller | 後端的入口，接到 API 請求後交給其他程式處理。 |
| Service | 放商業規則的地方，例如「餘額不足不能轉帳」。 |
| JWT | 登入後拿到的一張數位通行證，之後呼叫受保護 API 時要帶著它。 |
| Docker | 把資料庫與程式包成一致環境的工具，減少「我的電腦可以、你的不行」。 |

### 建議學習順序

不要一開始就讀全部程式碼。請照這個順序：

1. **先用線上前端操作一次**：註冊、登入、看帳戶、嘗試轉帳。
2. **看 Swagger UI**：了解每個 API 要傳什麼、會回什麼。
3. **看資料庫資料表**：理解使用者、帳戶、交易紀錄的關係。
4. **看 `AccountController`**：認識 API 如何接收請求。
5. **看 `TransferService`**：理解真正的轉帳規則在哪裡。
6. **看 `SecurityConfig`**：理解哪些功能要登入、哪些只有管理員能做。
7. **最後才看 Docker、CI、Flyway 與 Testcontainers**：這些是讓專案更可靠、更容易部署的工具。

### 第一次實作時，你應該觀察什麼？

| 你做的動作 | 背後發生的事 |
| --- | --- |
| 註冊 | 後端把帳號與「加密後的密碼」寫進 `app_users`。 |
| 登入 | 後端比對密碼，成功後回傳 JWT。 |
| 看帳戶 | 後端依 JWT 的使用者名稱，只找屬於你的帳戶。 |
| 轉帳 | 後端檢查帳戶所有權、餘額，並同時更新兩個帳戶與兩筆紀錄。 |
| 管理員開戶 | 後端建立帳戶、指定使用者，並寫入開戶紀錄。 |

### 卡住時的原則

1. **先看錯誤訊息，不要立刻亂改程式。**
2. 先確認後端是否啟動，再確認資料庫是否連得上。
3. 前端出現 `Failed to fetch`，優先想 CORS 或 API 尚未啟動。
4. 看到 401，通常是沒有登入或 token 過期；看到 403，通常是權限不足。
5. 一次只改一小段，改完就測試。

---

## 1. 專案要解決什麼問題？

以「使用者從 A001 轉 100 元到 B001」為例，真正需要保護的是：

1. 只有 A001 的擁有者可以發起扣款。
2. A001 餘額不足時，不能產生部分扣款。
3. A001 扣款、B001 入帳、兩筆交易紀錄必須一起成功或一起失敗。
4. 多個請求同時轉帳時，不能讓餘額被重複扣掉。
5. 交易完成後必須能追查資金變化。

本專案把這些規則集中在後端，而不是相信前端按鈕是否有被隱藏。

---

## 2. 系統總覽

```text
瀏覽器
  │ HTTPS / JSON
  ▼
React 前端（Vercel）
  │ Authorization: Bearer <access token>
  ▼
Spring Boot API（Railway）
  ├─ SecurityConfig：判斷公開、一般使用者、管理員路由
  ├─ JwtAuthenticationFilter：驗證 JWT，建立登入身分
  ├─ Controller：接收 HTTP、驗證輸入、回傳 JSON
  ├─ Service：放商業規則與資料庫交易邊界
  └─ Repository：使用 JPA 查詢 MySQL
       │
       ▼
MySQL（Railway／本機 Docker）
```

### 三個 repository 的分工

| Repository | 內容 | 用途 |
| --- | --- | --- |
| `bank-api` | Spring Boot 後端、MySQL migration、測試 | 本 README 所在專案 |
| `bank-web` | React / Vite 前端 | 使用者登入、查帳、轉帳、管理員開戶 |
| `bank-stack` | Docker Compose | 本機一次啟動 MySQL、後端、前端 |

---

## 3. 功能清單

### 一般使用者

- 註冊帳號，密碼只會儲存 BCrypt 雜湊值。
- 登入取得 access token 與 refresh token。
- 只能查看自己被指派的帳戶與交易紀錄。
- 只能從自己擁有的帳戶轉帳。
- 交易紀錄可依日期範圍與分頁查詢。
- Access token 過期後可使用 refresh token 換取新 token。
- 登出時撤銷 refresh token。

### 管理員

- `ROLE_ADMIN` 可建立新帳戶並指定擁有者。
- 開戶時自動建立一筆 `OPEN_ACCOUNT` 稽核紀錄。

### 開發與維運

- Swagger UI / OpenAPI 文件。
- 全域錯誤 JSON 格式。
- 單元測試、Testcontainers MySQL 整合測試與 GitHub Actions CI。
- Flyway 建立雲端空白資料庫的 schema 與展示資料。
- `/actuator/health` 健康檢查端點。

---

## 4. 資料模型

```text
app_users 1 ──── * accounts 1 ──── * account_transactions
     │
     └────────── * refresh_tokens
```

| 資料表 | 作用 |
| --- | --- |
| `app_users` | 使用者名稱、BCrypt 密碼雜湊、`ROLE_USER`／`ROLE_ADMIN` |
| `accounts` | 帳號、戶名、餘額、擁有者 `user_id` |
| `account_transactions` | 開戶、存提款與轉帳的稽核紀錄 |
| `refresh_tokens` | refresh token 的 SHA-256 雜湊、到期與撤銷狀態 |

一筆轉帳會寫入兩筆紀錄：

```text
A001 ── TRANSFER_OUT ──> B001
B001 ── TRANSFER_IN  ──> A001
```

---

## 5. API 與權限

| 方法 | 路徑 | 說明 | 權限 |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | 註冊使用者 | 公開 |
| POST | `/api/auth/login` | 登入並取得 token 組 | 公開 |
| POST | `/api/auth/refresh` | 輪替 refresh token | 公開 |
| POST | `/api/auth/logout` | 撤銷 refresh token | 公開 |
| GET | `/api/accounts` | 取得我的帳戶 | JWT |
| GET | `/api/accounts/{accountNumber}` | 取得我的單一帳戶 | JWT + 所有權 |
| GET | `/api/accounts/{accountNumber}/transactions` | 取得我的交易紀錄 | JWT + 所有權 |
| POST | `/api/transfers` | 從自己的帳戶轉帳 | JWT + 所有權 |
| POST | `/api/admin/accounts` | 建立並指派帳戶 | `ROLE_ADMIN` |
| GET | `/actuator/health` | 健康檢查 | 公開 |

受保護 API 的 Request Header：

```text
Authorization: Bearer <accessToken>
```

### 註冊與轉帳範例

```json
POST /api/auth/register

{
  "username": "alice_demo",
  "password": "PracticePassword123!"
}
```

```json
POST /api/transfers

{
  "fromAccountNumber": "A001",
  "toAccountNumber": "B001",
  "amount": 100.00
}
```

---

## 6. 轉帳為什麼安全？

轉帳邏輯位於 `TransferService`，並以 `@Transactional` 包住整個流程。

```text
1. 檢查來源與目的帳戶不同
2. 以固定帳號順序取得兩個帳戶的 PESSIMISTIC_WRITE 鎖
3. 驗證登入者確實擁有轉出帳戶
4. 驗證餘額足夠
5. 扣除來源帳戶餘額
6. 增加目的帳戶餘額
7. 寫入 TRANSFER_OUT 與 TRANSFER_IN 兩筆稽核紀錄
8. 全部成功才提交；任一步失敗則全部 rollback
```

### `@Transactional`

它保證一段資料庫操作具有「全有或全無」的效果。若入帳失敗，扣款也會回滾，不會留下錢消失、卻沒有入帳的狀況。

### `PESSIMISTIC_WRITE`

轉帳時鎖定帳戶資料列，避免兩個請求同時讀到相同餘額並各自扣款。兩個帳戶按照帳號字母順序上鎖，也可降低 A→B 與 B→A 同時轉帳造成死鎖的機率。

> 大型金融系統仍會視吞吐量與一致性需求，選擇樂觀鎖、事件流或帳本模型等不同架構；本專案選擇容易理解且正確的教學做法。

---

## 7. 認證與授權

### 密碼

註冊時以 BCrypt 轉成雜湊後才寫入資料庫；登入時用 `matches` 比對。後端無法「讀回」原密碼，因此忘記密碼需要另做重設流程。

### Access token 與 refresh token

| 類型 | 用途 | 預設期限 |
| --- | --- | --- |
| Access token | 呼叫受保護 API | 30 分鐘 |
| Refresh token | 換取新的一組 token | 7 天 |

Refresh token 每次使用後會撤銷舊 token 並發出新 token，稱為 **token rotation**。資料庫只保存其 SHA-256 雜湊。

### JWT Filter 與帳戶所有權

`JwtAuthenticationFilter` 驗證 `Authorization: Bearer ...` 後，把使用者名稱和角色放入 Spring Security 的 `SecurityContext`。Controller 用 `Authentication` 取得登入者身分，再依使用者名稱限制資料庫查詢。

所以：前端隱藏按鈕不等於安全；後端每次查帳與轉帳仍會檢查所有權。查別人的帳戶回傳 404，從別人的帳戶扣款回傳 403。

---

## 8. 統一錯誤回應

`GlobalExceptionHandler` 將商業規則與欄位驗證失敗統一成 JSON。前端可以直接顯示 `message`，不必解析 HTML 錯誤頁。

```json
{
  "timestamp": "2026-09-17 14:30:22",
  "status": 409,
  "error": "Conflict",
  "message": "使用者名稱已被使用",
  "path": "/api/auth/register"
}
```

| 狀態碼 | 情境 |
| --- | --- |
| 400 | 輸入不合法、餘額不足、轉入與轉出帳戶相同 |
| 401 | 未登入、JWT 無效、refresh token 過期 |
| 403 | 沒有轉出帳戶的權限，或非管理員呼叫管理 API |
| 404 | 帳戶不存在，或帳戶不屬於登入者 |
| 409 | 使用者名稱或帳號已存在 |

---

## 9. 本機啟動

### 方式 A：直接用本機 MySQL

先在 MySQL Workbench 依序執行：

1. [database/schema.sql](database/schema.sql)：建立 `bank_demo`、帳戶與交易範例資料。
2. [database/user_schema.sql](database/user_schema.sql)：建立使用者表。
3. [database/refresh_token_schema.sql](database/refresh_token_schema.sql)：建立 refresh token 表。
4. 註冊使用者後，執行 [database/assign_demo_account.sql](database/assign_demo_account.sql)：把 A001 指派給使用者。

接著在 PowerShell 的 `bank-api` 根目錄設定執行期間環境變數：

```powershell
$securePassword = Read-Host "MySQL root password" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $securePassword).Password

$randomBytes = New-Object byte[] 32
$rng = New-Object System.Security.Cryptography.RNGCryptoServiceProvider
$rng.GetBytes($randomBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($randomBytes)

.\mvnw.cmd spring-boot:run
```

後端會在 [http://localhost:8080](http://localhost:8080) 啟動。

### 方式 B：Docker Compose（推薦完整體驗）

請改到 `bank-stack` repository，建立 `.env` 後執行：

```powershell
docker compose up --build
```

Docker Compose 會建立 MySQL、後端與前端。前端通常在 `http://localhost:8081`；實際 port 以 `bank-stack` 的設定為準。

---

## 10. Swagger UI 操作方式

本機啟動後開啟：[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

1. 呼叫 `POST /api/auth/register` 建立使用者。
2. 呼叫 `POST /api/auth/login`，複製回傳中的 `accessToken`。
3. 點 Swagger 右上角 **Authorize**。
4. 只貼 access token 本身；不要自行加上 `Bearer `。
5. 再測試帳戶、交易紀錄與轉帳 API。

---

## 11. 雲端部署

### Railway：Spring Boot + MySQL

Railway 需設定下列環境變數：

| 變數 | 用途 |
| --- | --- |
| `PORT` | 平台指定的服務 port |
| `DB_USERNAME` / `DB_PASSWORD` | MySQL 應用程式帳密 |
| `SPRING_DATASOURCE_URL` | Railway 內網 MySQL JDBC URL |
| `JWT_SECRET` | JWT 簽章密鑰；至少 32 字元，絕不放進 Git |
| `CORS_ALLOWED_ORIGINS` | 允許的 Vercel 前端網址，以逗號分隔 |
| `FLYWAY_ENABLED=true` | 空白雲端資料庫啟動 migration |

Flyway 的 [V1__initial_bank_schema.sql](src/main/resources/db/migration/V1__initial_bank_schema.sql) 負責首次部署時建立資料表與展示資料。本機 Docker 則維持使用 `database/*.sql` 初始化。

### Vercel：React 前端

Vercel 的前端環境變數：

```text
VITE_API_BASE_URL=https://your-railway-api.up.railway.app/api
```

這是公開 API 網址，不是密碼，因此應建立為 **Config** 而非 Secret。Vite 只有 `VITE_` 前綴的變數才會在建置時提供給瀏覽器。

### CORS 為什麼重要？

瀏覽器把 Vercel 網站與 Railway API 視為不同來源。後端必須明確允許前端網址，否則前端常會看到 `Failed to fetch`。

```text
CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app
```

不要使用 `*`，因為帶有 Authorization header 的受保護 API 應只允許已知前端來源。

---

## 12. 健康檢查與常見排錯

部署成功後可開啟：

```text
https://your-railway-api.up.railway.app/actuator/health
```

預期回應：

```json
{ "status": "UP" }
```

### 為什麼 `/actuator/health` 可能回傳 403？

健康檢查是在提交 `a35af8c` 後加入的。若 Railway 仍跑舊版程式，Spring Security 會把 `/actuator/health` 視為一般受保護路徑並回傳 403。

請依序確認：

1. 本機已執行 `git push`，且 GitHub 有 `a35af8c` 或更新的提交。
2. Railway 的最新部署來源提交已更新。
3. Railway 部署狀態為 `Online`。
4. 再重新開啟健康檢查網址。

| 現象 | 常見原因與處理 |
| --- | --- |
| 前端顯示 `Request failed (404)` | Vercel 沒有設定或沒有重新部署 `VITE_API_BASE_URL`。 |
| 前端顯示 `Failed to fetch` | `CORS_ALLOWED_ORIGINS` 未包含目前 Vercel 網址，或 API 尚未 Online。 |
| 登入成功但帳戶清單空白 | 使用者尚未被指派帳戶；請用管理員建立帳戶並指定擁有者。 |
| Docker 指令不存在 | 安裝並啟動 Docker Desktop，確認 `docker info` 有 Server 區塊。 |
| Testcontainers 測試被跳過或失敗 | Docker Desktop 沒啟動，或第一次下載 MySQL image 尚未完成。 |

---

## 13. 測試與 CI

在 `bank-api` 根目錄執行：

```powershell
.\mvnw.cmd test
```

| 測試 | 類型 | 驗證內容 |
| --- | --- | --- |
| `TransferServiceTest` | 單元測試 | 帳戶所有權、餘額不足、成功轉帳規則 |
| `TransferPersistenceIntegrationTest` | Testcontainers 整合測試 | 真正 MySQL 中的餘額與兩筆交易紀錄 |
| `AuthPersistenceIntegrationTest` | Testcontainers 整合測試 | 註冊、登入、JWT／refresh token、重複帳號錯誤、健康檢查 |

Testcontainers 會啟動暫時的 MySQL 8 容器，結束後自動清除；它不會修改本機 `bank_demo` 或 Railway 資料庫。

每次推送到 `main` 或建立 Pull Request 時，GitHub Actions 的 **Backend CI** 都會在 Linux runner 中執行 Maven 測試。前端 repository 的 **Frontend CI** 則會執行 lint 與 production build。

---

## 14. 程式碼閱讀地圖

建議依下列順序閱讀：

1. `SecurityConfig`：哪些路徑公開、哪些需要登入、哪些需要管理員。
2. `JwtAuthenticationFilter` 與 `JwtService`：JWT 如何變成 Spring 的登入身分。
3. `AuthService` 與 `RefreshTokenService`：註冊、登入、token rotation。
4. `AccountController`：如何用登入者身分限制查詢範圍。
5. `TransferService`：交易、悲觀鎖與稽核紀錄的核心。
6. `GlobalExceptionHandler`：如何維持一致的錯誤 API 合約。
7. `src/test`：如何用單元測試與真實 MySQL 容器驗證規則。
8. `.github/workflows/backend-ci.yml`：如何在 GitHub 自動跑測試。

---

## 15. 下一步可以怎麼延伸？

- 忘記密碼與重設密碼流程（需 email 或驗證碼機制）。
- 管理員帳戶的安全初始化流程，避免直接用 SQL 升權。
- 前端改用 HttpOnly Cookie 保存 refresh token。
- 加入轉帳收款人白名單、每日額度與轉帳確認機制。
- 加入監控、日誌集中化與告警。
- 使用分支保護規則，要求 CI 通過後才能合併 Pull Request。

---

## 學習重點總結

```text
資料庫設計
  + Spring JPA
  + 資料庫交易與鎖
  + JWT 認證與角色授權
  + 統一錯誤格式
  + 自動化測試與 CI
  + Docker 與雲端部署
  = 可展示、可持續維護的全端作品
```

如果你能說清楚「為什麼轉帳需要 transaction、為什麼不能只相信前端、為什麼密碼只存雜湊、為什麼 CI 要跑 Testcontainers」，就不只是完成了一個功能，而是理解了後端系統設計的核心。
