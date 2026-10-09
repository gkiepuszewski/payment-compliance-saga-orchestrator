import 'package:flutter/material.dart';

import '../api/payment_api_client.dart';
import '../models/payment.dart';
import '../widgets/centered_content.dart';
import '../widgets/payment_status_style.dart';
import 'payment_detail_screen.dart';
import 'payment_form_screen.dart';

/// Landing screen: payments newest-first. Status shown here is just the
/// snapshot from the last list fetch (no live polling) - tap a row to fetch
/// its current status, pull-to-refresh/use the app bar's refresh button, or
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
            icon: const Icon(Icons.refresh_rounded),
          ),
        ],
      ),
      body: SafeArea(child: CenteredContent(child: _buildBody())),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _openNewPaymentForm,
        icon: const Icon(Icons.add_rounded),
        label: const Text('New payment'),
      ),
    );
  }

  Widget _buildBody() {
    if (_loading && _payments.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }

    // Both the empty and populated states are wrapped in RefreshIndicator +
    // an always-scrollable list, so pull-to-refresh works even with zero or
    // one screen's worth of rows.
    if (_error != null && _payments.isEmpty) {
      return _refreshableMessage(
        icon: Icons.cloud_off_rounded,
        iconColor: Theme.of(context).colorScheme.error,
        message: _error!,
        action: FilledButton.icon(
          onPressed: _loadPayments,
          icon: const Icon(Icons.refresh_rounded),
          label: const Text('Retry'),
        ),
      );
    }
    if (_payments.isEmpty) {
      return _refreshableMessage(
        icon: Icons.payments_outlined,
        iconColor: Theme.of(context).colorScheme.outline,
        message: 'No payments yet. Create the first one.',
      );
    }

    return RefreshIndicator(
      onRefresh: _loadPayments,
      child: ListView.builder(
        padding: const EdgeInsets.fromLTRB(12, 8, 12, 96),
        itemCount: _payments.length,
        itemBuilder: (context, index) => _PaymentCard(
          payment: _payments[index],
          onTap: () => _openPaymentDetail(_payments[index]),
        ),
      ),
    );
  }

  Widget _refreshableMessage({
    required IconData icon,
    required Color iconColor,
    required String message,
    Widget? action,
  }) {
    return RefreshIndicator(
      onRefresh: _loadPayments,
      child: LayoutBuilder(
        builder: (context, constraints) => SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          child: ConstrainedBox(
            constraints: BoxConstraints(minHeight: constraints.maxHeight),
            child: Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(icon, size: 64, color: iconColor),
                    const SizedBox(height: 16),
                    Text(
                      message,
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.bodyLarge,
                    ),
                    if (action != null) ...[
                      const SizedBox(height: 16),
                      action,
                    ],
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _PaymentCard extends StatelessWidget {
  final Payment payment;
  final VoidCallback onTap;

  const _PaymentCard({required this.payment, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final style = paymentStatusStyle(payment.status);
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Row(
            children: [
              CircleAvatar(
                backgroundColor: style.color.withValues(alpha: 0.15),
                foregroundColor: style.color,
                child: Icon(style.icon),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '${payment.amount} ${payment.currency}',
                      style: Theme.of(context)
                          .textTheme
                          .titleMedium
                          ?.copyWith(fontWeight: FontWeight.w600),
                    ),
                    Text(
                      '${payment.payerId} -> ${payment.payeeId}',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: Theme.of(context).colorScheme.onSurfaceVariant,
                          ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              _StatusBadge(status: payment.status),
              const Icon(Icons.chevron_right_rounded),
            ],
          ),
        ),
      ),
    );
  }
}

class _StatusBadge extends StatelessWidget {
  final PaymentStatus status;

  const _StatusBadge({required this.status});

  @override
  Widget build(BuildContext context) {
    final style = paymentStatusStyle(status);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(color: style.color, borderRadius: BorderRadius.circular(8)),
      child: Text(
        style.label,
        style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold),
      ),
    );
  }
}
