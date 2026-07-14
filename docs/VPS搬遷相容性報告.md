# WholeSummer VPS 搬遷相容性報告

**盤點日期：** 2026-07-14  
**盤點範圍：** WholeSummer 專案原始碼、資源、建置設定、Windows 讀卡 Client、GitHub Actions 與工作區中的本機設定檔  
**本階段：** 僅分析與新增文件；未修改正式程式碼

## 1. 結論摘要

目前系統可拆成兩個部署面：

1. **可搬到 Ubuntu VPS 的主體：** Spring Boot Web、MySQL 連線、Flyway、LINE Webhook、LINE LIFF、LINE 文字課表查詢、LINE 通知、Web Push、排程工作、資料庫備份與附件儲存。
2. **應留在補習班 Windows 現場的功能：** Swing 桌面啟動/狀態視窗、Windows 更新安裝器、Windows Card Listener、USB/Raw Input 讀卡功能、開啟本機資料夾的桌面操作。

**目前不能直接以正式 systemd 服務啟動 Ubuntu VPS 的主要原因：** `WholeSummerApplication` 啟動時無條件呼叫 Swing/AWT 啟動流程；外部設定初始化在缺少設定檔且 headless 時會直接失敗；部分更新與桌面流程明確呼叫 Windows 程式。這些問題尚未在本階段修改。

**安全性結論：** 敏感設定已有部分環境變數支援，但工作區存在被 `.gitignore` 忽略的 `config/application.properties`，其中包含資料庫密碼、LINE Secret/Token 與 VAPID 私鑰；目前未發現 Cloudflare 設定檔或 Cloudflare Token。敏感值未在本報告重複記錄。

## 2. 技術版本與建置狀態

| 項目 | 目前發現 | 證據 |
|---|---|---|
| Java | `java.version=25`；本次測試使用 Java 25.0.2 | `pom.xml:17-19`；Maven 測試輸出 |
| Spring Boot | 4.1.0 | `pom.xml:6-10` |
| Maven | 專案提供 Maven Wrapper；Windows 另有 `mvnw.cmd` | `mvnw`、`mvnw.cmd`、`.mvn/wrapper/maven-wrapper.properties` |
| MySQL JDBC | `mysql-connector-j` runtime dependency | `pom.xml:81-85` |
| Flyway | `flyway-core`、`spring-boot-starter-flyway`、`flyway-mysql` | `pom.xml:87-97` |
| Migration | `src/main/resources/db/migration/` 共 29 個 migration；本機 schema 測試時為 version 28 | Flyway 測試輸出 |
| 測試 | Spring context 曾成功啟動、連線本機 MySQL、驗證 migration；完整 `./mvnw -q test` 因既有資料庫 Lock wait timeout 未完成 | 測試輸出：MySQL 9.6、Flyway 已驗證至 9.4 的相容性警告、SQLState 40001 |
| Profile | 未發現 `application-{profile}`、`spring.profiles.active` 或 `@Profile`；測試輸出為 default profile | `src/main/resources/application.properties`、全專案搜尋 |

## 3. Windows 專屬程式碼與檔案位置

### 3.1 主 Java 專案

| 檔案 | Windows/桌面依賴 |
|---|---|
| `src/main/java/com/example/cramschool/WholeSummerApplication.java:3,14,23-24,31-63` | 無條件載入 AWT/Swing、`StartupLoadingWindow`、`SwingLookAndFeel`；headless 啟動仍會先進入這段流程 |
| `src/main/java/com/example/cramschool/desktop/StartupLoadingWindow.java` | Swing 啟動載入視窗 |
| `src/main/java/com/example/cramschool/desktop/DesktopStatusWindow.java` | Swing 狀態視窗、`Desktop.getDesktop().browse()`、結束桌面程式流程 |
| `src/main/java/com/example/cramschool/desktop/SwingLookAndFeel.java` | Swing 外觀設定 |
| `src/main/java/com/example/cramschool/desktop/CardListenerProcessManager.java` | 尋找並啟動 `WholeSummer.CardListener.exe` |
| `src/main/java/com/example/cramschool/config/FirstRunSetupDialog.java` | AWT/Swing 首次設定視窗與檔案選擇器 |
| `src/main/java/com/example/cramschool/config/ReportMailSetupDialog.java` | AWT/Swing 報告郵件設定視窗 |
| `src/main/java/com/example/cramschool/service/PowerShellUpdateInstaller.java` | 產生並執行 PowerShell、`msiexec.exe` 與 `.exe` 更新安裝器 |
| `src/main/java/com/example/cramschool/config/MysqlCommandLocator.java:64,77-89` | 預設以 `mysql.exe`/`mysqldump.exe` 尋找與測試 MySQL CLI |
| `src/main/java/com/example/cramschool/service/{ExamService,AnalysisHomeworkExportService,AnalysisAttendanceExportService,ClassStatisticsExportService}.java` | 開啟匯出/附件資料夾時依 OS 選擇 `explorer.exe`、`open` 或 `xdg-open`；在 VPS 不應執行此 UI 行為 |

