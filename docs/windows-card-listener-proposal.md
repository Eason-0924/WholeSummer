# Windows 鍵盤型刷卡機背景監聽程式企劃書

## 一、目標

為 WholeSummer 補習班管理系統新增一個在 Windows 上常駐執行的 `.NET` 背景程式，用來監聽鍵盤輸入型刷卡機的卡號資料，並在背景自動呼叫系統的刷卡點名 API，達成以下目標：

1. 不需切換到瀏覽器或刷卡頁面前景。
2. 刷卡後可立即完成學生點名或教師出勤。
3. 避免操作人員因游標焦點錯誤而刷卡失敗。
4. 保持與現有 WholeSummer 後端 API 的整合方式一致。

## 二、適用前提

本企劃適用於以下設備條件：

1. 刷卡機為 Windows 可辨識的 HID Keyboard 類型裝置。
2. 刷卡時會像鍵盤輸入一串字元。
3. 多數設備會在卡號後自動送出 `Enter`。

若未來改用非鍵盤輸入型設備，例如序列埠、IC 卡讀卡 SDK、USB 專屬驅動裝置，則需改走不同整合方式。

## 三、建議方案

建議採用：

`Windows 常駐 .NET 背景程式` + `呼叫 WholeSummer 本機 API`

原因如下：

1. 與現有 Java / Spring Boot 主系統責任分離，維護清楚。
2. 背景監聽、Windows 開機啟動、系統列常駐與裝置辨識，`.NET` 在 Windows 上較容易做穩。
3. 刷卡判斷與通知邏輯可在背景程式獨立演進，不必把 Windows 鍵盤 hook 硬塞進 Java 主程式。
4. 後端沿用現有刷卡點名服務邏輯，並透過本機內部端點呼叫，不必重寫點名規則。

## 四、系統架構

### 4.1 組成元件

1. WholeSummer 主系統
   - Spring Boot Web/API
   - 提供刷卡 API
   - 提供桌面狀態視窗與主程式啟動流程

2. Windows Card Listener
   - `.NET` 背景常駐程式
   - 負責監聽刷卡機輸入
   - 判斷是否為有效卡號
   - 呼叫本機刷卡 API
   - 顯示成功/失敗通知

3. 設定檔
   - 設定 API 位址、卡號長度、輸入逾時、通知開關、自動啟動等

### 4.2 流程摘要

1. WholeSummer 啟動。
2. WholeSummer 檢查 Windows Card Listener 是否已執行。
3. 若未執行，自動啟動 `.NET` 程式。
4. `.NET` 程式在背景監聽刷卡機輸入。
5. 讀到完整卡號後，呼叫 `http://127.0.0.1:{port}/internal/desktop/card-check-in`。
6. WholeSummer 回傳學生或教師刷卡結果。
7. `.NET` 程式以系統通知或小視窗顯示結果。

### 4.3 發布打包方式

Windows 版本由 GitHub Actions 統一產生：

1. 編譯 Spring Boot 主系統 jar。
2. 使用 `.NET 8` 將 Windows Card Listener 發布成 `WholeSummer.CardListener.exe`。
3. 將監聽器 exe 與 `appsettings.json` 放入 jpackage input folder。
4. 由 `jpackage` 產生 WholeSummer Windows 安裝檔。
5. 安裝後 WholeSummer 啟動時自動尋找並啟動監聽器。

因此正常部署時，使用者只需安裝 GitHub Release 產出的 WholeSummer Windows installer，不需另外安裝或手動執行 `.NET` 監聽器。

## 五、.NET 程式功能需求

### 5.1 核心功能

1. 背景常駐執行。
2. 開機後或登入後可自動啟動。
3. 可全域接收刷卡機輸入，不依賴瀏覽器焦點。
4. 能將刷卡輸入組成卡號字串。
5. 自動呼叫 WholeSummer 刷卡 API。
6. 顯示刷卡成功、遲到、簽退、失敗等通知。

### 5.2 輸入辨識功能

1. 支援短時間內連續輸入視為刷卡資料。
2. 支援以 `Enter` 作為卡號結束符號。
3. 可設定最短卡號長度與最長卡號長度。
4. 可設定輸入逾時，例如 100ms 至 300ms 無新字元即視為輸入結束。
5. 可清除空白、換行、Tab 等雜訊字元。

### 5.3 API 呼叫功能

1. 呼叫 WholeSummer 本機桌面 API：
   - `POST /internal/desktop/card-check-in`
2. 此 API 僅允許 loopback 本機來源呼叫，避免外部網路免登入存取。
3. 送出內容至少包含：
   - `cardId`
   - `deviceName`
