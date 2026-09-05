# Gigafix

> 本專案僅供「Ispn資轉國際 ─ AI輔助學習-跨域Java軟體工程師就業養成班」期末發表使用
> 不用於商業用途，如有侵權敬請告知

Gigafix 機不可失 ─ 是一個以中小型二手手機行業者為目標客群，提供網頁式RMS(Retail Management System, 零售管理系統)專案

本專案採訪了相關業者了解使用者需求，發現中小型二手手機行會仰賴各自的小型庫存管理系統作為核心營運工具，因二手手機行是透過管理收及賣出價以獲得其商業獲利空間
本團隊汰換二手手機行內部系統升級為網頁式RMS，使其功能性不在單只有庫存管理，而是更加貼近業者商業行為需求

Gigafix 拆分為五大系統，每個系統都有對應的前台（顧客使用）與後台（店家管理）介面：

| 系統                        | 對應 RMS 功能    | 前台（顧客）           | 後台（店家）                                             |
| --------------------------- | ---------------- | ---------------------- | -------------------------------------------------------- |
| **會員系統**<br>member      | CRM 顧客關係管理 | 註冊/登入/會員中心     | 會員列表查詢、篩選、編輯、刪除，會員成長曲線與地區分布圖 |
| **商品系統**<br>product     | 商品/庫存管理    | 商品瀏覽、篩選、搜尋   | 商品新增/編輯/刪除、進階篩選、JSON 匯入匯出              |
| **訂單系統**<br>cart, order | 銷售/收銀管理    | 加入購物車、結帳       | 訂單建立、出貨、狀態管理                                 |
| **維修系統**<br>repair      | 售後服務管理     | 線上預約維修           | 維修單狀態管理、分店管理、技師管理                       |
| **討論區系統**<br>forum     | 社群/行銷經營    | 發文、留言、按讚、收藏 | 文章/分類管理、檢舉處理                                  |

## 網站前後台畫面

### 前台首頁

![前台首頁](docs/images/gigafix_user_UI.png)

### 後台：會員管理

![後台會員管理](docs/images/gigafix_admin_UI.png)

## 系統架構

Gigafix 是前後端分離的三層式架構：前端 SPA 透過 REST API 呼叫後端，後端處理商業邏輯後經 JDBC 存取資料庫。

![系統架構圖](docs/images/gigafix_system_model.png)

### 前端

以 SPA 呈現前台（顧客）與後台（店家管理）兩套介面，透過 RESTful API 與後端溝通，畫面路由藉由 Vue Router 管理

### Web Server

由 Nginx 提供前端打包後的靜態頁面，並把 `/api/*` 的請求反向代理到後端，讓瀏覽器端只需面對單一 origin (避免CORS)

### 後端

提供 RESTful API，依模組（會員、商品、訂單、維修、討論區）處理商業邏輯，並以 JWT/session 做身份驗證/授權

### 資料庫

儲存會員、商品、訂單、維修、討論區等所有業務資料，Schema 由 Hibernate（JPA）依 Entity 自動建立/更新

## 使用技術

- **前端**：Vue 3.5、Vite 8、Pinia、Vue Router、Bootstrap 5
- **後端**：Java 21、Spring Boot 4.1、Spring Data JPA、Spring Security、JWT
- **資料庫**：Microsoft SQL Server 2022
- **Web Server**：Nginx（提供前端靜態頁面，並反向代理 `/api/*` 到後端）
- **CI/CD**：Jenkins
- **容器化/部署**：Docker、Docker Compose
- **版本控制**：Git、GitHub

## 系統需求

- **用 Docker 跑（推薦）**：Docker、Docker Compose
- **不用 Docker、本機分開跑前後端**：JDK 21、Maven（或用專案內的 `mvnw`）、Node.js（版本需求見 `gigafix-frontend/package.json` 的 `engines`）

## 本機啟動方式（Docker Compose）

1. 在專案根目錄建立 `.env`，填入以下變數：
   - `MSSQL_SA_PASSWORD`：資料庫 sa 密碼
   - `JWT_SECRET`：後端簽發 JWT 用的密鑰
   - `GIGAFIX_GMAIL_USERNAME` / `GIGAFIX_GMAIL_PASSWORD`：寄送 Email（OTP、忘記密碼等）用的 Gmail 帳密
2. 啟動所有服務：
   ```bash
   docker compose up -d --build
   ```
3. 開啟瀏覽器：
   - 前台網站：http://localhost
   - 後端 API：http://localhost:8080

## 本機啟動方式（不用 Docker，前後端分開跑）

適合想直接進 IDE debug、不想每次改程式都重新 build image 的情境。

1. **啟動資料庫**：可以只用 `docker compose up -d db db-init`，讓 MSSQL 跑在容器裡，前後端則在本機直接執行
2. **啟動後端**：
   ```bash
   cd gigafix-backend
   ./mvnw spring-boot:run
   ```
   需要設定跟 `docker-compose.yml` 裡 `backend` 服務同樣的環境變數（`SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`、`JWT_SECRET`、`GIGAFIX_GMAIL_USERNAME`、`GIGAFIX_GMAIL_PASSWORD`），只是連線的 host 要改成 `localhost`（不是容器內的服務名稱 `db`）
3. **啟動前端**：
   ```bash
   cd gigafix-frontend
   npm install
   npm run dev
   ```
   `vite.config.js` 已經設定好把 `/api` 開發階段代理到 `http://localhost:8080`，不用另外設定 CORS

## 專案結構

```
gigafix/
├── gigafix-backend/   # Spring Boot 後端（依模組分package：member、product、cart、order、repair、forum、admin...）
├── gigafix-frontend/  # Vue 3 前端（依 features 分模組，各自有 view/component/router/store）
├── db-init/           # 資料庫初始化用的 SQL
├── docker-compose.yml # 本機用 docker compose（自行 build image）
└── Jenkinsfile        # CI/CD pipeline
```

## CI/CD

`Jenkinsfile` 定義了 4 個 stage：

1. **Checkout**：從 Git 抓取最新程式碼
2. **Build backend image**：在 `gigafix-backend` 目錄用 Dockerfile build 後端 image
3. **Build frontend image**：在 `gigafix-frontend` 目錄用 Dockerfile build 前端 image
4. **Push images**：把兩個 image 推到 DockerHub

![CI/CD架構簡圖](docs/images/cicd.png)

## 網站部署

待實做...

## 目前已知限制

- **無多租戶(multi-tenant)設計**：目前一套系統對應一間店的資料，如果要賣給多間手機行，現階段做法是各自獨立部署一套（各自的資料庫、網域），還不是共用一套服務、資料互相隔離的 SaaS 架構
- **無 API 文件**：目前沒有整合 Swagger/OpenAPI，API 規格需直接看各模組的 Controller
- **測試覆蓋率低**：後端目前只有一個空的 Spring Boot context load 測試，前端沒有自動化測試，之後可以視情況補上單元測試/整合測試