### 3.2 C# 讀卡 Client

| 檔案 | Windows 專屬內容 |
|---|---|
| `tools/card-listener/WholeSummer.CardListener.csproj:3-6` | `WinExe`、`net8.0-windows`、Windows Forms |
| `tools/card-listener/Program.cs:1,13,19,27-28` | Windows Forms、Global Mutex、`shell32.dll` P/Invoke |
| `tools/card-listener/RawInputForm.cs` | Windows Raw Input/HID 讀卡器來源識別 |
| `tools/card-listener/KeyboardInputSuppressor.cs` | Windows 鍵盤輸入攔截 |
| `tools/card-listener/TrayApplicationContext.cs` | Windows 系統匣與通知 |
| `tools/card-listener/SingleInstanceGuard.cs` | Windows 單一執行個體機制 |
| `tools/card-listener/appsettings.json` | 現場 Client 設定；預設 API 為 `http://127.0.0.1:8080`，搬遷後需改為 HTTPS VPS URL |
| `tools/card-listener/README.md` | Windows `.exe` 發佈、PowerShell 建置與本機 API 說明 |

### 3.3 Windows 建置/包裝

| 檔案 | 內容 |
|---|---|
| `.github/workflows/build-windows.yml` | `windows-latest`、PowerShell、`.exe/.msi`、`jpackage --type exe/msi`、Windows 路徑與 `cmd` shell |
| `packaging/windows/WholeSummer.ico` | Windows 安裝包圖示 |
| `release.sh` | 發行流程；需與 Linux 發佈流程分開檢視 |
| `mvnw.cmd` | Windows Maven Wrapper 啟動腳本 |

## 4. 固定路徑與 Windows 路徑分隔符號

### 4.1 設定推導的路徑

| 設定/程式 | 目前值或規則 | 搬遷注意 |
|---|---|---|
| `app.data.dir` | `${user.dir}/data` | 依啟動工作目錄；systemd 的 WorkingDirectory 必須明確 |
| `app.backup.dir` | `${user.dir}/data/backups` | 備份檔與應用程式工作目錄耦合 |
| `app.exam-paper.dir` | `${user.home}/WholeSummer` | 附件/題目與 user home 耦合；需改成 VPS 明確持久化目錄 |
| `app.update.dir` | `WHOLESUMMER_UPDATE_DIR` 或外部 base dir 的 `update` | VPS 不應啟用 Windows 安裝更新流程 |
| 外部 base dir | `-Dwholesummer.base-dir`、`WHOLESUMMER_HOME`、Windows `ProgramData\WholeSummer`、fallback `user.home/WholeSummer` | `ExternalConfigPaths.java:18-32`；`ProgramData` 為 Windows 環境變數 |
| Windows class data | Windows 固定 `C:\WholeSummer`；非 Windows 為 `user.home/WholeSummer` | `ExternalConfigPaths.java:34-39` |
| 外部設定 | `<base>/config/application.properties` | `ExternalConfigPaths.java:45-51` |
| Log | 外部模式 migration 會寫 `<base>/logs/wholesummer.log`；bundled properties 未指定 `logging.file.name` | `ExternalConfigMigration.java:63-65`、`application.properties:1-59` |
| 匯出 | `user.home/WholeSummer/分析` | 三個 Export Service 的 `exportFolder()` |
| Cloudflare | 專案搜尋未發現 tunnel YAML、Cloudflare Token 或固定 Cloudflare 路徑；`.codex-tmp/bin/cloudflared` 是工作區暫存二進位檔，不是正式設定 | 需在 VPS 部署階段另行建立且不提交 Token |

