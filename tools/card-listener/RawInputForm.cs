using System.Runtime.InteropServices;
using System.Windows.Forms;

namespace WholeSummer.CardListener;

internal sealed class RawInputForm : Form
{
    private const int WM_INPUT = 0x00FF;
    private const int RID_INPUT = 0x10000003;
    private const int RIDI_DEVICENAME = 0x20000007;
    private const int RIM_TYPEKEYBOARD = 1;
    private const int RIDEV_INPUTSINK = 0x00000100;
    private const int RIDEV_DEVNOTIFY = 0x00002000;
    private const ushort RI_KEY_BREAK = 0x0001;
    private const ushort VK_BACK = 0x08;
    private const ushort VK_RETURN = 0x0D;

    private readonly CardInputBuffer inputBuffer;
    private readonly CardReaderOptions options;
    private readonly Func<char, bool> selectedReaderInputReceived;
    private readonly Dictionary<IntPtr, string> devicePaths = [];
    private bool registered;
    private bool learningReader;
    private string learningSwipeDevicePath = "";

    public RawInputForm(
        CardInputBuffer inputBuffer,
        CardReaderOptions options,
        Func<char, bool> selectedReaderInputReceived)
    {
        this.inputBuffer = inputBuffer;
        this.options = options;
        this.selectedReaderInputReceived = selectedReaderInputReceived;
        ShowInTaskbar = false;
        FormBorderStyle = FormBorderStyle.FixedToolWindow;
        Opacity = 0.01;
        StartPosition = FormStartPosition.Manual;
        Location = new System.Drawing.Point(-32000, -32000);
        Width = 1;
        Height = 1;
    }

    public bool Registered => registered;

    public int LastRegisterError { get; private set; }

    public int RawInputMessageCount { get; private set; }

    public int SelectedReaderInputCount { get; private set; }

    public int IgnoredReaderInputCount { get; private set; }

    public int UnsupportedKeyCount { get; private set; }

    public ushort? LastVirtualKey { get; private set; }

    public ushort? LastMakeCode { get; private set; }

    public ushort? LastFlags { get; private set; }

    public IntPtr? LastDeviceHandle { get; private set; }

    public string LastDevicePath { get; private set; } = "";

    public string LastIgnoredDevicePath { get; private set; } = "";

    public bool ReaderSelected => !string.IsNullOrWhiteSpace(options.ReaderDevicePath);

    public bool LearningReader => learningReader;

    public char? LastResolvedKey { get; private set; }

    public event EventHandler<ReaderDeviceLearnedEventArgs>? ReaderDeviceLearned;

    public void LearnNextReaderDevice()
    {
        learningReader = true;
    }

    public void RegisterForBackgroundInput()
    {
        RegisterKeyboardDevice();
    }

    protected override void OnHandleCreated(EventArgs e)
    {
        base.OnHandleCreated(e);
        RegisterKeyboardDevice();
    }

    protected override void OnShown(EventArgs e)
    {
        base.OnShown(e);
        RegisterKeyboardDevice();
    }

