# WholeSummer Windows Card Listener

This is the Windows tray application for keyboard-style card readers.

## Responsibilities

1. Listen to HID keyboard input in the background through Windows Raw Input.
2. Build a card id from fast reader input.
3. Send the card id to WholeSummer through the local desktop endpoint.
4. Show a Windows tray notification for success or failure.

## Build

Normal releases are built by GitHub Actions. The Windows installer includes:

```text
WholeSummer\app\tools\card-listener\WholeSummer.CardListener.exe
WholeSummer\app\tools\card-listener\appsettings.json
```

WholeSummer will try to start the executable automatically on Windows after the Spring Boot application is ready.

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

You can also run it manually for testing:

```powershell
.\WholeSummer.CardListener.exe --api-base-url http://127.0.0.1:8080
```

## Reader diagnostics

The listener uses Windows Raw Input so the user does not need to switch input method or focus a browser input before scanning.

```json
"CardReader": {
  "InputMode": "RawInput",
  "UseEnterAsTerminator": true
}
```

Use the tray menu's `顯示狀態` item to confirm whether Raw Input characters have been received. Use `測試 API 連線` to show a success/failure dialog for the local WholeSummer API.

## Notes

The listener does not suppress the reader's keyboard output from the currently focused Windows application. If a deployment needs to accept input only from a specific reader device or suppress foreground typing, a future version should add device filtering and an input-blocking strategy.