### 4.2 路徑分隔符號

* Java 大多使用 `Path.resolve()`，本身可跨平台。
* `ExternalConfigMigration.windowsPath()` 名稱雖為 Windows path，但目前只是將反斜線替換成 `/`；其 default 設定仍由 Windows/外部模式流程產生，需重新檢查語意。
* 明確 Windows 反斜線與磁碟代號位於 `ExternalConfigPaths.java:27-31,36`、`ExternalConfigPathsTests.java`、`.github/workflows/build-windows.yml`、`tools/card-listener/README.md`。
* 未發現 Registry API 使用；但 C# 使用 `shell32.dll`，Java 使用 AWT/Swing/桌面 API。

## 5. PowerShell、cmd.exe、EXE、Registry、桌面 API

* PowerShell：`PowerShellUpdateInstaller.java:24-72,118-128`；GitHub Actions 多處 `shell: pwsh`。
* `cmd.exe`：Workflow 的 `shell: cmd`（`build-windows.yml:106,127`）；Java 原始碼未發現直接呼叫 `cmd.exe`。
* EXE：`explorer.exe`（四個匯出/附件 Service）、`WholeSummer.CardListener.exe`（CardListenerProcessManager）、`powershell.exe`/`msiexec.exe`（PowerShellUpdateInstaller）、更新檢查器與下載器對 `.exe/.msi` 有 Windows 安裝檔邏輯。
* Registry：未發現 Java Registry 或 Windows Registry 呼叫。
* 桌面 API：`java.awt`、`javax.swing`、`GraphicsEnvironment`、`Desktop.getDesktop()`、Windows Forms、Raw Input、`shell32.dll`。

## 6. application.properties、Profile 與環境設定來源

### 6.1 設定檔來源

1. 內嵌預設：`src/main/resources/application.properties`。
2. 專案根目錄外部檔：`spring.config.import=optional:file:./config/application.properties`（`application.properties:2`）。
3. Windows 外部模式：`-Dwholesummer.external-config.enabled=true` 後，由 `ExternalConfigInitializer` 將 `<base>/config` 加入 Spring config location。
4. 環境變數：資料庫、LINE、Resend、Web Push 等由 `${ENV_VAR:default}` 取得。
5. 系統屬性：`WHOLESUMMER_HOME`、`wholesummer.base-dir`、`wholesummer.external-config.enabled`、`server.port` 等。
6. 資料庫內設定：`SystemSetting`/`SystemSettingService` 儲存部分應用程式選項；這不是秘密設定的主要安全存放區。

### 6.2 Profile

目前未發現正式 `dev/test/prod` Profile。Ubuntu 正式環境應在後續階段新增明確的 `prod` 設定策略，但不應在本階段直接改程式。

## 7. 敏感資料盤點

以下僅記錄類型與位置，報告不重複輸出實際值：

| 敏感資料 | 位置/來源 | 現況與風險 |
|---|---|---|
| MySQL URL、帳號、密碼 | `config/application.properties`（被 `.gitignore` 忽略）；預設鍵在 `src/main/resources/application.properties:5-7`；也支援 `WHOLESUMMER_DB_*` | 本機外部檔含實際 DB 密碼；需確認未曾被提交或出現在備份/Log |
| LINE Channel Secret | `config/application.properties`；環境變數 `LINE_CHANNEL_SECRET`；`application.properties:45-50` | Webhook 簽章驗證使用；若外洩必須旋轉 |
| LINE Channel Access Token | 同上；`LINE_CHANNEL_ACCESS_TOKEN` | Reply、Push、Profile 查詢使用；若外洩必須旋轉 |
| LIFF ID / Channel ID | `config/application.properties` 與 `LINE_LIFF_ID`/`LINE_LIFF_CHANNEL_ID` | LIFF ID 通常非密鑰，但需視為正式環境設定 |
| Web Push VAPID private key | `config/application.properties`；`WEBPUSH_VAPID_PRIVATE_KEY`；外部 migration 也可能自動產生 | 私鑰若變更，現有瀏覽器訂閱需重新啟用；不可進 Git |
| Web Push VAPID public key | 同上 | 非秘密但須與私鑰配對 |
| Resend API Key | `app.report.mail.api-key`，來源 `RESEND_API_KEY`；`BugReportService.java` | 郵件回報外部 API；不得寫入 Git |
| 教師登入密碼雜湊/鹽 | `data/settings.properties` 被忽略；資料庫 `teacher_accounts` 等 entity | 不是明文 API Token，但屬認證敏感資料；備份與權限需保護 |
| Cloudflare Token/設定 | 專案搜尋未發現 | 目前無法從專案證實已設定；VPS 階段應由 systemd/secret file/環境注入，不寫入 Git |
| GitHub Token | 僅 workflow 使用 GitHub Actions 的 `secrets.GITHUB_TOKEN` | 非專案硬編碼秘密；更新下載本身是公開 GitHub API |

