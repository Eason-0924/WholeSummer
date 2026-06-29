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

WholeSummer 是一套以 Spring Boot 與 MySQL 建立的補習班管理系統，提供學生、教師、班級、科目、測驗、考卷、作業、出勤、調課、補課、薪資、學費、系統更新與資料備份等日常教務功能。

系統以瀏覽器作為操作介面，可在本機或區域網路中使用；Windows 正式版本可透過安裝程式部署，首次啟動時會協助完成 MySQL 連線與資料庫初始化。

目前版本：**1.1.3**

## 使用者操作教學與須知

### 使用前須知

- WholeSummer 以瀏覽器操作。Windows 安裝版啟動後，請依畫面或狀態視窗開啟系統網址；本機開發預設為 `http://localhost:8080`。
- 登入帳號與密碼皆區分英文字母大小寫，請依建立帳號時的大小寫輸入。
- 每位教師看到的功能會依職位與個別權限不同而調整。主任固定擁有全部權限；一般教師或輔導老師需由主任授權後才能看到學費、備份、系統更新、一鍵升年級等管理功能。
- 新增、編輯、刪除或還原資料前，請先確認目前登入教師是否正確。系統會保存資料異動操作紀錄，供主任查詢。
- 資料庫備份與還原、系統更新、一鍵升年級、批次調整課表都會影響多人使用，建議在非上課尖峰時間操作，並先建立或下載備份。
- 問題回報若要寄送 Email，需先完成 Resend 郵件設定；未設定時仍會先保存於本機紀錄。

### 第一次使用與登入

1. 開啟 WholeSummer 系統網址。
2. 若系統尚未建立任何教師帳號，請點選登入頁的「教師註冊」，建立第一位主任與登入帳號。
3. 若系統已有教師資料但尚未建立個人帳號，請在「教師註冊」選擇自己的教師姓名，輸入登入帳號、密碼與教師註冊安全碼。
4. 註冊安全碼預設為 `whole-summer`。主任首次登入後，請到「系統設定」變更安全碼。
5. 回到登入頁輸入帳號與密碼。登入成功後會進入首頁。
6. 右上角會顯示目前登入教師姓名；使用完畢請按「登出」結束工作階段。

### 首頁與通知

- 首頁提供學生、班級、科目、測驗、作業、學費、薪資與系統設定等入口。
- 通知中心會顯示即將到期作業、未來測驗、逾期學費、系統更新、學生生日、補課與待安排調課提醒。
- 不同通知會以不同顏色標示，點選通知可進入相關頁面。
- 學費通知只會顯示給具備學費管理權限的教師；版本更新通知只會顯示給具備系統更新權限的教師。

### 學生管理

1. 從首頁或上方選單進入「學生」。
2. 可新增學生，維護中文名、英文名、生日、學校、年級、聯絡方式與備註。
3. 學生詳細頁可查看個人成績、作業紀錄、出缺席紀錄與學費紀錄。
4. 學生可設為在學、畢業或復學；刪除前請確認不再需要保留日常操作入口。
5. 學生網址會使用系統產生的代稱，方便在詳細頁之間穩定連結。

### 教師、打卡、請假與補課

1. 進入「教師」可管理教師資料、職位、任教狀態、授課科目與班級。
2. 一般教師可從首頁「前往打卡」或自己的出勤頁快速記錄上班、下班與請假。
3. 主任或具備全體出勤權限的教師可為所有教師補登出勤、手動請假或查看出勤統計。
4. 上班打卡會依當日課表自動配對課程，判斷出勤或遲到，並計算與課程重疊的工時。
5. 同時段多個班級不會重複計算工時；無對應課程的打卡紀錄需由主任補充備註與時數後才會納入薪資。
6. 教師請假或缺勤產生的課程需求會進入「補課需求」，可查看待補課項目並安排合適的補課時間。
7. 若補課或調課尚未決定新時間，首頁通知會提醒相關教師或主任後續安排。

### 班級、課表與點名

1. 進入「班級」可建立班級，設定年級、科目、授課教師、班別、說明與多個上課時段。
2. 班級詳細頁可維護學生名單，並快速進入測驗、作業與點名流程。
3. 點名頁可登記學生出席、請假、缺席等狀態。
4. 上課時間表可切換「固定課表」與「當週課表」。固定課表顯示班級原始週期課程；當週課表會整合原課程、補課、調課與已取消課程。
5. 當週課表可勾選是否顯示已取消課程，並以標籤區分補課、調課與取消課程。
6. 課表可依科目、老師或年級套用不同標記顏色，並匯出 JPEG、DOCX 或 XLSX。
7. 班級網址會使用系統產生的代稱，班名或資料調整後仍可保持較穩定的頁面路徑。

