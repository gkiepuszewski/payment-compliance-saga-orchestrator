import 'package:flutter/material.dart';

import '../api/payment_api_client.dart';
import '../models/payment.dart';
import '../widgets/payment_status_card.dart';

/// Shows the current status of a single payment, fetched fresh from
/// `payment-service` (not from the list's cached snapshot). Used both as the
/// "View" screen from the payments list and as the result view right after
/// submitting a new payment.
///
/// No auto-refresh/polling: the user explicitly presses "Refresh" to re-check
/// whether the saga has resolved the payment to CONFIRMED/CANCELLED yet, and
/// "Back" to return to the list (the list itself is not auto-refreshed).
class PaymentDetailScreen extends StatefulWidget {
  final String paymentId;

  /// Optional payment already fetched by the caller (e.g. the response of a
  /// just-submitted POST /api/payments), shown immediately while a fresh copy
  /// is not yet requested - avoids a redundant GET right after creation.
  final Payment? initialPayment;

  const PaymentDetailScreen({
    super.key,
    required this.paymentId,
    this.initialPayment,
  });

  @override
  State<PaymentDetailScreen> createState() => _PaymentDetailScreenState();
}

class _PaymentDetailScreenState extends State<PaymentDetailScreen> {
  final PaymentApiClient _apiClient = PaymentApiClient();

  Payment? _payment;
  String? _error;
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    _payment = widget.initialPayment;
    if (_payment == null) {
      _refresh();
    }
  }

  @override
  void dispose() {
    _apiClient.dispose();
    super.dispose();
  }

  Future<void> _refresh() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final payment = await _apiClient.getPayment(widget.paymentId);
      setState(() {
        _payment = payment;
        _loading = false;
      });
    } on PaymentApiException catch (e) {
      setState(() {
        _error = '${e.statusCode}: ${e.detail}';
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _error = 'Could not reach payment-service: $e';
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Payment status')),
      body: Column(
        children: [
          Expanded(
            child: _loading && _payment == null
                ? const Center(child: CircularProgressIndicator())
                : _error != null && _payment == null
                    ? Center(
                        child: Padding(
                          padding: const EdgeInsets.all(24),
                          child: Text(
                            _error!,
                            style: const TextStyle(color: Colors.red),
                            textAlign: TextAlign.center,
                          ),
                        ),
                      )
                    : ListView(
                        children: [
                          if (_payment != null) PaymentStatusCard(payment: _payment!),
                          if (_error != null)
                            Padding(
                              padding: const EdgeInsets.symmetric(horizontal: 24),
                              child: Text(
                                _error!,
                                style: const TextStyle(color: Colors.red),
                                textAlign: TextAlign.center,
                              ),
                            ),
                        ],
                      ),
          ),
          Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => Navigator.of(context).pop(),
                    child: const Text('Back'),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: FilledButton(
                    onPressed: _loading ? null : _refresh,
                    child: _loading
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Text('Refresh'),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
