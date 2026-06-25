# WholeSummer 補習班管理系統

> [!IMPORTANT]
> 問題回報郵件需要先設定 Resend 環境變數。請在 Windows PowerShell 執行下列指令，
> 將 API Key 與 Email 換成自己的資料，接著完全關閉並重新啟動 WholeSummer：

```powershell
[Environment]::SetEnvironmentVariable("WHOLESUMMER_REPORT_ENABLED", "true", "User")
[Environment]::SetEnvironmentVariable("RESEND_API_KEY", "re_新的API金鑰", "User")
[Environment]::SetEnvironmentVariable("WHOLESUMMER_REPORT_FROM", "WholeSummer <onboarding@resend.dev>", "User")
[Environment]::SetEnvironmentVariable("WHOLESUMMER_REPORT_RECIPIENT", "你的Resend註冊Email", "User")
```

請勿將 API Key 寫入 GitHub、`src/main/resources/application.properties` 或公開安裝包。

WholeSummer 是一套以 Spring Boot 與 MySQL 建立的補習班管理系統，提供學生、教師、班級、科目、測驗、作業、出勤、薪資與資料備份等日常教務功能。

系統以瀏覽器作為操作介面，可在本機或區域網路中使用；Windows 正式版本可透過安裝程式部署，首次啟動時會協助完成 MySQL 連線與資料庫初始化。

目前版本：**1.1.1**

## 1.1.1 更新內容-2

- 導入 Flyway 統一管理資料庫結構與版本更新，Hibernate 改為只驗證 Entity 與資料表是否一致。
- 新版程式啟動時會自動執行尚未套用的 migration，不會清空或覆蓋既有資料。
- 新增完整資料庫初始結構、教師權限初始化、操作紀錄及後續欄位調整 migration。
- 新增資料庫操作紀錄，保存資料異動時間、操作教師、動作、路徑與執行結果，並提供主任查看。
- 登入帳號與密碼全面區分大小寫；帳號欄位改用 case-sensitive collation，登入查詢使用精確比對。
- 主任薪資頁可展開或收合每位教師的當月打卡明細，一般教師只能查看自己的資料。
- 打卡明細新增對應課程、課程時間、計入時數與備註。
- 打卡工時依教師、日期及打卡時間與課表重疊結果自動配對，同時段多個班級不重複計算。
- 無對應課程且未經主任調整的打卡紀錄暫不計入工時與薪資。
- 主任可對無對應課程的紀錄補充備註與上課時數，儲存後立即重新計算當月累計工時與薪資。
- 教師時薪完全改為按教師、年份及月份獨立保存，不再於教師主檔保留固定時薪。
- 移除班級與科目的舊教師名稱字串，以及操作紀錄中已停用的帳號字串欄位。
- 修正非授權教師可能看到設定頁受限區塊內容的問題。

## 1.1.1 更新內容

- 新增學生生日欄位，可於學生新增、編輯及詳細資料頁維護。
- 首頁新增今日生日通知，顯示當天生日的在學學生。
- 新增學生學費管理，可記錄應繳、已繳、未繳、繳費期限與逾期狀態。
- 新增主任專用一鍵升年級流程，分階段處理學生年級、畢業狀態、班級升級及新班學生名單。
- 國三學生升高一時必須輸入新學校，完成升級後同步更新學生年級與學校。
- 高三學生執行一鍵升年級後自動設為已畢業。
- 升級班級時保留原班歷史資料，並複製科目、教師、班別、說明及所有上課時段至新班。
- 設定頁各功能改為可收合區塊，減少長頁面的視覺雜亂。
- 新增教師個別權限設定，主任可分別授予一般設定、註冊安全碼、新增教師、職位管理、全體出勤、全體薪資、學費、系統更新、資料庫備份及一鍵升年級權限。
- 主任固定擁有全部權限；其他教師的頁面入口與後端操作會依授權結果開放。
- 首頁「即將到期作業」改為通知中心，以可橫向捲動的方塊顯示通知。
- 通知中心整合作業到期、未來測驗、逾期學費、版本更新及學生生日，並以不同顏色區分種類。
- 逾期學費通知只向具備學費管理權限的教師顯示，版本更新通知只向主任顯示。
- 首頁新增薪資查詢與系統設定入口，學費入口則依教師權限顯示。
- Windows 啟動時若未設定 Resend API Key 或開發者 Email，會顯示郵件設定視窗；收件 Email 預設帶入 `aassddlee0924@gmail.com`。

## 1.1.0 更新內容

