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

    public void Push(char value)
    {
        InputReceived?.Invoke(this, value);
        string? readyCard = null;
        lock (gate)
        {
            if (value == '\b')
            {
                buffer.Clear();
                return;
            }
            if (value == '\r')
            {
                readyCard = FlushIfValid();
            }
            else if (char.IsLetterOrDigit(value))
            {
                ResetWhenExpired();
                buffer.Append(char.ToUpperInvariant(value));
                lastInputUtc = DateTime.UtcNow;
                if (buffer.Length > options.MaxLength)
                {
                    buffer.Clear();
                }
            }
        }
        RaiseCardReady(readyCard);
    }

    public void FlushExpired()
    {
        string? readyCard = null;
        lock (gate)
        {
            if (buffer.Length == 0 || options.UseEnterAsTerminator)
            {
                return;
            }
            if (DateTime.UtcNow - lastInputUtc >= TimeSpan.FromMilliseconds(options.InputTimeoutMs))
            {
                readyCard = FlushIfValid();
            }
        }
        RaiseCardReady(readyCard);
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

    private string? FlushIfValid()
    {
        if (buffer.Length < options.MinLength || buffer.Length > options.MaxLength)
        {
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
}
