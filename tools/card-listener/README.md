# WholeSummer Windows Card Listener

This is the Windows tray application for keyboard-style card readers.

## Responsibilities

1. Listen to HID keyboard input in the background through Windows Raw Input.
2. Build a card id from fast reader input.
3. Send the card id to WholeSummer through the configured desktop endpoint.
4. Ask the user to scan one card on first launch to bind the Raw Input reader source.
5. Only accept input from the selected Raw Input device path.
6. Suppress numeric card-reader keystrokes only after the selected Raw Input source is detected.
7. Show a Windows tray notification for success or failure.

## Build

Normal releases are built by GitHub Actions. The Windows installer includes:

```text
WholeSummer\app\tools\card-listener\WholeSummer.CardListener.exe
WholeSummer\app\tools\card-listener\appsettings.json
```

WholeSummer will try to start the executable automatically on Windows after the Spring Boot application is ready.

Card Listener 使用獨立版本號，版本存放在 `tools/card-listener/VERSION`（目前為 `4`），發布檔名為
`WholeSummer-CardListener-v4.zip`。只有 Card Listener 目錄有變更時，Release 才會包含新版 Card Listener；
修改 Card Listener 時請同步遞增 `VERSION`。

首次啟動後，Card Listener 會加入目前 Windows 使用者的登入啟動項目，之後登入 Windows
會自動執行。每次 Card Listener 重新啟動時，會先比對已記住的讀卡機來源；若 30 秒內找到就沿用，
找不到才會顯示提示並要求重新刷一張卡完成設定。若裝置路徑改變但 VID/PID 相同，會自動更新設定檔中的 Device Path。
若在 30 秒內連續三次只收到 1 碼，程式會顯示通知並詢問是否重新設定讀卡機。

For local manual builds, install .NET 8 SDK on Windows, then run:

```powershell
dotnet publish .\WholeSummer.CardListener.csproj -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true
```

Copy the published `WholeSummer.CardListener.exe` and `appsettings.json` to one of these folders:

```text
<project>\tools\card-listener\
<installed WholeSummer>\app\tools\card-listener\
```

## Runtime arguments

WholeSummer starts the listener with:

```text
--api-base-url http://127.0.0.1:{port} --device-name windows-card-listener
```

For a Windows reader that remains outside the Ubuntu VPS, set `WholeSummer.ApiBaseUrl`
to `https://app.whole-summer.com` and set `WholeSummer.ApiToken` to the same secret as
the VPS environment variable `WHOLESUMMER_CARD_LISTENER_TOKEN`. The token is sent in
the `X-WholeSummer-Card-Token` header. Do not commit the real token to source control.
The public endpoint is rejected unless this token is configured and matches.

You can also run it manually for testing:

```powershell
.\WholeSummer.CardListener.exe --api-base-url http://127.0.0.1:8080
```

## Reader diagnostics

The listener uses Windows Raw Input so the user does not need to switch input method or focus a browser input before scanning.

```json
"CardReader": {
  "InputMode": "RawInput",
  "UseEnterAsTerminator": true,
  "RequireFastInput": false,
  "RequireSelectedReader": true,
  "ReaderDevicePath": "",
  "SuppressKeyboardInput": true,
  "SuppressWindowMs": 800
}
```

On first launch, if `ReaderDevicePath` is empty, the listener asks the user to scan any card once. That scan is used only to bind the reader source and is not sent to WholeSummer.

Use the tray menu's `重新設定讀卡機` item to bind a different reader. Use `顯示狀態` to confirm the selected reader path, ignored non-reader input count, Raw Input messages, and keyboard suppression count. Use `測試 API 連線` to show a success/failure dialog for the configured WholeSummer API.

## Notes

Keyboard suppression only targets numeric keys and Enter after Raw Input confirms that the selected reader is producing input. It is a focused input-protection fallback, not the primary source detection mechanism. The primary source check is always the Raw Input device path. When suppression consumes reader keystrokes before they reach the foreground app, those suppressed digits and Enter are forwarded into the card buffer so background scanning does not stop after the first Raw Input digit. After a selected reader source is confirmed, card validity is based on card length and the reader's Enter/timeout behavior, not keyboard input speed. If the reader's Enter key is not delivered reliably, selected-reader input is also flushed by timeout as a fallback.