- 完整優化手機與平板的學生、教師、班級、科目、測驗、作業、設定及薪資頁面。
- 手機版表格隱藏次要資訊，日期與操作按鈕依可用空間重新排列。
- 平板直向與橫向分別套用響應式排版，保留接近桌面版的操作方式。
- 詳細頁在手機支援區塊展開與收合，統計資料改為單欄顯示。
- 上課時間表支援橫向捲動、多日課程時間分行及響應式匯出工具列。
- 手機版資訊提示與操作確認改用適合觸控的浮動面板。
- 新增問題與 Bug 回報，可保存本機紀錄、透過 Resend 通知開發者及重新寄送。
- 新增自訂 Spring 設定 metadata，改善 Eclipse 對 `app.*` 屬性的提示。
- 一般教師只能為自己快速打卡，主任可管理所有教師出勤。
- 教師新增時可選擇職位，每月時薪改以整數儲存。

## 主要功能

- **帳號與權限**
  - 教師註冊與個人帳號登入
  - 登入帳號與密碼皆區分英文字母大小寫
  - 教師、輔導老師、主任三種職位
  - 主任可為每位教師設定個別管理權限
  - 頁面入口與後端操作依教師權限同步限制
- **學生管理**
  - 中文名、英文名、生日與基本資料
  - 在學、畢業、復學及刪除
  - 個人成績、作業與出缺席紀錄
- **教師管理**
  - 教師資料、任教狀態、授課科目與班級
  - 上下班、請假及出勤統計
  - 依當日課程自動計算工時與遲到狀態
- **班級與課表**
  - 年級、科目、教師、班別及多個上課時段
  - 班級學生、點名、測驗及作業整合
  - 星期課表與 JPEG、DOCX、XLSX 匯出
- **科目管理**
  - 適用年級複選
  - 科目與多位建議教師關聯
- **測驗與成績**
  - 計分測驗與不計分課堂練習
  - 分數或完成狀態登記
  - 平均、最高、最低、缺考與學生歷史成績
- **作業管理**
  - 班級作業建立及學生紀錄自動產生
  - 未繳交、已繳交、逾期補交、免交
  - 下次上課日、自動截止日期與首頁通知
- **薪資試算**
  - 每位教師、每月份獨立時薪
  - 依打卡紀錄所配對的實際課程時數計算薪資
  - 無對應課程的紀錄由主任補充備註與時數後才納入計算
  - 主任可展開或收合各教師的當月打卡明細
  - 教師只能查看本人薪資，主任可管理所有教師
- **學費管理**
  - 為學生建立個別應繳費用，不依賴學費方案
  - 記錄應繳、已繳、未繳金額與繳費日期
  - 自動判斷未繳、部分繳費、已繳清與逾期狀態
  - 提供全校統計及學生詳細頁個人繳費紀錄
- **系統管理**
  - 亮色、暗色及跟隨裝置模式
  - 可收合設定區塊與教師個別權限設定
  - 作業、測驗、學費、更新及生日通知中心
  - MySQL 備份、下載、還原及初始資料匯入
  - Flyway 資料庫版本管理與自動升級
  - 資料異動操作紀錄與主任查詢
  - 問題回報本機紀錄與 Resend Email 通知
  - Windows 外部設定檔與 GitHub Releases 更新

## 技術架構

| 類別 | 技術 |
| --- | --- |
| 後端 | Java 25、Spring Boot 4.1 |
| Web | Spring MVC、Thymeleaf、Bootstrap 5 |
| 資料存取 | Spring Data JPA、Hibernate、Flyway |
| 資料庫 | MySQL |
| 建置 | Maven Wrapper |
| Windows 發布 | jpackage、GitHub Actions |
| 測試 | JUnit、Spring Boot Test、MockMvc |

後端採用 Controller、Service、Repository、Entity 分層。系統設定與主要業務資料存於 MySQL，畫面模式則保存在使用者目前的瀏覽器。

## 系統需求

### Windows 正式安裝

- Windows 10 或 Windows 11
- 可連線的 MySQL Server
- MySQL 帳號具備建立 `WholeSummer` Database 的權限
- 使用備份／還原功能時，需安裝 MySQL Client 工具

Java Runtime 已包含在 jpackage 安裝內容中，不需要另外安裝 Java。

### 本機開發

- Java 25
- MySQL
- Git
- Maven 可由專案內的 Maven Wrapper 自動取得

## Windows 安裝與首次設定

從 GitHub Releases 下載：

```text
WholeSummer-Windows-Installer-v{version}.exe
```

安裝後第一次啟動時，系統會：

