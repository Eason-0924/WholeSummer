using System.Text;

namespace WholeSummer.CardListener;

internal sealed class CardInputBuffer
{
    private readonly CardReaderOptions options;
    private readonly StringBuilder buffer = new();
    private readonly object gate = new();
    private DateTime lastInputUtc = DateTime.MinValue;

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
                return buffer.Length;
            }
        }
    }

    public void Push(char value)
    {
        string? readyCard = null;
        CardRejectedEventArgs? rejected = null;
        bool acceptedInput = false;
        lock (gate)
        {
            if (value == '\b')
            {
                buffer.Clear();
                return;
            }
            if (value == '\r')
            {
                if (buffer.Length == 0)
                {
                    return;
                }
                acceptedInput = true;
                readyCard = FlushIfValid(out rejected);
            }
            else if (char.IsLetterOrDigit(value))
            {
                ResetWhenExpired();
                acceptedInput = true;
                buffer.Append(char.ToUpperInvariant(value));
                lastInputUtc = DateTime.UtcNow;
                if (buffer.Length > options.MaxLength)
                {
                    rejected = new CardRejectedEventArgs(buffer.Length, "卡號長度超過上限");
                    buffer.Clear();
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
        string? readyCard = null;
        CardRejectedEventArgs? rejected = null;
        lock (gate)
        {
            if (buffer.Length == 0 || options.UseEnterAsTerminator)
            {
                return;
            }
            if (DateTime.UtcNow - lastInputUtc >= TimeSpan.FromMilliseconds(options.InputTimeoutMs))
            {
                readyCard = FlushIfValid(out rejected);
            }
        }
        RaiseCardReady(readyCard);
        RaiseCardRejected(rejected);
    }

    private void ResetWhenExpired()
    {
        if (buffer.Length == 0)
        {
            return;
        }
        if (DateTime.UtcNow - lastInputUtc >= TimeSpan.FromMilliseconds(options.InputTimeoutMs))
        {
            buffer.Clear();
        }
    }

    private string? FlushIfValid(out CardRejectedEventArgs? rejected)
    {
        rejected = null;
        if (buffer.Length < options.MinLength || buffer.Length > options.MaxLength)
        {
            rejected = new CardRejectedEventArgs(buffer.Length, $"卡號長度需為 {options.MinLength}-{options.MaxLength}");
            buffer.Clear();
            return null;
        }
        var cardId = buffer.ToString();
        buffer.Clear();
        return cardId;
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
