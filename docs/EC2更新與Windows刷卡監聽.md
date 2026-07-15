# EC2 更新與 Windows 刷卡監聽

## 執行架構

- EC2：執行 `WholeSummer.jar`、MySQL、Flyway、備份、上傳檔案、Log、LINE Webhook 與排程。
- Windows：只執行 `WholeSummer.CardListener.exe`，以及保留 `C:\WholeSummer` 班級資料夾。
- Windows Card Listener 透過 `https://app.whole-summer.com/internal/desktop/card-check-in` 呼叫 EC2。

WholeSummer systemd 服務執行 `/opt/WholeSummer/current.jar`。此檔案是符號連結，會指向
`/opt/WholeSummer/releases/` 中目前啟用的版本，例如：

```text
/opt/WholeSummer/current.jar -> /opt/WholeSummer/releases/WholeSummer-1.4.4.jar
```

## EC2 更新權限

若要允許管理員從系統更新頁部署 JAR，將 `deploy/wholesummer-update.sudoers` 安裝到 EC2：

```bash
sudo install -o root -g root -m 0440 deploy/wholesummer-update.sudoers /etc/sudoers.d/wholesummer-update
sudo visudo -cf /etc/sudoers.d/wholesummer-update
```

確認 JAR 由 `wholesummer` 使用者可寫入：

```bash
sudo chown wholesummer:wholesummer /opt/WholeSummer/app/WholeSummer.jar
sudo -u wholesummer test -w /opt/WholeSummer/app/WholeSummer.jar
```

在 `/opt/WholeSummer/config/wholesummer.env` 啟用：

```bash
WHOLESUMMER_AUTO_UPDATE_ENABLED=true
WHOLESUMMER_JAR_PATH=/opt/WholeSummer/current.jar
WHOLESUMMER_RELEASE_DIR=/opt/WholeSummer/releases
WHOLESUMMER_SERVICE_NAME=wholesummer.service
```

修改後執行：

```bash
sudo systemctl restart wholesummer
sudo systemctl status wholesummer --no-pager
```

更新時不會覆寫舊版 JAR；新版會保留在 `releases`，再原子切換 `current.jar`。

## Windows 設定

解壓縮 GitHub Release 的 `WholeSummer-CardListener-v{version}.zip`，設定同目錄的
`appsettings.json`：

```json
{
  "WholeSummer": {
    "ApiBaseUrl": "https://app.whole-summer.com",
    "CheckInApiPath": "/internal/desktop/card-check-in",
    "ApiToken": "與 EC2 相同的 WHOLESUMMER_CARD_LISTENER_TOKEN"
  }
}
```

Windows 不需要安裝 JAR、Java、MySQL、Cloudflare 或 WholeSummer Windows Installer。
