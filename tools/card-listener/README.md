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

## Notes

The first version receives Raw Input from keyboard-class devices while running in the background. It does not suppress the reader's keyboard output from the currently focused Windows application. If a deployment needs to accept input only from a specific reader device or suppress foreground typing, the next version should add device filtering and a low-level keyboard hook strategy.
