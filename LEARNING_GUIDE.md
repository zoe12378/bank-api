# Bank API 新手學習指南

這份文件不是對外的 API 說明書，而是給第一次接觸全端專案的人看的學習筆記。目標是理解：你現在看到的畫面、後端程式、資料庫和雲端服務，究竟如何一起完成「登入、查帳、轉帳」這些功能。

## 1. 這個專案到底在做什麼？

它是一個簡化版網路銀行：使用者可以註冊、登入、查看自己的帳戶和交易紀錄，並從自己擁有的帳戶轉帳；管理員則可替指定使用者建立並指派帳戶。

真實銀行會有更多規則，例如 KYC、雙因子驗證、跨行清算、反洗錢與人工覆核。本專案不試圖模擬全部，而是先把一個後端專案最核心的安全與資料一致性觀念做好。

## 2. 先認識四個角色

| 名稱 | 這個專案中是什麼 | 負責什麼 |
| --- | --- | --- |
| 前端 | `bank-web`，React / Vite | 顯示頁面、收集輸入、呼叫 API |
| 後端 | 本 repository，Spring Boot | 驗證登入者、執行商業規則、回傳 JSON |
| 資料庫 | MySQL | 長期保存使用者、帳戶、交易與 refresh token |
| API | 前後端溝通的網址與資料格式 | 例如 `POST /api/auth/login` |

前端不是資料庫，也不該決定誰可以轉帳。前端只是提出請求；真正做權限判斷與扣款的地方必須是後端。

## 3. 從按下按鈕到資料存進資料庫

以「登入後送出一筆轉帳」為例，整條路徑如下：

```text
瀏覽器中的 React 畫面
  → 呼叫 HTTPS API，帶上 JSON 與 JWT
  → Spring Security 驗證 JWT
  → Controller 接收請求
  → Service 檢查所有權、餘額與轉帳規則
  → Repository / JPA 對 MySQL 讀寫資料
  → 後端回傳 JSON
  → 前端重新讀取帳戶與交易紀錄並更新畫面
```

在雲端環境中，React 前端部署在 Vercel，Spring Boot API 部署在 Railway，MySQL 也在 Railway。這三個服務透過網路連線，不會因為在不同平台就自動互通，所以必須設定 API URL、資料庫連線字串與 CORS。

## 4. 後端為什麼要分層？

請把後端想成一間銀行櫃台：

| 程式層 | 比喻 | 應負責的事 |
| --- | --- | --- |
| Controller | 櫃台窗口 | 收 HTTP 請求、檢查輸入格式、回傳 HTTP 回應 |
| Service | 真正處理業務的行員 | 轉帳規則、權限、交易邊界、錯誤處理 |
| Repository | 查帳系統 | 用 JPA 與 MySQL 讀寫資料 |
| Entity | 資料表的 Java 表示 | 對應 `app_users`、`accounts` 等資料表 |
| DTO | 表單／收據格式 | 定義 API 收到和回傳的 JSON，不直接暴露 Entity |

常見程式位置：

```text
src/main/java/bank_api/
├─ auth/          註冊、登入、JWT、refresh token
├─ account/       帳戶查詢與管理員開戶
├─ transaction/   交易紀錄
├─ transfer/      轉帳規則
├─ security/      Spring Security、CORS、JWT filter
└─ common/        共用錯誤格式與例外處理
```

初學時最值得先讀的順序是：`SecurityConfig` → `AuthController` → `AuthService` → `TransferController` → `TransferService` → 對應的 Entity/Repository。每讀一個檔案，都問自己：「它接收什麼？呼叫誰？最後改了哪一筆資料？」

## 5. 四條最重要的功能流程

### 註冊

1. 前端送出 username 和 password 到 `POST /api/auth/register`。
2. `AuthService` 檢查使用者名稱是否重複。
3. 密碼先經 BCrypt 雜湊，再寫入 `app_users.password_hash`。
4. 回傳新使用者資訊；資料庫不會保存明碼密碼。

### 登入與 JWT

