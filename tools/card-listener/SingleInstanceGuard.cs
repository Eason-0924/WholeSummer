using System.Threading;

namespace WholeSummer.CardListener;

internal sealed class SingleInstanceGuard : IDisposable
{
    private readonly Mutex mutex;

    private SingleInstanceGuard(Mutex mutex, bool hasHandle)
    {
        this.mutex = mutex;
        HasHandle = hasHandle;
    }

    public bool HasHandle { get; }

    public static SingleInstanceGuard Acquire(string name)
    {
        var mutex = new Mutex(initiallyOwned: true, name, out bool createdNew);
        return new SingleInstanceGuard(mutex, createdNew);
    }

    public void Dispose()
    {
        if (HasHandle)
        {
            mutex.ReleaseMutex();
        }
        mutex.Dispose();
    }
}
