using System.Text;

namespace WholeSummer.CardListener;

internal sealed class CardInputBuffer
{
    private readonly CardReaderOptions options;
    private readonly Dictionary<IntPtr, SourceBuffer> buffers = [];
    private readonly object gate = new();

    public CardInputBuffer(CardReaderOptions options)
    {
        this.options = options;
    }

    public event EventHandler<string>? CardReady;

    public event EventHandler<char>? InputReceived;

    public event EventHandler<CardRejectedEventArgs>? CardRejected;

    public int BufferedLength
    {
        get
        {
            lock (gate)
            {
                return buffers.Values.Sum(buffer => buffer.Value.Length);
            }
        }
    }

    public void Push(char value)
    {
        Push(value, IntPtr.Zero);
    }

    public void Push(char value, IntPtr sourceDevice)
    {
        string? readyCard = null;
        CardRejectedEventArgs? rejected = null;
        bool acceptedInput = false;
        lock (gate)
        {
            SourceBuffer source = GetSourceBuffer(sourceDevice);
            DateTime now = DateTime.UtcNow;
            if (value == '\b')
            {
                source.Clear();
                return;
            }
            if (value == '\r')
            {
                if (source.Value.Length == 0)
                {
                    return;
                }
                source.UpdateTerminatorTiming(now);
                acceptedInput = IsLikelyCardReaderInput(source);
                readyCard = FlushIfValid(source, out rejected);
            }
            else if (char.IsLetterOrDigit(value))
            {
                ResetWhenExpired(source, now);
                source.Append(char.ToUpperInvariant(value), now);
                acceptedInput = IsLikelyCardReaderInput(source);
                if (source.Value.Length > options.MaxLength)
                {
                    if (IsLikelyCardReaderInput(source))
                    {
                        rejected = new CardRejectedEventArgs(source.Value.Length, "卡號長度超過上限");
                    }
                    source.Clear();
                }
            }
        }
        if (acceptedInput)
        {
            InputReceived?.Invoke(this, value);
        }
        RaiseCardReady(readyCard);
        RaiseCardRejected(rejected);
    }

    public void FlushExpired()
    {
        List<string> readyCards = [];
        List<CardRejectedEventArgs> rejectedCards = [];
        lock (gate)
        {
            if (options.UseEnterAsTerminator)
            {
                return;
            }
            DateTime now = DateTime.UtcNow;
            foreach (SourceBuffer source in buffers.Values)
            {
                if (source.Value.Length == 0)
                {
                    continue;
                }
                if (now - source.LastInputUtc >= TimeSpan.FromMilliseconds(options.InputTimeoutMs))
                {
                    string? readyCard = FlushIfValid(source, out CardRejectedEventArgs? rejected);
                    if (!string.IsNullOrWhiteSpace(readyCard))
                    {
                        readyCards.Add(readyCard);
                    }
                    if (rejected != null)
                    {
                        rejectedCards.Add(rejected);
                    }
                }
            }
        }
        foreach (string readyCard in readyCards)
        {
            RaiseCardReady(readyCard);
        }
        foreach (CardRejectedEventArgs rejected in rejectedCards)
        {
            RaiseCardRejected(rejected);
        }
    }

    private SourceBuffer GetSourceBuffer(IntPtr sourceDevice)
    {
        if (!buffers.TryGetValue(sourceDevice, out SourceBuffer? source))
        {
            source = new SourceBuffer();
            buffers[sourceDevice] = source;
        }
        return source;
    }

    private void ResetWhenExpired(SourceBuffer source, DateTime now)
    {
        if (source.Value.Length == 0)
        {
            return;
        }
        if (now - source.LastInputUtc >= TimeSpan.FromMilliseconds(options.InputTimeoutMs))
        {
            source.Clear();
        }
    }

    private string? FlushIfValid(SourceBuffer source, out CardRejectedEventArgs? rejected)
    {
        rejected = null;
        bool likelyCardReaderInput = IsLikelyCardReaderInput(source);
        if (source.Value.Length < options.MinLength || source.Value.Length > options.MaxLength)
        {
            if (likelyCardReaderInput)
            {
                rejected = new CardRejectedEventArgs(source.Value.Length,
                    $"卡號長度需為 {options.MinLength}-{options.MaxLength}");
            }
            source.Clear();
            return null;
        }
        if (!likelyCardReaderInput)
        {
            source.Clear();
            return null;
        }
        var cardId = source.Value.ToString();
        source.Clear();
        return cardId;
    }

    private bool IsLikelyCardReaderInput(SourceBuffer source)
    {
        if (!options.RequireFastInput)
        {
            return true;
        }
        if (source.Value.Length <= 1)
        {
            return false;
        }
        return source.MaxInterKeyInterval <= TimeSpan.FromMilliseconds(options.MaxInterKeyIntervalMs)
            && source.TotalInputDuration <= TimeSpan.FromMilliseconds(options.MaxTotalInputMs);
    }

    private void RaiseCardReady(string? cardId)
    {
        if (!string.IsNullOrWhiteSpace(cardId))
        {
            CardReady?.Invoke(this, cardId);
        }
    }

    private void RaiseCardRejected(CardRejectedEventArgs? rejected)
    {
        if (rejected != null)
        {
            CardRejected?.Invoke(this, rejected);
        }
    }
}

internal sealed record CardRejectedEventArgs(int Length, string Reason);

internal sealed class SourceBuffer
{
    public StringBuilder Value { get; } = new();

    public DateTime FirstInputUtc { get; private set; } = DateTime.MinValue;

    public DateTime LastInputUtc { get; private set; } = DateTime.MinValue;

    public TimeSpan MaxInterKeyInterval { get; private set; } = TimeSpan.Zero;

    public TimeSpan TotalInputDuration => FirstInputUtc == DateTime.MinValue || LastInputUtc == DateTime.MinValue
        ? TimeSpan.Zero
        : LastInputUtc - FirstInputUtc;

    public void Append(char value, DateTime inputUtc)
    {
        if (Value.Length == 0)
        {
            FirstInputUtc = inputUtc;
        }
        else
        {
            TimeSpan interval = inputUtc - LastInputUtc;
            if (interval > MaxInterKeyInterval)
            {
                MaxInterKeyInterval = interval;
            }
        }
        Value.Append(value);
        LastInputUtc = inputUtc;
    }

    public void UpdateTerminatorTiming(DateTime inputUtc)
    {
        if (Value.Length == 0 || LastInputUtc == DateTime.MinValue)
        {
            return;
        }
        TimeSpan interval = inputUtc - LastInputUtc;
        if (interval > MaxInterKeyInterval)
        {
            MaxInterKeyInterval = interval;
        }
        LastInputUtc = inputUtc;
    }

    public void Clear()
    {
        Value.Clear();
        FirstInputUtc = DateTime.MinValue;
        LastInputUtc = DateTime.MinValue;
        MaxInterKeyInterval = TimeSpan.Zero;
    }
}
