using Microsoft.Win32;

namespace WholeSummer.CardListener;

internal static class StartupRegistration
{
    private const string RunKeyPath = @"Software\Microsoft\Windows\CurrentVersion\Run";
    private const string ValueName = "WholeSummer.CardListener";

    public static void EnsureRegistered()
    {
        string executable = Environment.ProcessPath
            ?? throw new InvalidOperationException("找不到目前執行檔路徑");
        string configPath = Path.Combine(AppContext.BaseDirectory, "appsettings.json");
        string command = $"\"{executable}\" --config \"{configPath}\"";
        using RegistryKey? key = Registry.CurrentUser.CreateSubKey(RunKeyPath);
        key?.SetValue(ValueName, command, RegistryValueKind.String);
    }
}
