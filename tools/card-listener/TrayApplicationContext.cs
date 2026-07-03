using System.Windows.Forms;

namespace WholeSummer.CardListener;

internal sealed class TrayApplicationContext : ApplicationContext
{
    private readonly AppSettings settings;
    private readonly CardCheckInClient client;
    private readonly NotifyIcon notifyIcon;
    private readonly CardInputBuffer inputBuffer;
    private readonly RawInputForm rawInputForm;
    private readonly System.Windows.Forms.Timer flushTimer;
    private readonly List<string> recentRecords = [];
    private string statusText = "監聽中";
    private DateTime? lastInputAt;
    private int receivedInputCount;
    private int cardReadyCount;
    private int? lastCardLength;
    private DateTime? lastCardReadyAt;
    private DateTime? lastApiResultAt;
    private string? lastApiResult;
    private int? lastRejectedLength;
    private string? lastRejectedReason;
    private DateTime? lastRejectedAt;

    public TrayApplicationContext(AppSettings settings, CardCheckInClient client)
    {
        this.settings = settings;
        this.client = client;
        inputBuffer = new CardInputBuffer(settings.CardReader);
        inputBuffer.InputReceived += (_, _) =>
        {
            lastInputAt = DateTime.Now;
            receivedInputCount += 1;
        };
        inputBuffer.CardReady += async (_, cardId) => await ProcessCardAsync(cardId);
        inputBuffer.CardRejected += (_, rejected) =>
        {
            lastRejectedLength = rejected.Length;
            lastRejectedReason = rejected.Reason;
            lastRejectedAt = DateTime.Now;
            statusText = "卡號長度不符";
            string message = $"{rejected.Reason}（收到 {rejected.Length} 碼）";
            AddRecent($"{DateTime.Now:HH:mm:ss} 卡號未送出 {message}");
            ShowNotification("卡號未送出", message, ToolTipIcon.Warning);
        };

        notifyIcon = new NotifyIcon
        {
            Icon = System.Drawing.SystemIcons.Application,
            Text = "WholeSummer 刷卡監聽",
            Visible = true,
            ContextMenuStrip = CreateMenu()
        };
        notifyIcon.DoubleClick += (_, _) => ShowStatus();

        rawInputForm = new RawInputForm(inputBuffer);
        rawInputForm.Show();

        flushTimer = new System.Windows.Forms.Timer
        {
            Interval = Math.Max(100, settings.CardReader.InputTimeoutMs)
        };
        flushTimer.Tick += (_, _) => inputBuffer.FlushExpired();
        flushTimer.Start();

        ShowNotification("刷卡監聽已啟動", $"WholeSummer 刷卡背景程式正在監聽。模式：{settings.CardReader.InputMode}", ToolTipIcon.Info);
    }

    protected override void Dispose(bool disposing)
    {
        if (disposing)
        {
            flushTimer.Dispose();
            rawInputForm.Dispose();
            notifyIcon.Visible = false;
            notifyIcon.Dispose();
        }
        base.Dispose(disposing);
    }

    private ContextMenuStrip CreateMenu()
    {
        var menu = new ContextMenuStrip();
        menu.Items.Add("顯示狀態", null, (_, _) => ShowStatus());
        menu.Items.Add("測試通知", null, (_, _) => TestNotification());
        menu.Items.Add("測試 API 連線", null, async (_, _) => await TestConnectionAsync());
        menu.Items.Add("最近刷卡結果", null, (_, _) => ShowRecentRecords());
        menu.Items.Add(new ToolStripSeparator());
        menu.Items.Add("結束程式", null, (_, _) => ExitThread());
        return menu;
    }

    private async Task ProcessCardAsync(string cardId)
    {
        cardReadyCount += 1;
        lastCardLength = cardId.Length;
        lastCardReadyAt = DateTime.Now;
        statusText = "送出刷卡資料中";
        try
        {
            CardCheckInResponse response = await client.CheckInAsync(cardId);
            string title = response.Success ? "刷卡成功" : "刷卡失敗";
            string message = $"{response.DisplayName} {response.Message ?? response.Status ?? "-"}";
            lastApiResultAt = DateTime.Now;
            lastApiResult = $"{title} {message}";
            AddRecent($"{DateTime.Now:HH:mm:ss} {title} {message}");
            ShowNotification(title, message, response.Success ? ToolTipIcon.Info : ToolTipIcon.Warning);
            statusText = response.Success ? "最近刷卡成功" : "最近刷卡失敗";
        }
        catch (Exception ex)
        {
            string message = "無法連線 WholeSummer 系統：" + ex.Message;
            lastApiResultAt = DateTime.Now;
            lastApiResult = "連線失敗 " + message;
            AddRecent($"{DateTime.Now:HH:mm:ss} 連線失敗 {message}");
            ShowNotification("刷卡送出失敗", message, ToolTipIcon.Error);
            statusText = "API 無法連線";
        }
    }

