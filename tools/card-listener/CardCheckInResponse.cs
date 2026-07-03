namespace WholeSummer.CardListener;

internal sealed class CardCheckInResponse
{
    public bool Success { get; set; }

    public string? Status { get; set; }

    public string? Message { get; set; }

    public long? StudentId { get; set; }

    public string? StudentName { get; set; }

    public long? TeacherId { get; set; }

    public string? TeacherName { get; set; }

    public string? PersonType { get; set; }

    public string? ClassName { get; set; }

    public DateTime? CheckInTime { get; set; }

    public DateTime? CheckOutTime { get; set; }

    public string? CardId { get; set; }

    public string DisplayName => StudentName ?? TeacherName ?? CardId ?? "-";
}