## 8. MySQL、Flyway、備份、上傳檔案與 Log

### MySQL/Flyway

* 預設 JDBC：`jdbc:mysql://localhost:3306/WholeSummer?...`，位於 `application.properties:5`。
* JPA 使用 `ddl-auto=validate`；Flyway 啟用、validate-on-migrate 啟用、clean disabled；migration 位於 `src/main/resources/db/migration/`。
* `DatabaseSetupService` 另提供建立資料庫、測試連線、匯入 SQL 的桌面首次設定流程；這不適合 headless VPS 首次啟動。
* 本機測試連到 MySQL 9.6；Flyway 顯示最新驗證版本為 9.4，正式搬遷前需以 Ubuntu 目標版本做相容性測試。

### 備份/還原

* `BackupService.java:40-50` 以 `app.backup.dir` 建立目錄。
* `BackupService.java:67-98` 用 `mysqldump` 產生 `WholeSummer_backup_<timestamp>.sql`。
* 還原流程在同一 Service 使用 MySQL CLI、先建立安全備份、清理 SQL 中的 GTID/SQL_LOG_BIN，再執行還原；`BackupController` 提供管理頁操作。
* 目前是應用程式內建、以本機目錄保存；尚未有 VPS 外部異地備份、保留政策或 systemd timer。
* 密碼會傳入 MySQL CLI ProcessBuilder（`BackupService.java:318` 附近）；後續應避免在 process arguments 或 Log 暴露。

### 上傳附件

* 考卷/題目檔：`ExamService.java:38-41,158-187`，根目錄由 `app.exam-paper.dir` 指定，實際檔案路徑存入資料庫 `Exam.paperFilePath`。
* 分析匯出：`AnalysisHomeworkExportService.java:166-167`、`AnalysisAttendanceExportService.java:174-175`、`ClassStatisticsExportService.java:271`，保存到使用者 home 下的 `WholeSummer/分析`。
* 目前未見物件儲存；VPS 必須把這些目錄列入持久化備份與權限規劃。

### Log

* Spring Boot 預設 Log 若未配置 file name，主要由服務 stdout/stderr 接收；外部模式會追加 `logging.file.name=<base>/logs/wholesummer.log`。
* 更新器另寫 `<base>/logs/updater.log` 與 MSI log，但該流程為 Windows 專用。
* 目前未發現 journald、logrotate、集中式 Log 或明確容量上限設定。

## 9. LINE Webhook、LIFF、Rich Menu、排程通知

### Webhook/訊息

* Webhook：`LineWebhookController.java:39-88`，POST `${line.webhook-path:/api/line/webhook}`，驗證 `x-line-signature`，再由 `LineMessageRouter` 分派。
* LINE API：`LineMessageService.java` 呼叫 reply、push、profile API。
* 綁定：`LineBindingService.java`、`LineBindCode`、`ParentLineBinding`、`LineNotificationTemplate`。
* 課表查詢：`LineMessageRouter.java` 將文字命令交給 `LineScheduleService.java`，由綁定的學生課表產生文字回覆。
* LIFF 頁面：`LiffPageController.java` `/liff/leave` 與 `templates/liff/leave.html`；LIFF API：`LineLiffApiController.java` `/api/line/liff/*`；Token 驗證：`LineLiffAuthService.java` 呼叫 LINE verify API。
* Rich Menu：專案搜尋未發現 LINE Rich Menu 建立/查詢 API、Rich Menu ID 或排程同步程式；目前可確認的是文字課表查詢與 LIFF，不能把它宣稱為已有 Rich Menu 管理功能。