1. 前端送 username/password 到 `POST /api/auth/login`。
2. 後端用 BCrypt 比對輸入密碼與資料庫雜湊。
3. 驗證成功後，後端簽發短效的 access token 和較長效的 refresh token。
4. 前端以 `Authorization: Bearer <access token>` 呼叫受保護 API。
5. `JwtAuthenticationFilter` 驗證 token，Spring Security 才知道目前是誰。

JWT 不是密碼，也不是加密後的使用者資料。它是由伺服器簽名的身分憑證；`JWT_SECRET` 外洩時，任何人都可能偽造 token，因此絕不能放在 GitHub 或前端環境變數。

### 查詢帳戶

使用者呼叫 `GET /api/accounts` 時，後端先從 JWT 取得 username，再只回傳該使用者被指派的帳戶。即使有人在瀏覽器手動改網址或 JSON，後端仍會再次判斷所有權。

### 轉帳

一筆轉帳不是單純把兩個數字加減：

1. 從 JWT 找出目前登入者。
2. 確認來源帳戶存在且屬於登入者。
3. 確認金額大於 0、來源和目標帳戶不同、餘額足夠。
4. 鎖住兩個帳戶資料列，避免同時兩筆請求各自看到舊餘額。
5. 扣除來源餘額、增加目標餘額。
6. 寫入 `TRANSFER_OUT` 與 `TRANSFER_IN` 兩筆稽核紀錄。
7. 任一步失敗時，由 `@Transactional` 讓全部操作回滾。

這就是為什麼交易規則放在 `TransferService`，而不是寫在 React 前端或 Controller。

## 6. 資料庫要看懂什麼？

```text
app_users 1 ──── * accounts 1 ──── * account_transactions
     │
     └────────── * refresh_tokens
```

- `app_users`：帳號、密碼雜湊、角色。
- `accounts`：帳號、餘額、擁有者 `user_id`。一個使用者可有多個帳戶。
- `account_transactions`：每次開戶、存提款、轉帳後留下的歷史紀錄。這是稽核資料，不應隨便修改或刪除。
- `refresh_tokens`：保存 refresh token 的雜湊、到期時間與撤銷狀態。

資料表 SQL 在 `database/`；雲端初始結構與資料由 `src/main/resources/db/migration/` 的 Flyway migration 管理。要注意：修改已經在雲端執行過的 migration 不是好習慣，應新增下一個版本的 migration。

## 7. 本機如何啟動？

有兩種方式，學習時先選一種即可。

### 方式 A：各自啟動

1. 在 MySQL 建立 `bank_demo` 資料庫並執行 `database/` 的 schema。
2. 在 PowerShell 設定 `DB_PASSWORD` 和至少 32 字元的 `JWT_SECRET`。
3. 在本 repository 執行 `./mvnw.cmd spring-boot:run`。
4. 在 `bank-web` 執行前端開發伺服器。

後端預設是 `http://localhost:8080`，前端開發伺服器通常是 `http://localhost:5173`。

### 方式 B：Docker Compose

`bank-stack` repository 會一次啟動 MySQL、API 和前端。先把 `.env.example` 複製成 `.env`，填入你自己的本機密碼和 JWT 密鑰，再執行 Compose。

`.env` 已被 Git 忽略；不要把真正的密碼、token 或 Railway 變數貼進 commit、截圖或 README。

## 8. 雲端部署時，三個設定最容易出錯

### 前端的 API URL

Vercel 需要 `VITE_API_BASE_URL` 指向 Railway API，例如：

```text
https://你的-api.up.railway.app/api
```

它是前端必須知道的公開網址，所以可設為 Vercel 的 Config；不要把 JWT secret 或資料庫密碼設成 `VITE_` 開頭，因為那類值會被打包進瀏覽器。

更改 Vercel 環境變數後必須重新部署，舊的前端 bundle 不會自動取得新值。

### 後端 CORS

瀏覽器會阻擋不同網域的前端直接呼叫 API，除非 API 明確允許。Railway 的 `CORS_ALLOWED_ORIGINS` 必須包含 Vercel 實際使用的網址，且完整比對協定與網域，例如：