1. 在 `%ProgramData%\WholeSummer` 建立外部資料目錄。
2. 詢問 MySQL 主機、Port、帳號、密碼及系統 Port。
3. 測試 MySQL 連線。
4. 檢查是否存在 `WholeSummer` Database。
5. 必要時自動建立 Database。
6. 詢問是否選擇 `.sql` 備份檔初始化資料。
7. 建立外部 `application.properties` 並啟動系統。

若使用空白資料庫，第一次進入註冊頁時需建立第一位主任及登入帳號。教師註冊安全碼預設為：

```text
whole-summer
```

主任登入後應立即在設定頁變更安全碼。

Windows 外部資料結構：

```text
%ProgramData%\WholeSummer
├─ config
│  └─ application.properties
├─ logs
├─ data
├─ backups
└─ update
```

更新安裝程式不會覆蓋上述設定、備份及資料目錄。

## 本機開發

### 1. 建立資料庫

確認 MySQL 正在執行，並建立資料庫：

```sql
CREATE DATABASE WholeSummer
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

### 2. 建立開發設定檔

在專案根目錄建立 `config/application.properties`：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/WholeSummer?serverTimezone=Asia/Taipei&characterEncoding=utf8&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_password

spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
spring.flyway.validate-on-migrate=true
spring.flyway.clean-disabled=true

spring.jpa.hibernate.ddl-auto=validate
```

此檔案已列入 `.gitignore`，請勿將資料庫密碼提交至版本控制。

也可改用環境變數：

```text
WHOLESUMMER_DB_URL
WHOLESUMMER_DB_USERNAME
WHOLESUMMER_DB_PASSWORD
```

### 3. 啟動

macOS 或 Linux：

```bash
./mvnw spring-boot:run
```

Windows：

```powershell
.\mvnw.cmd spring-boot:run
```

啟動後開啟：

```text
http://localhost:8080
```

系統預設監聽 `0.0.0.0`。若需從其他裝置連線，請確認作業系統防火牆與區域網路設定。

## 外部設定

常用設定如下：

```properties
server.port=8080
server.address=0.0.0.0

spring.datasource.url=jdbc:mysql://localhost:3306/WholeSummer?useSSL=false&serverTimezone=Asia/Taipei&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_password

app.data.dir=C:/ProgramData/WholeSummer/data
app.backup.dir=C:/ProgramData/WholeSummer/backups
app.update.dir=C:/ProgramData/WholeSummer/update
app.mysql.bin-dir=

app.auto-update.enabled=true
app.update.check-on-startup=true
app.update.check-interval-hours=24
```

新版啟動時只會補充缺少的設定，不會覆蓋既有資料庫連線資訊。

## 資料庫版本管理

WholeSummer 使用 Flyway 管理資料庫結構。Migration 位於：

```text
src/main/resources/db/migration
```

啟動時會自動檢查 `flyway_schema_history`，並依版本執行尚未套用的 migration。
全新空資料庫會從 `V1__init_schema.sql` 建立完整結構；既有非空資料庫會以版本 1
建立 baseline，再從版本 2 開始套用更新。Flyway 不會清空既有學生、教師、班級或其他資料。

目前 migration：

| 版本 | 用途 |
| --- | --- |
| V1 | 建立全新資料庫的完整初始結構 |
| V2 | 對齊教師權限資料表與權限欄位 |
| V3 | 依教師職位初始化全部權限項目 |
| V4 | 建立資料異動操作紀錄表 |
| V5 | 登入帳號改為區分大小寫，並加入打卡課程配對與人工工時欄位 |
| V6 | 移除教師主檔固定時薪，改由每月薪資資料保存 |
| V7 | 移除班級、科目與操作紀錄中的舊版字串欄位 |

Flyway 使用規則：

1. 已執行過的 `V` 開頭 migration 不可修改。
2. 資料表需要調整時，新增下一個版本的 SQL。
3. 檔名格式為 `V版本號__英文描述.sql`。
4. SQL 應盡量採用 `IF NOT EXISTS` 或其他可安全套用的寫法。
5. 正式資料庫禁止執行 `flyway clean`，專案也已設定 `spring.flyway.clean-disabled=true`。
6. 開發環境重建資料庫前，必須先確認資料可以刪除。
7. 更新 exe 或 JAR 不會覆蓋外部 `config/application.properties`，也不會刪除 MySQL 資料庫。

若 migration 失敗，系統會停止啟動；Windows 版會顯示啟動錯誤視窗，詳細原因可在
`%ProgramData%\WholeSummer\logs\wholesummer.log` 查看。不要用修改舊 SQL 的方式修正，
應新增下一個 migration。

## 備份與還原

主任或具備資料庫備份權限的教師，可在設定頁使用資料庫備份功能：