### 排程通知

* 全域啟用：`WholeSummerApplication.java:26-28` 的 `@EnableScheduling`。
* 缺席處理：`AbsenceService.java:47` 每 5 分鐘。
* 遲到通知：`LateArrivalReminderService.java:48` LINE cron 預設每分鐘；`LateArrivalReminderService.java:53` Web Push 預設每 10 分鐘，時區為 Asia/Taipei。
* LINE 通知服務：`LineNotificationService.java`，包含到班、遲到、請假、補課/調課等通知與去重 Log。
* VPS 要求：確保單一應用程式 instance、時區、systemd restart 策略與資料庫鎖定行為，避免重複通知。

## 10. WholeSummer.CardListener 與刷卡 API

* 現場 Client：`tools/card-listener`，Windows Forms + Raw Input，預設 `http://127.0.0.1:8080`。
* 主程式自動啟動：`CardListenerProcessManager.java` 尋找 `WholeSummer.CardListener.exe` 並以 `--api-base-url`、`--device-name` 啟動；這段只適用現場 Windows。
* API：一般刷卡 `CardAttendanceApiController.java` `/api/attendance/card-check-in`；桌面 Client 使用 `DesktopCardAttendanceController.java:24-53` 的 `/internal/desktop/card-check-in`。
* Request 欄位包含 `cardId`、`deviceName`；現行 Client 沒有 API Token/mTLS/簽章設定。若將 endpoint 暴露到公網，必須在後續階段補上網路限制或專用認證，不能直接把 `/internal/desktop` 開放給任何來源。
* Card binding/近期紀錄：`CardBindingController.java`、`CardBindingModeService.java`、`RecentCardCheckInService.java`。
* 搬遷後的預期流向是：現場 Windows Client → HTTPS VPS `/internal/desktop/card-check-in`；不應讓 VPS 依賴 USB 或 Windows Raw Input。

## 11. 目前能否直接在 Ubuntu 執行

**判定：不建議直接作為正式 VPS 服務執行；需先做 Linux headless 相容性修改。**

有利條件：

* Java 主體以 Spring Boot/JPA/JDBC/Flyway/標準 HTTP 為主，未發現 Registry 或 Windows 驅動依賴於核心 Web service。
* 多數檔案操作使用 `java.nio.file.Path`，資料庫 migration 是 classpath 資源。
* 匯出資料夾開啟已具備 `xdg-open` 分支，顯示曾考慮 Linux。

阻礙條件：

* main 啟動流程無條件執行 Swing 外觀與啟動畫面；systemd/headless 需要繞過。
* 外部設定缺失時，headless 模式會因無法顯示首次設定視窗而丟例外。
* Windows 更新、桌面狀態視窗、Card Listener 自動啟動不應進入 VPS。
* `MysqlCommandLocator` 偏向 `.exe`；VPS 的 Linux `mysqldump` 路徑與 executable 檢查需驗證。
* `user.dir`/`user.home` 路徑與 systemd 使用者、WorkingDirectory、檔案權限尚未固定。
* 本次測試只能證明目前開發機環境能啟動 Spring context，不能證明 Ubuntu headless、systemd、Cloudflare Tunnel 或遠端 MySQL 已可用。

## 12. 搬遷風險

