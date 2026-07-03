using System.Net.Http.Json;
using System.Text.Json;

namespace WholeSummer.CardListener;

internal sealed class CardCheckInClient : IDisposable
{
    private readonly AppSettings settings;
    private readonly HttpClient httpClient;
    private readonly JsonSerializerOptions jsonOptions = new()
    {
        PropertyNameCaseInsensitive = true
    };

    public CardCheckInClient(AppSettings settings)
    {
        this.settings = settings;
        httpClient = new HttpClient
        {
            Timeout = TimeSpan.FromSeconds(5)
        };
    }

    public async Task<CardCheckInResponse> CheckInAsync(string cardId, CancellationToken cancellationToken = default)
    {
        var payload = new
        {
            cardId,
            deviceName = settings.CardReader.DeviceName
        };
        using var response = await httpClient.PostAsJsonAsync(settings.CheckInUri, payload, cancellationToken);
        response.EnsureSuccessStatusCode();
        var result = await response.Content.ReadFromJsonAsync<CardCheckInResponse>(jsonOptions, cancellationToken);
        return result ?? new CardCheckInResponse
        {
            Success = false,
            Status = "EMPTY_RESPONSE",
            Message = "WholeSummer 未回傳刷卡結果",
            CardId = cardId
        };
    }

    public async Task TestConnectionAsync(CancellationToken cancellationToken = default)
    {
        using var response = await httpClient.GetAsync(settings.WholeSummer.ApiBaseUrl, cancellationToken);
        if ((int)response.StatusCode >= 500)
        {
            response.EnsureSuccessStatusCode();
        }
    }

    public void Dispose()
    {
        httpClient.Dispose();
    }
}
