using System.Windows.Forms;

namespace WholeSummer.CardListener;

internal static class Program
{
    [STAThread]
    private static void Main(string[] args)
    {
        using var instanceGuard = SingleInstanceGuard.Acquire("Global\\WholeSummer.CardListener");
        if (!instanceGuard.HasHandle)
        {
            return;
        }

        Application.EnableVisualStyles();
        Application.SetCompatibleTextRenderingDefault(false);

        var settings = AppSettings.Load(args);
        using var client = new CardCheckInClient(settings);
        Application.Run(new TrayApplicationContext(settings, client));
    }
}