### 調課操作

1. 進入「班級」並切換到「當週課表」。
2. 在「安排調課」選擇要調整的課程與原上課日期。
3. 點選「新上課時間」開啟可調課時段視窗。
4. 在月曆中選擇日期，系統會列出可用、教師衝突或學生衝突的候選時段。
5. 學生衝突的時段不可選；教師衝突的時段需再次確認後才能選擇。
6. 若暫時無法決定新時間，可按「待定」，系統會建立待安排調課通知。
7. 送出後，原課程會在當週課表標示為已取消，新時段會標示為調課。

### 科目管理

- 進入「科目」可新增或編輯科目。
- 科目可設定適用年級，並關聯多位建議教師。
- 建立班級或查詢課程時會使用科目資料。

### 測驗與成績

1. 進入「測驗」建立測驗，可選擇計分測驗或不計分課堂練習。
2. 選擇班級後系統會自動帶入科目。
3. 可上傳考卷檔案；若上傳 PDF，可輸入頁數範圍，例如 `1-3`、`1,3,5` 或 `2-4,7`，系統只儲存擷取後的 PDF。
4. 建立後進入測驗詳細頁登記學生分數或完成狀態。
5. 測驗詳細頁會顯示考卷檔案名稱，並可開啟本機考卷資料夾。
6. 系統會整理平均、最高、最低、缺考與學生歷史成績。
7. 未來測驗會顯示於首頁通知中心，方便提早準備。

### 作業管理

1. 進入「作業」建立班級作業，系統會自動產生班上學生的作業紀錄。
2. 可登記未繳交、已繳交、逾期補交或免交。
3. 系統可依班級下次上課日與設定的提醒天數，顯示即將到期作業通知。
4. 作業詳細頁可追蹤每位學生的繳交狀態。

### 學費管理

1. 具備學費管理權限的教師可進入「學費」。
2. 可為學生建立個別應繳費用，記錄應繳、已繳、未繳、繳費日期與繳費期限。
3. 系統會自動判斷未繳、部分繳費、已繳清與逾期狀態。
4. 逾期學費會顯示於具備權限者的首頁通知。
5. 學生詳細頁也會顯示該學生的個人繳費紀錄。

### 薪資查詢

1. 進入「薪資」查看指定月份的累積工時與薪資。
2. 教師只能查看自己的薪資；主任或具備全體薪資權限者可查看所有教師。
3. 每位教師每個月份可獨立設定時薪。
4. 薪資依打卡紀錄配對的實際課程時數計算。
5. 主任可展開或收合教師當月打卡明細，檢查對應課程、課程時間、計入時數與備註。
6. 無對應課程的紀錄可由主任補充備註與上課時數，儲存後會立即重新計算當月薪資。

### 系統設定

- 「畫面模式」可切換亮色、暗色或跟隨裝置，設定會保存在目前瀏覽器。
- 「一般設定」可調整系統名稱、作業提醒天數與備份提醒天數。
- 「個人登入密碼」可變更目前登入教師自己的密碼。
- 「教師註冊安全碼」可變更新教師註冊時必填的安全碼。
- 「教師權限設定」由主任為每位教師授權，一般教師與輔導老師的入口與後端操作都會依權限開放。
- 「操作紀錄」提供主任查看最近資料異動紀錄，包含操作時間、教師、動作、路徑與結果。
- 「問題與 Bug 回報」可提交錯誤回報、操作問題或功能建議，並可附加基本系統資訊。
- 「系統更新」可檢查 GitHub Releases 上的新版本，Windows 安裝版可直接下載並啟動更新。
- 「資料庫備份」可建立、下載、刪除、還原或匯入 SQL 備份；還原前系統會先建立安全備份。
- 「一鍵升年級」會分步確認學生年級、畢業狀態、班級升級與新班名單，適合學期或學年轉換時使用。

### 系統更新操作

1. 使用具備系統更新權限的帳號登入。
2. 進入「系統設定」並展開「系統更新」。
3. 按「檢查更新」確認是否有新版本。
4. 若有可安裝版本，可查看版本號、安裝檔名稱與更新內容。
5. 按「稍後再說」可暫時略過此版本；按「立即更新」會下載安裝檔並關閉目前系統。
6. Windows updater 會等待主程式關閉、執行 installer，完成後重新啟動 WholeSummer。
7. 更新不會刪除外部設定檔、MySQL 資料庫、備份檔或使用者資料。

### 備份與還原操作