- 建立 SQL 備份
- 下載或刪除備份
- 還原既有備份
- 匯入 SQL 作為初始資料庫

還原前系統會先建立安全備份。Windows 若無法找到 MySQL 工具，可在外部設定加入：

```properties
app.mysql.bin-dir=C:/Program Files/MySQL/MySQL Server 9.0/bin
```

## 系統更新

Windows 安裝版會透過 GitHub Releases 檢查新版本：

- 啟動後在背景檢查，24 小時內最多一次
- 主任或具備系統更新權限的教師可查看及執行更新
- 新版會先下載到外部 `update` 目錄
- PowerShell updater 會等待主程式關閉、執行 installer，完成後重新啟動
- 設定檔、資料與備份不會因更新被刪除

若更新失敗，安裝檔會保留，使用者可手動執行。

## 問題回報郵件

每位登入教師都可以在設定頁提交錯誤回報、操作問題或功能建議。回報會先保存至本機
`bug_reports` 資料表，再透過 Resend 寄送給開發者。寄送失敗時可從設定頁重新寄送。

在外部 `application.properties` 設定：

```properties
app.report.mail.enabled=true
app.report.mail.api-key=re_xxxxxxxxx
app.report.mail.from=WholeSummer <reports@updates.example.com>
app.report.mail.recipient=developer@example.com
```

若目前沒有自己的網域，可先使用 Resend 測試寄件地址：

```properties
app.report.mail.enabled=true
app.report.mail.api-key=re_xxxxxxxxx
app.report.mail.from=WholeSummer <onboarding@resend.dev>
app.report.mail.recipient=你註冊 Resend 使用的 Email
```

測試寄件地址通常只能寄到 Resend 帳號擁有者自己的 Email，正好適合將所有使用者回報集中寄給
WholeSummer 開發者。若日後需要寄給其他收件人，則需加入並驗證自己的寄件網域。

也可以使用環境變數提供敏感資訊：

```text
RESEND_API_KEY
WHOLESUMMER_REPORT_ENABLED
WHOLESUMMER_REPORT_FROM
WHOLESUMMER_REPORT_RECIPIENT
```

Windows 指令已列於本文件最前方。設定後需完全關閉並重新啟動 WholeSummer。
首次初始化建立的外部設定檔會保留環境變數占位符，
不會將 API Key 寫入安裝包或 Git Repository。

`app.report.mail.from` 必須使用已在 Resend 驗證的寄件網域。API Key 不可提交至 Git，
也不應直接寫入公開安裝包。若系統會提供給無法信任的第三方使用，建議改由自有 HTTPS
回報 API 代為呼叫 Resend，避免使用者從本機設定檔取得寄信金鑰。

## 測試

執行全部測試：

```bash
./mvnw test
```

建立可執行 JAR：

```bash
./mvnw clean package
```

測試會使用設定中的 MySQL Database，執行前請確認 MySQL 可連線。整合測試可能建立並清除暫時測試資料。

## 發布 Windows 版本

專案提供 `release.sh` 協助發布：

```bash
./release.sh
```

腳本會：

1. 確認目前位於 `main` 分支。
2. 讀取 `pom.xml` 版本。
3. 在版本已存在時選擇覆蓋、變更版本或取消。
4. 建置專案、建立 commit 與 Git tag。
5. 推送至 GitHub。
6. 由 GitHub Actions 產生 Windows installer 並建立 Release。

版本格式使用：

```text
x.y.z
```

Git tag 與 `pom.xml` 版本必須一致。

## 專案結構

```text
src/main/java/com/example/cramschool
├─ config       啟動、權限、外部設定與 Windows 初始化
├─ controller   Web 請求與頁面流程
├─ dto          查詢與統計資料
├─ entity       JPA Entity 與狀態列舉
├─ form         表單輸入模型
├─ repository   Spring Data JPA Repository
└─ service      業務邏輯、備份、薪資與更新

src/main/resources
├─ db/migration Flyway 資料庫版本 SQL
├─ templates    Thymeleaf 頁面
└─ application.properties

.github/workflows
└─ build-windows.yml
```

## 安全注意事項

- 不要提交資料庫密碼、外部設定檔或 SQL 備份。
- 不要提交 Resend API Key；若金鑰疑似外洩，應立即在 Resend Dashboard 刪除並重建。
- 正式環境請變更預設教師註冊安全碼。
- 資料庫帳號應限制在必要權限範圍。
- 主任固定擁有全部管理權限，其他教師依主任授予的個別權限操作。
- 建議定期下載備份並保存到其他裝置。

## 授權

本專案目前未提供公開授權條款。未經專案擁有者授權，不得視為可自由使用、修改或散布的開源軟體。
