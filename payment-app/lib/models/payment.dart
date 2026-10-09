/// Mirrors `payment-service`'s `PaymentStatus` enum.
enum PaymentStatus { pending, confirmed, cancelled, unknown }

PaymentStatus paymentStatusFromJson(String value) {
  switch (value) {
    case 'PENDING':
      return PaymentStatus.pending;
    case 'CONFIRMED':
      return PaymentStatus.confirmed;
    case 'CANCELLED':
      return PaymentStatus.cancelled;
    default:
      return PaymentStatus.unknown;
  }
}

/// Mirrors `payment-service`'s `PaymentResponse` DTO
/// (`com.gk3.demo.payment.api.PaymentResponse`).
class Payment {
  final String id;
  final String payerId;
  final String payeeId;
  final String amount;
  final String currency;
  final PaymentStatus status;
  final String? cancelReason;
  final DateTime createdAt;
  final DateTime updatedAt;

  Payment({
    required this.id,
    required this.payerId,
    required this.payeeId,
    required this.amount,
    required this.currency,
    required this.status,
    required this.cancelReason,
    required this.createdAt,
    required this.updatedAt,
  });

  factory Payment.fromJson(Map<String, dynamic> json) {
    return Payment(
      id: json['id'] as String,
      payerId: json['payerId'] as String,
      payeeId: json['payeeId'] as String,
      // amount comes over the wire as a JSON number; keep the server's
      // textual representation to avoid floating-point surprises.
      amount: json['amount'].toString(),
      currency: json['currency'] as String,
      status: paymentStatusFromJson(json['status'] as String),
      cancelReason: json['cancelReason'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: DateTime.parse(json['updatedAt'] as String),
    );
  }
}