4. 支援 API 失敗重試一次或記錄失敗結果。

### 5.4 使用者通知

1. 刷卡成功：
   - 顯示姓名
   - 顯示學生/教師
   - 顯示出席、遲到、上班、下班、簽退等結果
2. 刷卡失敗：
   - 顯示卡號未綁定
   - 卡片停用
   - 今日無課程
   - API 連線失敗

## 六、技術實作建議

### 6.1 開發框架

建議使用：

1. `.NET 8`
2. `WPF` 或 `WinForms`

建議優先選 `WinForms`：

1. 系統列常駐與通知圖示實作較快。
2. 對本專案而言 UI 很輕，無需 WPF 的較重結構。
3. 維護門檻較低。

### 6.2 背景監聽方式

建議分兩階段：

#### 第一階段：快速可用版

使用 Windows `Raw Input` 背景接收鍵盤類型輸入，依下列規則判斷刷卡：

1. 短時間內連續輸入。
2. 字元長度符合卡號規則。
3. 以 `Enter` 結束。

優點：

1. 實作快。
2. 可先驗證設備是否穩定。

缺點：

1. 若人工鍵盤快速輸入，也可能誤判。

#### 第二階段：穩定版

強化 `Raw Input` 裝置辨識，僅接受指定 HID 讀卡機來源。

優點：

1. 可以區分刷卡機與人工鍵盤。
2. 誤判率低。

缺點：

1. 開發難度較高。
2. 需先確認設備在 Windows 中可穩定辨識。

### 6.3 API 呼叫實作

建議使用 `HttpClient` 呼叫本機 API。

範例請求：

```http
POST http://127.0.0.1:8080/internal/desktop/card-check-in
Content-Type: application/json

{
  "cardId": "04A1B2C3",
  "deviceName": "windows-card-listener"
}
```

### 6.4 設定檔設計

建議使用 `appsettings.json`，內容可包含：

```json
{
  "WholeSummer": {
    "ApiBaseUrl": "http://127.0.0.1:8080",
    "CheckInApiPath": "/internal/desktop/card-check-in"
  },
  "CardReader": {
    "DeviceName": "windows-card-listener",
    "MinLength": 6,
    "MaxLength": 32,
    "InputTimeoutMs": 200,
    "UseEnterAsTerminator": true
  },
  "Notification": {
    "Enabled": true
  }
}
```

## 七、程式運行方式

### 7.1 啟動模式

建議有兩種啟動方式：

1. WholeSummer 啟動時自動啟動 `.NET` 程式。
2. `.NET` 程式也可獨立手動啟動，供除錯與維運使用。

### 7.2 背景執行方式

建議執行後：

1. 不顯示主視窗，僅顯示系統列圖示。
2. 右鍵選單提供：
   - 顯示狀態
   - 測試 API 連線
   - 查看最近刷卡結果
   - 重新載入設定
   - 結束程式

### 7.3 自動啟動

建議使用登入後自動啟動，而非 Windows Service。

原因：

1. 鍵盤輸入監聽屬於互動桌面行為。
2. Windows Service 不適合直接監聽登入使用者桌面上的 HID 鍵盤輸入。

可用方式：

1. 加入啟動資料夾
2. 登錄表 `Run`
3. 由 WholeSummer 主程式啟動

最建議：由 WholeSummer 主程式統一啟動與管理。

## 八、與 WholeSummer 主程式整合方式

### 8.1 啟動整合

WholeSummer 啟動完成後：

1. 檢查 `.NET` 背景程式是否已在執行。
2. 若未執行，使用 `ProcessBuilder` 啟動 `.exe`。
3. 傳入必要參數，例如 API Port、設定檔路徑、工作目錄。

### 8.2 重複啟動防呆

`.NET` 程式需具備單例執行保護，例如：

1. `Mutex`
2. 已執行程序名稱檢查

避免 WholeSummer 重開時產生多個背景監聽程式。

### 8.3 健康檢查

可選擇擴充：

1. `.NET` 程式提供本機健康檢查端點或命名管道
2. WholeSummer 定期檢查其是否存活
3. 若異常結束，自動重啟

## 九、畫面與互動需求

### 9.1 系統列圖示

需提供以下狀態：

1. 正常監聽中
2. API 無法連線
3. 讀卡機無輸入
4. 最近刷卡失敗

### 9.2 通知訊息

建議使用 Windows Toast 或右下角通知。

成功訊息示例：

1. `王小明 點名成功`
2. `王小明 遲到，到班時間 18:07`
3. `陳老師 上班打卡成功`
4. `陳老師 下班打卡成功`

失敗訊息示例：