    protected override void WndProc(ref Message message)
    {
        if (message.Msg == WM_INPUT)
        {
            RawInputMessageCount += 1;
            RawKeyInfo? keyInfo = ReadRawKey(message.LParam);
            if (keyInfo.HasValue)
            {
                LastVirtualKey = keyInfo.Value.VirtualKey;
                LastMakeCode = keyInfo.Value.MakeCode;
                LastFlags = keyInfo.Value.Flags;
                LastDeviceHandle = keyInfo.Value.DeviceHandle;
                LastDevicePath = DevicePath(keyInfo.Value.DeviceHandle);
                LastResolvedKey = keyInfo.Value.Key;
                if (keyInfo.Value.Key.HasValue)
                {
                    if (IsDiscardingLearningSwipe(LastDevicePath, keyInfo.Value.Key.Value))
                    {
                        selectedReaderInputReceived(keyInfo.Value.Key.Value);
                        return;
                    }
                    if (learningReader)
                    {
                        learningReader = false;
                        learningSwipeDevicePath = LastDevicePath;
                        selectedReaderInputReceived(keyInfo.Value.Key.Value);
                        ReaderDeviceLearned?.Invoke(this,
                            new ReaderDeviceLearnedEventArgs(LastDevicePath, keyInfo.Value.DeviceHandle));
                        return;
                    }
                    if (IsSelectedReader(LastDevicePath))
                    {
                        SelectedReaderInputCount += 1;
                        if (selectedReaderInputReceived(keyInfo.Value.Key.Value))
                        {
                            inputBuffer.Push(
                                keyInfo.Value.Key.Value,
                                options.RequireSelectedReader ? IntPtr.Zero : keyInfo.Value.DeviceHandle);
                        }
                    }
                    else
                    {
                        IgnoredReaderInputCount += 1;
                        LastIgnoredDevicePath = LastDevicePath;
                    }
                }
                else
                {
                    UnsupportedKeyCount += 1;
                }
            }
        }
        base.WndProc(ref message);
    }

    private bool IsDiscardingLearningSwipe(string devicePath, char key)
    {
        if (string.IsNullOrWhiteSpace(learningSwipeDevicePath)
                || !string.Equals(
                    NormalizeDevicePath(devicePath),
                    NormalizeDevicePath(learningSwipeDevicePath),
                    StringComparison.OrdinalIgnoreCase))
        {
            return false;
        }
        if (key == '\r')
        {
            learningSwipeDevicePath = "";
        }
        return true;
    }

    private bool IsSelectedReader(string devicePath)
    {
        if (!options.RequireSelectedReader)
        {
            return true;
        }
        if (string.IsNullOrWhiteSpace(options.ReaderDevicePath))
        {
            return false;
        }
        bool exactPathMatch = string.Equals(
            NormalizeDevicePath(devicePath),
            NormalizeDevicePath(options.ReaderDevicePath),
            StringComparison.OrdinalIgnoreCase);
        if (exactPathMatch)
        {
            return true;
        }
        if (string.IsNullOrWhiteSpace(options.ReaderVid) || string.IsNullOrWhiteSpace(options.ReaderPid))
        {
            return false;
        }
        string normalizedPath = NormalizeDevicePath(devicePath).ToUpperInvariant();
        return normalizedPath.Contains("VID_" + options.ReaderVid.ToUpperInvariant(), StringComparison.Ordinal)
            && normalizedPath.Contains("PID_" + options.ReaderPid.ToUpperInvariant(), StringComparison.Ordinal);
    }

    private string DevicePath(IntPtr deviceHandle)
    {
        if (deviceHandle == IntPtr.Zero)
        {
            return "";
        }
        if (devicePaths.TryGetValue(deviceHandle, out string? cachedPath))
        {
            return cachedPath;
        }
        uint size = 0;
        GetRawInputDeviceInfo(deviceHandle, RIDI_DEVICENAME, IntPtr.Zero, ref size);
        if (size == 0)
        {
            return "";
        }
        IntPtr buffer = Marshal.AllocHGlobal((int)size * 2);
        try
        {
            uint result = GetRawInputDeviceInfo(deviceHandle, RIDI_DEVICENAME, buffer, ref size);
            if (result == uint.MaxValue)
            {
                return "";
            }
            string path = Marshal.PtrToStringUni(buffer) ?? "";
            devicePaths[deviceHandle] = path;
            return path;
        }
        finally
        {
            Marshal.FreeHGlobal(buffer);
        }
    }

    private static string NormalizeDevicePath(string devicePath)
    {
        return devicePath.Trim();
    }