| 等級 | 風險 | 影響 |
|---|---|---|
| 高 | 敏感設定檔若被備份、提交或 Log 洩漏 | DB/LINE/Web Push/郵件服務遭未授權使用 |
| 高 | 啟動流程依賴 GUI | systemd 服務反覆啟動失敗，網站、Webhook、排程全部中斷 |
| 高 | 刷卡 endpoint 無專用認證且原本為 localhost 情境 | 暴露到公網後可被偽造刷卡請求 |
| 高 | DB 搬遷與附件路徑未同時備份 | 資料庫有紀錄但考卷/附件遺失，或反之 |
| 高 | 直接在正式 DB 執行 migration/restore | schema 不一致或資料損壞；需先做完整 dump 與驗證 |
| 中 | MySQL 9.6 高於 Flyway 已驗證 9.4 | 可能出現 migration/SQL 相容性問題 |
| 中 | 沒有正式 Profile 與 systemd 設定來源 | 環境差異難以追蹤，容易誤用本機設定 |
| 中 | 排程在多 instance 或重啟時重複執行 | LINE/Push 通知重複發送 |
| 中 | `user.dir`/`user.home` 與檔案權限 | 上傳、備份、匯出或 Log 寫入失敗 |
| 中 | Cloudflare Tunnel、DNS、Webhook URL 尚未在 repo 中可驗證 | 域名可達性與 LINE callback 切換風險 |
| 低 | 使用 Java 25、Spring Boot 4.1.0 的 VPS 套件供應 | Ubuntu LTS 預設套件可能沒有相同版本，需使用 Temurin/官方 JDK |

## 13. 建議修改順序與涉及檔案

以下是下一階段以後的建議，現在不執行：

1. **先建立 Linux/headless 執行邊界**
   * 涉及：`WholeSummerApplication.java`、`StartupLoadingWindow.java`、`SwingLookAndFeel.java`、`ExternalConfigInitializer.java`、`ExternalConfigPaths.java`、`DesktopStatusWindow.java`、`CardListenerProcessManager.java`。
   * 目標：Linux/systemd 不載入 GUI/Windows 啟動器；Windows 原功能保留。

2. **整理正式環境設定與秘密注入**
   * 涉及：`src/main/resources/application.properties`、`ExternalConfigMigration.java`、`LineProperties.java`、`WebPushProperties.java`、`README.md`、`.gitignore`；新增 `docs/` 與範例設定檔。
   * 目標：明確 `prod` 來源、檔案權限、環境變數名稱、秘密輪替流程；不把實際值放 Git。

3. **固定 VPS 持久化目錄與 Log**
   * 涉及：`application.properties`、`BackupService.java`、`ExamService.java`、三個分析 Export Service、`ExternalConfigPaths.java`。
   * 目標：明確 `/opt`/`/var/lib`/`/var/log` 類型目錄、systemd 使用者權限、附件與 DB dump 的備份邊界。

4. **確認 Linux MySQL CLI、Flyway 與備份恢復流程**
   * 涉及：`MysqlCommandLocator.java`、`BackupService.java`、`DatabaseSetupService.java`、`src/main/resources/db/migration/*`；另新增 Linux 備份腳本/文件。
   * 目標：先做 staging DB dump/import/validate，再建立 timer、保留週期、異地備份與 restore runbook。

5. **建立 systemd/健康檢查/更新回復流程**
   * 涉及：新增 `deploy/systemd/`、`deploy/scripts/`、`docs/`；必要時調整 `UpdateChecker.java`、`UpdateDownloader.java`、`UpdateCoordinator.java`。
   * 目標：systemd restart、journalctl、health endpoint、版本回復；VPS 不執行 PowerShell 安裝器。

6. **處理 Cloudflare 與 LINE 切換驗證**
   * 涉及：`LineWebhookController.java`、`LineLiffAuthService.java`、`LiffPageController.java`、LIFF template、部署文件；Cloudflare 設定放 VPS 外部秘密來源。
   * 目標：先以 staging/domain 測試 HTTPS、Webhook 簽章、LIFF、LINE 文字課表與通知，再切正式 URL。

7. **最後處理現場讀卡 Client 遠端 API 安全與切換**
   * 涉及：`tools/card-listener/appsettings.json`、`AppSettings.cs`、`CardCheckInClient.cs`、`DesktopCardAttendanceController.java`、`CardAttendanceApiController.java`、相關測試。
   * 目標：保留 Windows 現場功能，改用 HTTPS VPS URL；加入可撤銷的 Client 認證/網路限制與重試策略，測試離線與重送。

## 14. 本階段新增/未修改項目

本階段僅新增本報告文件；未修改 Java、C#、設定檔、Migration、Workflow 或部署架構。報告中的實際秘密值均未複製出來。下一階段應先由使用者確認本盤點與修改順序，再開始 Linux 相容性修改。
