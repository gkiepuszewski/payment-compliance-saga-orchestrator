import 'package:flutter/material.dart';

import '../api/payment_api_client.dart';
import '../models/payment.dart';
import 'payment_detail_screen.dart';
import 'payment_form_screen.dart';

/// Landing screen: payments newest-first. Status shown here is just the
/// snapshot from the last list fetch (no live polling) - press "View" on a
/// row to fetch its current status, use the app bar's refresh button, or
/// simply return from the "New payment" form, which always refreshes the
/// list automatically.
class PaymentsListScreen extends StatefulWidget {
  const PaymentsListScreen({super.key});

  @override
  State<PaymentsListScreen> createState() => _PaymentsListScreenState();
}

class _PaymentsListScreenState extends State<PaymentsListScreen> {
  final PaymentApiClient _apiClient = PaymentApiClient();

  List<Payment> _payments = [];
  bool _loading = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadPayments();
  }

  @override
  void dispose() {
    _apiClient.dispose();
    super.dispose();
  }

  Future<void> _loadPayments() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final payments = await _apiClient.listPayments();
      setState(() {
        _payments = payments;
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

  Future<void> _openNewPaymentForm() async {
    await Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => const PaymentFormScreen()),
    );
    // Returning from the form always means either a payment was just created
    // or the user went "Back" - either way, refresh so the list is current.
    await _loadPayments();
  }

  Future<void> _openPaymentDetail(Payment payment) async {
    await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => PaymentDetailScreen(paymentId: payment.id),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Payments'),
        actions: [
          IconButton(
            tooltip: 'Refresh',
            onPressed: _loading ? null : _loadPayments,
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: _buildBody(),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _openNewPaymentForm,
        icon: const Icon(Icons.add),
        label: const Text('New payment'),
      ),
    );
  }

  Widget _buildBody() {
    if (_loading && _payments.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null && _payments.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                _error!,
                style: const TextStyle(color: Colors.red),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 12),
              FilledButton(onPressed: _loadPayments, child: const Text('Retry')),
            ],
          ),
        ),
      );
    }
    if (_payments.isEmpty) {
      return const Center(child: Text('No payments yet. Create the first one.'));
    }
    return ListView.separated(
      padding: const EdgeInsets.only(bottom: 88),
      itemCount: _payments.length,
      separatorBuilder: (_, _) => const Divider(height: 1),
      itemBuilder: (context, index) {
        final payment = _payments[index];
        return ListTile(
          title: Text('${payment.amount} ${payment.currency}'),
          subtitle: Text('${payment.payerId} -> ${payment.payeeId}'),
          trailing: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              _StatusBadge(status: payment.status),
              const SizedBox(width: 8),
              TextButton(
                onPressed: () => _openPaymentDetail(payment),
                child: const Text('View'),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _StatusBadge extends StatelessWidget {
  final PaymentStatus status;

  const _StatusBadge({required this.status});

  @override
  Widget build(BuildContext context) {
    final (label, color) = switch (status) {
      PaymentStatus.pending => ('PENDING', Colors.orange),
      PaymentStatus.confirmed => ('CONFIRMED', Colors.green),
      PaymentStatus.cancelled => ('CANCELLED', Colors.red),
      PaymentStatus.unknown => ('UNKNOWN', Colors.grey),
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(4)),
      child: Text(
        label,
        style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold),
      ),
    );
  }
}
