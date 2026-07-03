using System.Runtime.InteropServices;
using System.Windows.Forms;

namespace WholeSummer.CardListener;

internal sealed class RawInputForm : Form
{
    private const int WM_INPUT = 0x00FF;
    private const int RID_INPUT = 0x10000003;
    private const int RIM_TYPEKEYBOARD = 1;
    private const int RIDEV_INPUTSINK = 0x00000100;
    private const ushort RI_KEY_BREAK = 0x0001;
    private const ushort VK_BACK = 0x08;
    private const ushort VK_RETURN = 0x0D;

    private readonly CardInputBuffer inputBuffer;
    private bool registered;

    public RawInputForm(CardInputBuffer inputBuffer)
    {
        this.inputBuffer = inputBuffer;
        ShowInTaskbar = false;
        FormBorderStyle = FormBorderStyle.FixedToolWindow;
        Opacity = 0;
        StartPosition = FormStartPosition.Manual;
        Location = new System.Drawing.Point(-32000, -32000);
        Width = 1;
        Height = 1;
    }

    public bool Registered => registered;

    public int RawInputMessageCount { get; private set; }

    public int UnsupportedKeyCount { get; private set; }

    public ushort? LastVirtualKey { get; private set; }

    public ushort? LastMakeCode { get; private set; }

    public ushort? LastFlags { get; private set; }

    public char? LastResolvedKey { get; private set; }

    protected override void OnHandleCreated(EventArgs e)
    {
        base.OnHandleCreated(e);
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
                LastResolvedKey = keyInfo.Value.Key;
                if (keyInfo.Value.Key.HasValue)
                {
                    inputBuffer.Push(keyInfo.Value.Key.Value);
                }
                else
                {
                    UnsupportedKeyCount += 1;
                }
            }
        }
        base.WndProc(ref message);
    }

    private void RegisterKeyboardDevice()
    {
        RAWINPUTDEVICE[] devices =
        [
            new RAWINPUTDEVICE
            {
                usUsagePage = 0x01,
                usUsage = 0x06,
                dwFlags = RIDEV_INPUTSINK,
                hwndTarget = Handle
            }
        ];

        registered = RegisterRawInputDevices(devices, (uint)devices.Length, (uint)Marshal.SizeOf<RAWINPUTDEVICE>());
        if (!registered)
        {
            throw new InvalidOperationException("無法註冊鍵盤型刷卡機背景監聽");
        }
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

    private readonly record struct RawKeyInfo(ushort VirtualKey, ushort MakeCode, ushort Flags, char? Key);
}