1. 使用主任或具備資料庫備份權限的帳號登入。
2. 進入「系統設定」並展開「資料庫備份」。
3. 按「立即備份」建立目前資料庫的 SQL 備份。
4. 在備份紀錄中可下載或刪除備份。
5. 還原或匯入 SQL 會覆蓋目前資料，操作前請再次確認；系統會先自動建立一份安全備份。
6. 還原完成後，所有使用者都需要重新登入。

### 問題回報操作

1. 進入「系統設定」並展開「問題與 Bug 回報」。
2. 選擇問題類型，輸入標題、問題描述、發生頁面與聯絡 Email。
3. 問題描述建議包含操作步驟、預期結果與實際發生情況。
4. 可勾選附加作業系統、Java 版本與教師職位；系統不會附加密碼、註冊安全碼、資料庫連線資訊或學生資料。
5. 送出後會保存於本機；若郵件服務已設定，會同時寄送給開發者。

### 一鍵升年級操作

1. 使用主任或具備一鍵升年級權限的帳號登入。
2. 進入「系統設定」底部的「危險操作區域」，點選「一鍵升年級」。
3. 依畫面分階段確認學生年級、畢業狀態、班級升級與新班學生名單。
4. 國三學生升高一時必須輸入新學校；高三學生會自動設為已畢業。
5. 升級班級時會保留原班歷史資料，並複製科目、教師、班別、說明及上課時段至新班。
6. 執行前建議先建立並下載資料庫備份。

## 主要功能

以下內容已整合近期版本的更新功能，包含權限、操作紀錄、通知中心、當週課表、調課、補課、考卷檔案、學費、薪資、備份、Flyway 資料庫版本管理與 Windows 自動更新。

- **帳號與權限**
  - 教師註冊與個人帳號登入
  - 登入帳號與密碼皆區分英文字母大小寫
  - 教師、輔導老師、主任三種職位
  - 主任固定擁有全部權限
  - 主任可為每位教師設定個別管理權限
  - 頁面入口與後端操作依教師權限同步限制
- **首頁與通知中心**
  - 作業到期、未來測驗、逾期學費、版本更新、生日、補課與待安排調課通知
  - 通知以直向清單呈現，並以顏色區分類型
  - 學費與更新通知依教師權限顯示
  - 手機與平板支援響應式表格、詳細頁收合區塊與觸控操作
- **學生管理**
  - 中文名、英文名、生日與基本資料
  - 在學、畢業、復學及刪除
  - 個人成績、作業、學費與出缺席紀錄
  - 學生詳細頁使用穩定網址代稱
- **教師管理**
  - 教師資料、任教狀態、職位、授課科目與班級
  - 上下班、請假、缺勤及出勤統計
  - 依當日課程自動計算工時與遲到狀態
  - 教師詳細頁使用穩定網址代稱
- **補課管理**
  - 教師請假或缺勤產生補課需求
  - 主任可查看全部待補課，一般教師查看自己的待補課
  - 點進單筆需求後計算可補課時段並安排時間
- **班級與課表**
  - 年級、科目、教師、班別及多個上課時段
  - 班級學生、點名、測驗及作業整合
  - 固定課表與當週課表切換
  - 當週課表整合原課程、補課、調課與取消課程
  - 課表依科目、老師或年級標記顏色
  - JPEG、DOCX、XLSX 匯出
  - 班級詳細頁使用穩定網址代稱
- **調課管理**
  - 從當週課表選擇原課程與原上課日期
  - 依課程長度產生未來可調課時段
  - 檢查學生、教師、補課與既有調課衝突
  - 可立即安排新時段，或建立待安排調課通知
  - 調課完成後保留原課程取消紀錄與新課程紀錄
- **科目管理**
  - 適用年級複選
  - 科目與多位建議教師關聯
  - 科目詳細資料使用穩定網址代稱
- **測驗與成績**
  - 計分測驗與不計分課堂練習
  - 選擇班級後自動帶入科目
  - 測驗可上傳考卷檔案
  - PDF 考卷可依頁數範圍擷取後儲存
  - 測驗詳細頁可查看考卷檔案並開啟本機資料夾
  - 分數或完成狀態登記
  - 平均、最高、最低、缺考與學生歷史成績
- **作業管理**
  - 班級作業建立及學生紀錄自動產生
  - 未繳交、已繳交、逾期補交、免交
  - 下次上課日、自動截止日期與首頁通知
