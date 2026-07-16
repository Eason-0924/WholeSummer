using System.Text.Json;
using System.Text.Json.Serialization;

namespace WholeSummer.CardListener;

internal sealed class AppSettings
{
    public WholeSummerOptions WholeSummer { get; set; } = new();

    public CardReaderOptions CardReader { get; set; } = new();

    public NotificationOptions Notification { get; set; } = new();

    [JsonIgnore]
    public string ConfigPath { get; private set; } = "appsettings.json";

    public static AppSettings Load(string[] args)
    {
        var configPath = ArgumentValue(args, "--config") ?? "appsettings.json";
        AppSettings settings = new();
        if (File.Exists(configPath))
        {
            var json = File.ReadAllText(configPath);
            settings = JsonSerializer.Deserialize<AppSettings>(json, new JsonSerializerOptions
            {
                PropertyNameCaseInsensitive = true
            }) ?? new AppSettings();
        }

        settings.WholeSummer.ApiBaseUrl = ArgumentValue(args, "--api-base-url")
            ?? settings.WholeSummer.ApiBaseUrl;
        settings.WholeSummer.ApiToken = ArgumentValue(args, "--api-token")
            ?? settings.WholeSummer.ApiToken;
        settings.CardReader.DeviceName = ArgumentValue(args, "--device-name")
            ?? settings.CardReader.DeviceName;

        settings.ConfigPath = configPath;
        settings.Normalize();
        return settings;
    }

    [JsonIgnore]
    public Uri CheckInUri => new(new Uri(WholeSummer.ApiBaseUrl), WholeSummer.CheckInApiPath);

    public void Save()
    {
        string json = JsonSerializer.Serialize(this, new JsonSerializerOptions
        {
            WriteIndented = true
        });
        File.WriteAllText(ConfigPath, json);
    }

    public void ClearReaderSelection()
    {
        CardReader.ReaderDevicePath = "";
        CardReader.ReaderVid = "";
        CardReader.ReaderPid = "";
    }

    private void Normalize()
    {
        if (string.IsNullOrWhiteSpace(WholeSummer.ApiBaseUrl))
        {
            WholeSummer.ApiBaseUrl = "http://127.0.0.1:8080";
        }
        if (!WholeSummer.ApiBaseUrl.EndsWith("/", StringComparison.Ordinal))
        {
            WholeSummer.ApiBaseUrl += "/";
        }
        if (string.IsNullOrWhiteSpace(WholeSummer.CheckInApiPath))
        {
            WholeSummer.CheckInApiPath = "/internal/desktop/card-check-in";
        }
        if (CardReader.MinLength < 1)
        {
            CardReader.MinLength = 1;
        }
        if (CardReader.MaxLength < CardReader.MinLength)
        {
            CardReader.MaxLength = CardReader.MinLength;
        }
        if (CardReader.InputTimeoutMs < 50)
        {
            CardReader.InputTimeoutMs = 50;
        }
        if (CardReader.MaxInterKeyIntervalMs < 20)
        {
            CardReader.MaxInterKeyIntervalMs = 20;
        }
        if (CardReader.MaxTotalInputMs < CardReader.MaxInterKeyIntervalMs)
        {
            CardReader.MaxTotalInputMs = CardReader.MaxInterKeyIntervalMs;
        }
        if (CardReader.SuppressWindowMs < CardReader.MaxInterKeyIntervalMs)
        {
            CardReader.SuppressWindowMs = CardReader.MaxInterKeyIntervalMs;
        }
        if (string.IsNullOrWhiteSpace(CardReader.DeviceName))
        {
            CardReader.DeviceName = "windows-card-listener";
        }
        if (string.IsNullOrWhiteSpace(CardReader.InputMode))
        {
            CardReader.InputMode = "RawInput";
        }
        if (!CardReader.InputMode.Equals("RawInput", StringComparison.OrdinalIgnoreCase))
        {
            CardReader.InputMode = "RawInput";
        }
        if (CardReader.RequireSelectedReader)
        {
            CardReader.RequireFastInput = false;
        }
    }

    private static string? ArgumentValue(string[] args, string key)
    {
        for (var i = 0; i < args.Length - 1; i++)
        {
            if (string.Equals(args[i], key, StringComparison.OrdinalIgnoreCase))
            {
                return args[i + 1];
            }
        }
        return null;
    }
}

internal sealed class WholeSummerOptions
{
    public string ApiBaseUrl { get; set; } = "http://127.0.0.1:8080";

    public string CheckInApiPath { get; set; } = "/internal/desktop/card-check-in";

    public string ApiToken { get; set; } = "";
}

internal sealed class CardReaderOptions
{
    public string DeviceName { get; set; } = "windows-card-listener";

    public string InputMode { get; set; } = "RawInput";

    public int MinLength { get; set; } = 6;

    public int MaxLength { get; set; } = 32;

    public int InputTimeoutMs { get; set; } = 200;

    public bool UseEnterAsTerminator { get; set; } = true;

    public bool RequireFastInput { get; set; } = false;

    public bool RequireSelectedReader { get; set; } = true;

    public string ReaderDevicePath { get; set; } = "";

    public string ReaderVid { get; set; } = "";

    public string ReaderPid { get; set; } = "";

    public int MaxInterKeyIntervalMs { get; set; } = 120;

    public int MaxTotalInputMs { get; set; } = 1500;

    public bool SuppressKeyboardInput { get; set; } = true;

    public int SuppressWindowMs { get; set; } = 800;
}

internal sealed class NotificationOptions
{
    public bool Enabled { get; set; } = true;

    public bool ReplacePrevious { get; set; } = true;
}
