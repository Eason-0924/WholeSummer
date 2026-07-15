using System.Runtime.InteropServices;
using System.Windows.Forms;

namespace WholeSummer.CardListener;

internal static class Program
{
    [STAThread]
    private static void Main(string[] args)
    {
        SetCurrentProcessExplicitAppUserModelID("WholeSummer.CardListener");

        using var instanceGuard = SingleInstanceGuard.Acquire("Global\\WholeSummer.CardListener");
        if (!instanceGuard.HasHandle)
        {
            return;
        }

        Application.EnableVisualStyles();
        Application.SetCompatibleTextRenderingDefault(false);

        var settings = AppSettings.Load(args);
        try
        {
            StartupRegistration.EnsureRegistered();
        }
        catch
        {
            // The listener can still run manually if startup registration is unavailable.
        }
        using var client = new CardCheckInClient(settings);
        Application.Run(new TrayApplicationContext(settings, client));
    }

    [DllImport("shell32.dll", CharSet = CharSet.Unicode)]
    private static extern int SetCurrentProcessExplicitAppUserModelID(string appID);
}
