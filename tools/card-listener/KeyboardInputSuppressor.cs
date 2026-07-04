using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Windows.Forms;

namespace WholeSummer.CardListener;

internal sealed class KeyboardInputSuppressor : IDisposable
{
    private const int WH_KEYBOARD_LL = 13;
    private const int WM_KEYDOWN = 0x0100;
    private const int WM_SYSKEYDOWN = 0x0104;

    private readonly CardReaderOptions options;
    private readonly LowLevelKeyboardProc hookProc;
    private readonly object gate = new();
    private IntPtr hookHandle;
    private DateTime suppressUntilUtc = DateTime.MinValue;
    private bool disposed;

    public KeyboardInputSuppressor(CardReaderOptions options)
    {
        this.options = options;
        hookProc = HookCallback;
        if (options.SuppressKeyboardInput)
        {
            hookHandle = SetHook(hookProc);
        }
    }

    public bool Enabled => options.SuppressKeyboardInput && hookHandle != IntPtr.Zero;

    public int SuppressedKeyCount { get; private set; }

    public DateTime? LastSuppressedAt { get; private set; }

    public string LastSuppressedKey { get; private set; } = "-";

    public void AllowSelectedReaderKey(char key)
    {
        if (!options.SuppressKeyboardInput || !IsSupportedReaderKey(key))
        {
            return;
        }
        lock (gate)
        {
            suppressUntilUtc = DateTime.UtcNow.AddMilliseconds(options.SuppressWindowMs);
        }
    }

    private IntPtr HookCallback(int nCode, IntPtr wParam, IntPtr lParam)
    {
        if (nCode >= 0 && (wParam == new IntPtr(WM_KEYDOWN) || wParam == new IntPtr(WM_SYSKEYDOWN)))
        {
            KBDLLHOOKSTRUCT keyInfo = Marshal.PtrToStructure<KBDLLHOOKSTRUCT>(lParam);
            char? key = VirtualKeyToChar(keyInfo.vkCode);
            if (key.HasValue && ShouldSuppress(key.Value))
            {
                SuppressedKeyCount += 1;
                LastSuppressedAt = DateTime.Now;
                LastSuppressedKey = key.Value == '\r' ? "Enter" : key.Value.ToString();
                return new IntPtr(1);
            }
        }
        return CallNextHookEx(hookHandle, nCode, wParam, lParam);
    }

    private bool ShouldSuppress(char key)
    {
        lock (gate)
        {
            DateTime now = DateTime.UtcNow;
            bool currentlySuppressing = now <= suppressUntilUtc;
            if (!IsSupportedReaderKey(key))
            {
                return false;
            }
            return currentlySuppressing;
        }
    }

    private static bool IsSupportedReaderKey(char key)
    {
        return key == '\r' || char.IsDigit(key);
    }

    private static IntPtr SetHook(LowLevelKeyboardProc proc)
    {
        using Process currentProcess = Process.GetCurrentProcess();
        using ProcessModule? currentModule = currentProcess.MainModule;
        IntPtr moduleHandle = currentModule == null
            ? IntPtr.Zero
            : GetModuleHandle(currentModule.ModuleName);
        return SetWindowsHookEx(WH_KEYBOARD_LL, proc, moduleHandle, 0);
    }

    private static char? VirtualKeyToChar(uint virtualKey)
    {
        if (virtualKey == 0x0D)
        {
            return '\r';
        }
        if (virtualKey >= 0x30 && virtualKey <= 0x39)
        {
            return (char)virtualKey;
        }
        if (virtualKey >= 0x60 && virtualKey <= 0x69)
        {
            return (char)('0' + virtualKey - 0x60);
        }
        return null;
    }

    public void Dispose()
    {
        if (disposed)
        {
            return;
        }
        disposed = true;
        if (hookHandle != IntPtr.Zero)
        {
            UnhookWindowsHookEx(hookHandle);
            hookHandle = IntPtr.Zero;
        }
    }

    private delegate IntPtr LowLevelKeyboardProc(int nCode, IntPtr wParam, IntPtr lParam);

    [DllImport("user32.dll", SetLastError = true)]
    private static extern IntPtr SetWindowsHookEx(int idHook, LowLevelKeyboardProc lpfn, IntPtr hMod, uint dwThreadId);

    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool UnhookWindowsHookEx(IntPtr hhk);

    [DllImport("user32.dll", SetLastError = true)]
    private static extern IntPtr CallNextHookEx(IntPtr hhk, int nCode, IntPtr wParam, IntPtr lParam);

    [DllImport("kernel32.dll", CharSet = CharSet.Auto, SetLastError = true)]
    private static extern IntPtr GetModuleHandle(string? lpModuleName);

    [StructLayout(LayoutKind.Sequential)]
    private struct KBDLLHOOKSTRUCT
    {
        public uint vkCode;
        public uint scanCode;
        public uint flags;
        public uint time;
        public IntPtr dwExtraInfo;
    }
}