- **薪資試算**
  - 每位教師、每月份獨立時薪
  - 依打卡紀錄所配對的實際課程時數計算薪資
  - 同時段多個班級不重複計算
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
  - 上方導覽支援學生、教師、班級與科目的快速下拉清單
  - 可收合設定區塊與教師個別權限設定
  - Windows 桌面狀態視窗可顯示並重新喚回既有執行中的系統
  - MySQL 備份、下載、還原及初始資料匯入
  - Flyway 資料庫版本管理與自動升級
  - 資料異動操作紀錄與主任查詢
  - 問題回報本機紀錄與 Resend Email 通知
  - Windows 外部設定檔與 GitHub Releases 更新
  - 發布腳本可從 README 版本章節讀取 Release 說明

## 1.1.3 更新內容

- 優化上方導覽列，學生、教師、班級與科目支援滑鼠下拉快速前往詳細頁。
- 手機版導覽新增可展開的子選單，方便在小螢幕快速切換資料。
- 新增全站導覽資料模型，讓常用資料可在每個頁面共用導覽清單。
- 新增科目詳細頁，可查看科目狀態、適用年級、授課教師與目前開課班級。
- 科目列表的科目名稱改為可點擊連結，直接進入科目詳細頁。
- 班級詳細頁的出席統計、作業列表與測驗列表改為可展開／收合，減少長頁面干擾。
- 薪資頁教師打卡明細改用自製展開／收合控制，改善點擊範圍與視覺一致性。
- 首頁通知中心改為直向可捲動清單，提升多筆通知時的閱讀效率。
- 統一多數頁面與匯出檔的日期顯示格式為 `yyyy/MM/dd` 或 `yyyy/MM/dd HH:mm`。
- 調課表單版面微調，讓原上課日期警示與課表切換按鈕更清楚。
- Windows 啟動時會先檢查設定的連接埠是否已被使用，避免重複啟動造成錯誤。
- 若偵測到既有 WholeSummer 正在執行，會嘗試喚回原本的桌面狀態視窗。
- 桌面狀態視窗優化顯示與聚焦行為，並改用新版 `favicon.ico` 載入視窗圖示。
- 整理網站與 Windows 安裝圖示，移除舊的多尺寸 PNG manifest 圖示檔。
- 內部桌面狀態視窗端點排除登入與操作紀錄攔截，並限制只允許本機呼叫。

## 1.1.2 更新內容

- 首頁通知中心新增待安排調課提醒，補課與調課通知會依教師身分顯示對應內容。
- 班級頁上課時間表新增「固定課表」與「當週課表」切換。
- 當週課表整合原課程、補課、調課與已取消課程，並可切換是否顯示取消課程。
- 課表卡片新增課程類型標籤，可區分補課、調課與取消課程。
- 新增調課流程，可從班級頁選擇原課程、原上課日期、調課原因與新上課時間。
- 調課時會依原課程長度產生未來 30 天候選時段，平日自 14:00 起、週末自 09:00 起，每 30 分鐘檢查一次。
- 調課時會檢查學生班級衝突、教師衝突、補課衝突與既有調課衝突。
- 學生衝突時段不可選擇；教師衝突時段需確認後才可使用。
- 調課可先標記為待定，產生待安排調課通知，待後續再安排新時間。
- 完成調課後，系統會建立原課程取消紀錄、新調課課程紀錄與調課歷史資料。
- 測驗新增考卷檔案上傳，可在新增或編輯測驗時附加檔案。
- PDF 考卷支援輸入頁數範圍，只保存擷取後的 PDF，頁數資訊會自動寫入測驗說明。
- 測驗詳細頁新增考卷檔案顯示與「開啟資料夾」操作。
- 新增考卷檔案保存位置設定，考卷會複製到系統考卷資料夾。
- 新增課程類型、調課紀錄與考卷檔案欄位 migration。

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

班級相關檔案預設會存放在可見的 `C:\WholeSummer`，例如：

```text
C:\WholeSummer\國一數理\exam\隨堂練習\考卷.pdf
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
app.exam-paper.dir=C:/WholeSummer
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
| V8 | 建立教師請假資料表 |
| V9 | 建立補課需求資料表 |
| V10 | 新增學生網址代稱欄位 |
| V11 | 新增教師、科目與班級網址代稱欄位 |
| V12 | 新增課程類型、調課事件與調課歷史資料 |
| V13 | 新增測驗考卷檔案路徑與檔名欄位 |
| V14 | 新增測驗考卷檔案保存模式欄位 |

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
4. 從 README 的 `## x.y.z 更新內容` 章節自動讀取 Release 說明。
5. 建置專案、建立 commit 與 Git tag。
6. 推送至 GitHub。
7. 由 GitHub Actions 產生 Windows installer 並建立 Release。

若 README 沒有對應版本章節，腳本才會要求手動輸入；輸入 `END` 後按 Enter 即可完成，
空白行不會讓輸入流程提前結束。

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