1. `此卡尚未綁定學生或教師`
2. `今日沒有需要點名的課程`
3. `無法連線 WholeSummer 系統`

## 十、異常與風險處理

### 10.1 常見風險

1. 人工鍵盤輸入被誤判為刷卡
2. 輸入法或遠端桌面干擾鍵盤事件
3. 刷卡機未附 `Enter`，導致卡號無法完整結束
4. WholeSummer 未啟動，API 無法呼叫
5. 同一張卡在極短時間內重複刷入

### 10.2 對應措施

1. 設定輸入時間窗、長度限制、結束符判定
2. 規劃第二階段加入指定 HID 裝置過濾
3. 設定輸入逾時後自動結束卡號
4. 顯示 API 失敗通知並保留最近錯誤記錄
5. 由後端維持重複刷卡判斷規則

## 十一、開發階段規劃

### 第一階段：PoC 驗證

目標：

1. 證明刷卡機輸入可被背景程式接收
2. 可成功呼叫 WholeSummer 本機 API
3. 可顯示基本成功/失敗通知

工作項目：

1. 建立 `.NET` WinForms 專案
2. 完成 Raw Input 背景鍵盤輸入監聽
3. 完成卡號組裝與清洗
4. 完成 API 呼叫
5. 完成基本通知

### 第二階段：可上線版

目標：

1. 提升穩定性與設定能力
2. 加入自動啟動與單例保護
3. 整合 WholeSummer 啟動流程

工作項目：

1. 加入 `appsettings.json`
2. 加入系統列圖示與選單
3. 加入 `Mutex` 單例保護
4. Java 主程式自動啟動 `.NET`
5. 增加錯誤紀錄與狀態檢查

### 第三階段：穩定強化版

目標：

1. 進一步降低誤判
2. 提升設備相容性與維護性

工作項目：

1. 加入 `Raw Input` 指定裝置來源過濾
2. 增加最近刷卡紀錄視窗
3. 增加診斷模式與測試模式
4. 加入更新與版本檢查機制

## 十二、測試計畫

### 12.1 功能測試

1. 學生正常刷卡
2. 學生遲到刷卡
3. 學生第二次刷卡簽退
4. 教師第一次刷卡上班
5. 教師第二次刷卡下班
6. 未綁卡刷卡
7. 停用卡刷卡

### 12.2 環境測試

1. Windows 10
2. Windows 11
3. 單螢幕環境
4. 多螢幕環境
5. 有中文輸入法切換的環境
6. 遠端桌面使用情境

### 12.3 壓力與穩定性測試

1. 快速連續刷卡
2. 開機後自動啟動
3. WholeSummer 重啟時背景程式是否正常恢復
4. API 暫時失敗時的通知與恢復能力

## 十三、部署建議

建議將 `.NET` 程式與 WholeSummer 一起包裝進安裝目錄，例如：

```text
WholeSummer/
  WholeSummer.exe
  app/
    WholeSummer.jar
    tools/
      card-listener/
        WholeSummer.CardListener.exe
        appsettings.json
```

部署策略：

1. WholeSummer 安裝時一起安裝 `.NET` Card Listener
2. 主程式啟動時自動檢查其存在與版本
3. 若缺少檔案，提示重新安裝或修復

### 13.1 GitHub Actions 發布流程

建議使用既有 `.github/workflows/build-windows.yml`：

1. `actions/setup-java` 安裝 Java 建置環境。
2. `actions/setup-dotnet` 安裝 `.NET 8` 建置環境。
3. `mvn clean package -DskipTests` 建置 WholeSummer jar。
4. `dotnet publish tools\card-listener\WholeSummer.CardListener.csproj -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true` 產生單檔 exe。
5. 將 `WholeSummer.CardListener.exe` 與 `appsettings.json` 複製到：
   - `input\tools\card-listener\`
6. `jpackage --input input ...` 產生 Windows 安裝檔。
7. GitHub Release 上傳：
   - WholeSummer Windows installer
   - Card Listener 獨立 zip，供測試或現場臨時替換使用

正式使用時以 WholeSummer Windows installer 為主，獨立 zip 不作為一般使用者的必要安裝步驟。

## 十四、最終建議

本案建議採用：

`WholeSummer 主程式` + `Windows .NET 背景刷卡監聽程式` + `本機刷卡 API`

執行順序建議：

1. 先做 PoC 驗證鍵盤型刷卡機輸入是否穩定。
2. 再做可上線版，整合自動啟動與通知。
3. 若現場使用發現誤判，再升級為 `Raw Input` 裝置辨識版。

此方案可在不大幅改動現有 WholeSummer 後端邏輯的前提下，快速導入 Windows 背景刷卡能力，並保留未來擴充空間。
