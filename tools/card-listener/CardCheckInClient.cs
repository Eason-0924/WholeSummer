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
        using var request = new HttpRequestMessage(HttpMethod.Post, settings.CheckInUri)
        {
            Content = JsonContent.Create(payload)
        };
        if (!string.IsNullOrWhiteSpace(settings.WholeSummer.ApiToken))
        {
            request.Headers.TryAddWithoutValidation("X-WholeSummer-Card-Token", settings.WholeSummer.ApiToken);
        }
        using var response = await httpClient.SendAsync(request, cancellationToken);
        if (!response.IsSuccessStatusCode)
        {
            string body = await response.Content.ReadAsStringAsync(cancellationToken);
            throw new HttpRequestException($"HTTP {(int)response.StatusCode} {response.ReasonPhrase}: {TrimBody(body)}");
        }
        var result = await response.Content.ReadFromJsonAsync<CardCheckInResponse>(jsonOptions, cancellationToken);
        return result ?? new CardCheckInResponse
        {
            Success = false,
            Status = "EMPTY_RESPONSE",
            Message = "WholeSummer 未回傳刷卡結果",
            CardId = cardId
        };
    }

    private static string TrimBody(string body)
    {
        if (string.IsNullOrWhiteSpace(body))
        {
            return "伺服器未回傳錯誤內容";
        }
        string normalized = body.Replace("\r", " ").Replace("\n", " ").Trim();
        return normalized.Length <= 500 ? normalized : normalized[..500] + "...";
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
