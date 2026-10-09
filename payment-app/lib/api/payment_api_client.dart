import 'dart:convert';

import 'package:http/http.dart' as http;

import '../config.dart';
import '../models/payment.dart';

/// Thrown when `payment-service` responds with a non-2xx status.
///
/// [detail] is best-effort extracted from Spring's default
/// `ProblemDetail`/validation error JSON body (falls back to the raw body).
class PaymentApiException implements Exception {
  final int statusCode;
  final String detail;

  PaymentApiException(this.statusCode, this.detail);

  @override
  String toString() => 'PaymentApiException($statusCode): $detail';
}

class PaymentApiClient {
  final http.Client _client;
  final String baseUrl;

  PaymentApiClient({http.Client? client, this.baseUrl = paymentServiceBaseUrl})
      : _client = client ?? http.Client();

  /// GET /api/payments?sort=createdAt,desc - newest payments first.
  Future<List<Payment>> listPayments() async {
    final uri = Uri.parse('$baseUrl/api/payments?sort=createdAt,desc&size=50');
    final response = await _client.get(uri);
    _throwIfNotOk(response);
    final body = jsonDecode(response.body) as Map<String, dynamic>;
    final content = body['content'] as List<dynamic>? ?? const [];
    return content
        .map((item) => Payment.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  /// GET /api/payments/{id} - current status of a single payment.
  Future<Payment> getPayment(String id) async {
    final uri = Uri.parse('$baseUrl/api/payments/$id');
    final response = await _client.get(uri);
    _throwIfNotOk(response);
    return Payment.fromJson(jsonDecode(response.body) as Map<String, dynamic>);
  }

  /// POST /api/payments - creates a new payment (always comes back PENDING;
  /// the saga resolves it to CONFIRMED/CANCELLED asynchronously).
  Future<Payment> createPayment({
    required String payerId,
    required String payeeId,
    required String amount,
    required String currency,
  }) async {
    final uri = Uri.parse('$baseUrl/api/payments');
    final response = await _client.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'payerId': payerId,
        'payeeId': payeeId,
        'amount': amount,
        'currency': currency,
      }),
    );
    _throwIfNotOk(response, expected: 201);
    return Payment.fromJson(jsonDecode(response.body) as Map<String, dynamic>);
  }

  void _throwIfNotOk(http.Response response, {int expected = 200}) {
    if (response.statusCode == expected) {
      return;
    }
    String detail = response.body;
    try {
      final decoded = jsonDecode(response.body);
      if (decoded is Map<String, dynamic>) {
        detail = (decoded['detail'] ?? decoded['message'] ?? response.body)
            .toString();
      }
    } catch (_) {
      // Body wasn't JSON - keep the raw text as the detail.
    }
    throw PaymentApiException(response.statusCode, detail);
  }

  void dispose() => _client.close();
}