    private void TestNotification()
    {
        string message = $"這是一則測試通知，時間：{DateTime.Now:HH:mm:ss}";
        AddRecent($"{DateTime.Now:HH:mm:ss} 測試通知 {message}");
        ShowNotification("WholeSummer 測試通知", message, ToolTipIcon.Info);
        MessageBox.Show(
            "已送出測試通知。\n\n如果這個視窗有出現，但 Windows 右下角沒有通知，通常是 Windows 通知設定、勿擾模式或通知中心規則擋住。",
            "WholeSummer 刷卡監聽",
            MessageBoxButtons.OK,
            MessageBoxIcon.Information);
    }

    private async Task TestConnectionAsync()
    {
        try
        {
            await client.TestConnectionAsync();
            string message = $"API 連線正常\n{settings.WholeSummer.ApiBaseUrl}";
            AddRecent($"{DateTime.Now:HH:mm:ss} API 測試成功 {settings.WholeSummer.ApiBaseUrl}");
            ShowNotification("API 連線正常", settings.WholeSummer.ApiBaseUrl, ToolTipIcon.Info);
            MessageBox.Show(message, "WholeSummer 刷卡監聽", MessageBoxButtons.OK, MessageBoxIcon.Information);
        }
        catch (Exception ex)
        {
            string message = $"API 連線失敗\n{ex.Message}";
            AddRecent($"{DateTime.Now:HH:mm:ss} API 測試失敗 {ex.Message}");
            ShowNotification("API 連線失敗", ex.Message, ToolTipIcon.Error);
            MessageBox.Show(message, "WholeSummer 刷卡監聽", MessageBoxButtons.OK, MessageBoxIcon.Error);
        }
    }

    private void ShowStatus()
    {
        string lastInputText = lastInputAt == null ? "尚未收到輸入" : lastInputAt.Value.ToString("HH:mm:ss");
        string lastCardText = lastCardReadyAt == null ? "尚未組成卡號" : $"{lastCardReadyAt.Value:HH:mm:ss}（{lastCardLength} 碼）";
        string lastApiText = lastApiResultAt == null ? "尚未送出 API" : $"{lastApiResultAt.Value:HH:mm:ss} {lastApiResult}";
        string rejectedText = lastRejectedAt == null ? "-" : $"{lastRejectedAt.Value:HH:mm:ss} {lastRejectedReason}（收到 {lastRejectedLength} 碼）";
        string rawKeyText = rawInputForm.LastVirtualKey == null
            ? "-"
            : $"VKey={rawInputForm.LastVirtualKey}, MakeCode={rawInputForm.LastMakeCode}, Flags={rawInputForm.LastFlags}, Char={(rawInputForm.LastResolvedKey == null ? "-" : rawInputForm.LastResolvedKey)}";
        MessageBox.Show(
            $"狀態：{statusText}\nAPI：{settings.WholeSummer.ApiBaseUrl}\n設備名稱：{settings.CardReader.DeviceName}\n監聽模式：{settings.CardReader.InputMode}\nRaw Input 註冊：{(rawInputForm.Registered ? "成功" : "失敗")}\nRaw Input 訊息數：{rawInputForm.RawInputMessageCount}\n未支援按鍵數：{rawInputForm.UnsupportedKeyCount}\n最近 Raw Key：{rawKeyText}\n卡號長度：{settings.CardReader.MinLength}-{settings.CardReader.MaxLength}\n逾時送出：{settings.CardReader.InputTimeoutMs} ms\n最近收到輸入：{lastInputText}\n收到有效字元次數：{receivedInputCount}\n目前緩衝長度：{inputBuffer.BufferedLength}\n已組成卡號次數：{cardReadyCount}\n最後組成卡號：{lastCardText}\n最後 API 結果：{lastApiText}\n最後未送出原因：{rejectedText}",
            "WholeSummer 刷卡監聽",
            MessageBoxButtons.OK,
            MessageBoxIcon.Information);
    }

    private void ShowRecentRecords()
    {
        string message = recentRecords.Count == 0
            ? "尚無刷卡紀錄"
            : string.Join(Environment.NewLine, recentRecords);
        MessageBox.Show(message, "最近刷卡結果", MessageBoxButtons.OK, MessageBoxIcon.Information);
    }

    private void AddRecent(string record)
    {
        recentRecords.Insert(0, record);
        if (recentRecords.Count > 10)
        {
            recentRecords.RemoveAt(recentRecords.Count - 1);
        }
    }

    private void ShowNotification(string title, string message, ToolTipIcon icon)
    {
        if (!settings.Notification.Enabled)
        {
            return;
        }
        notifyIcon.ShowBalloonTip(3500, title, message, icon);
    }
}