    private void RegisterKeyboardDevice()
    {
        RAWINPUTDEVICE[] devices =
        [
            new RAWINPUTDEVICE
            {
                usUsagePage = 0x01,
                usUsage = 0x06,
                dwFlags = RIDEV_INPUTSINK | RIDEV_DEVNOTIFY,
                hwndTarget = Handle
            }
        ];

        registered = RegisterRawInputDevices(devices, (uint)devices.Length, (uint)Marshal.SizeOf<RAWINPUTDEVICE>());
        LastRegisterError = registered ? 0 : Marshal.GetLastWin32Error();
    }

    private static RawKeyInfo? ReadRawKey(IntPtr rawInputHandle)
    {
        uint size = 0;
        GetRawInputData(rawInputHandle, RID_INPUT, IntPtr.Zero, ref size,
            (uint)Marshal.SizeOf<RAWINPUTHEADER>());
        if (size == 0)
        {
            return null;
        }

        IntPtr buffer = Marshal.AllocHGlobal((int)size);
        try
        {
            uint copied = GetRawInputData(rawInputHandle, RID_INPUT, buffer, ref size,
                (uint)Marshal.SizeOf<RAWINPUTHEADER>());
            if (copied != size)
            {
                return null;
            }

            RAWINPUT rawInput = Marshal.PtrToStructure<RAWINPUT>(buffer);
            if (rawInput.header.dwType != RIM_TYPEKEYBOARD
                    || (rawInput.keyboard.Flags & RI_KEY_BREAK) == RI_KEY_BREAK)
            {
                return null;
            }
            return new RawKeyInfo(
                rawInput.keyboard.VKey,
                rawInput.keyboard.MakeCode,
                rawInput.keyboard.Flags,
                rawInput.header.hDevice,
                VirtualKeyToChar(rawInput.keyboard.VKey));
        }
        finally
        {
            Marshal.FreeHGlobal(buffer);
        }
    }

    private static char? VirtualKeyToChar(ushort virtualKey)
    {
        if (virtualKey == VK_RETURN)
        {
            return '\r';
        }
        if (virtualKey == VK_BACK)
        {
            return '\b';
        }
        if (virtualKey >= 0x30 && virtualKey <= 0x39)
        {
            return (char)virtualKey;
        }
        if (virtualKey >= 0x41 && virtualKey <= 0x5A)
        {
            return (char)virtualKey;
        }
        if (virtualKey >= 0x60 && virtualKey <= 0x69)
        {
            return (char)('0' + virtualKey - 0x60);
        }
        return null;
    }

    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool RegisterRawInputDevices(
        [MarshalAs(UnmanagedType.LPArray, SizeParamIndex = 1)] RAWINPUTDEVICE[] pRawInputDevices,
        uint uiNumDevices,
        uint cbSize);

    [DllImport("user32.dll", SetLastError = true)]
    private static extern uint GetRawInputData(
        IntPtr hRawInput,
        uint uiCommand,
        IntPtr pData,
        ref uint pcbSize,
        uint cbSizeHeader);

    [DllImport("user32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    private static extern uint GetRawInputDeviceInfo(
        IntPtr hDevice,
        uint uiCommand,
        IntPtr pData,
        ref uint pcbSize);

    [StructLayout(LayoutKind.Sequential)]
    private struct RAWINPUTDEVICE
    {
        public ushort usUsagePage;
        public ushort usUsage;
        public int dwFlags;
        public IntPtr hwndTarget;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct RAWINPUTHEADER
    {
        public uint dwType;
        public uint dwSize;
        public IntPtr hDevice;
        public IntPtr wParam;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct RAWKEYBOARD
    {
        public ushort MakeCode;
        public ushort Flags;
        public ushort Reserved;
        public ushort VKey;
        public uint Message;
        public uint ExtraInformation;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct RAWINPUT
    {
        public RAWINPUTHEADER header;
        public RAWKEYBOARD keyboard;
    }

    private readonly record struct RawKeyInfo(
        ushort VirtualKey,
        ushort MakeCode,
        ushort Flags,
        IntPtr DeviceHandle,
        char? Key);
}

internal sealed record ReaderDeviceLearnedEventArgs(string DevicePath, IntPtr DeviceHandle);