```text
https://bank-web-git-main-zoe12378s-projects.vercel.app
```

不要把 `*` 當成登入 API 的正式解法；需要帶 Authorization header 的系統應限制可信任網域。

### Railway 的資料庫變數

後端會需要資料庫 URL、使用者與密碼，以及 `JWT_SECRET`。Railway 可用服務間的參照變數連接 MySQL；不要把本機的 `localhost` 當成 Railway 資料庫主機，因為 Railway 容器中的 `localhost` 指向容器自己。

## 9. 常見錯誤：從症狀找方向

| 看到的現象 | 常見原因 | 先檢查什麼 |
| --- | --- | --- |
| `Failed to fetch` | CORS、API 網址錯誤或 API 未啟動 | Vercel 的 `VITE_API_BASE_URL`、Railway deployment、`CORS_ALLOWED_ORIGINS` |
| 404 | API 路徑或環境變數錯誤 | URL 是否包含 `/api`、Swagger 是否有該端點 |
| 401 Unauthorized | 沒帶 token、token 過期或登入失敗 | 重新登入、確認 Authorization header |
| 403 Forbidden | 已登入但沒有權限或不擁有帳戶 | 帳戶 `user_id`、角色是否為 `ROLE_ADMIN` |
| 帳戶清單空白 | 新使用者尚未被指派任何帳戶 | 以管理員建立帳戶並指定擁有者 |
| 500 | 後端程式或資料不符合預期 | 直接查看 Railway logs 或本機後端終端機的第一個例外 |
| `/actuator/health` 為 403 | 線上服務尚未部署包含 health 設定的新版 | 確認最新 commit 已推送、Railway 已完成新 deployment |

除錯原則：先看 HTTP status，再看後端 log 中最早出現的 `Caused by` 或資料庫錯誤；畫面最下方的一句錯誤通常只是結果，不是根因。

## 10. 測試是在保護什麼？

- 單元測試：快速驗證一個 Service 的規則，例如餘額不足不能轉帳。
- 整合測試：啟動接近真實的 Spring Boot 與 MySQL，驗證 Controller、Security、JPA 和 SQL 是否能一起工作。
- Testcontainers：測試時用 Docker 啟動乾淨的 MySQL，不依賴你電腦上剛好有什麼資料。
- GitHub Actions：每次 push 時，自動執行檢查，避免明顯錯誤直接進主分支。

如果 Testcontainers 無法執行，先確認 Docker Desktop 正在運行；這是因為它真的需要容器來建立測試用 MySQL。

## 11. 建議的閱讀與練習順序

1. 先用前端完成一次註冊、登入、查帳、轉帳。
2. 在 Swagger UI 重做同一個流程，觀察每個請求的 URL、header 與 JSON。
3. 讀 `SecurityConfig` 和 `JwtAuthenticationFilter`，理解為何沒 token 是 401。
4. 讀 `TransferService`，畫出扣款、入帳、兩筆交易紀錄與回滾關係。
5. 在 MySQL 查 `accounts` 和 `account_transactions`，比對畫面結果。
6. 刻意輸入錯誤金額、別人的來源帳戶或不存在的帳號，觀察 400、403、404 的差異。
7. 最後閱讀 Flyway migration、Docker Compose 和 GitHub Actions，理解從本機到雲端的流程。

## 12. 可以自己嘗試的下一步

- 新增「修改密碼」功能，要求舊密碼驗證與 BCrypt 重新雜湊。
- 為交易紀錄加入交易備註與查詢條件，並建立新的 Flyway migration。
- 加入帳戶凍結欄位，讓凍結帳戶不能轉出。
- 寫一個測試，模擬兩筆同時扣款並確認餘額不會變成負數。
- 加入 rate limiting、登入失敗次數限制與 audit log，思考安全性和使用便利性的取捨。

這份專案最重要的不是背下每個 annotation，而是每次新增功能時都能回答：資料從哪裡來？誰有權操作？失敗後資料會不會留下半套結果？以及祕密是否被安全保存？
