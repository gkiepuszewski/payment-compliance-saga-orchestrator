import 'package:flutter/material.dart';

import '../models/payment.dart';
import 'payment_status_style.dart';

/// Read-only card showing a payment's current state. Reused both by the
/// "View" screen and by the result view shown right after submitting a new
/// payment, so both places render status consistently.
class PaymentStatusCard extends StatelessWidget {
  final Payment payment;

  const PaymentStatusCard({super.key, required this.payment});

  @override
  Widget build(BuildContext context) {
    final style = paymentStatusStyle(payment.status);
    return Card(
      margin: const EdgeInsets.all(16),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                CircleAvatar(
                  radius: 24,
                  backgroundColor: style.color.withValues(alpha: 0.15),
                  foregroundColor: style.color,
                  child: Icon(style.icon, size: 26),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Text(
                    '${payment.amount} ${payment.currency}',
                    style: Theme.of(context).textTheme.headlineSmall,
                  ),
                ),
                _StatusChip(style: style),
              ],
            ),
            const SizedBox(height: 20),
            _DetailRow(label: 'Payment ID', value: payment.id),
            _DetailRow(label: 'Payer', value: payment.payerId),
            _DetailRow(label: 'Payee', value: payment.payeeId),
            if (payment.cancelReason != null)
              _DetailRow(label: 'Cancel reason', value: payment.cancelReason!),
            _DetailRow(label: 'Created', value: _formatInstant(payment.createdAt)),
            _DetailRow(label: 'Updated', value: _formatInstant(payment.updatedAt)),
          ],
        ),
      ),
    );
  }
}

String _formatInstant(DateTime instant) {
  final local = instant.toLocal();
  String two(int n) => n.toString().padLeft(2, '0');
  return '${local.year}-${two(local.month)}-${two(local.day)} '
      '${two(local.hour)}:${two(local.minute)}:${two(local.second)}';
}

class _DetailRow extends StatelessWidget {
  final String label;
  final String value;

  const _DetailRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 110,
            child: Text(label, style: Theme.of(context).textTheme.bodySmall),
          ),
          Expanded(child: Text(value, overflow: TextOverflow.ellipsis)),
        ],
      ),
    );
  }
}

class _StatusChip extends StatelessWidget {
  final PaymentStatusStyle style;

  const _StatusChip({required this.style});

  @override
  Widget build(BuildContext context) {
    return Chip(
      avatar: Icon(style.icon, color: Colors.white, size: 18),
      label: Text(style.label),
      backgroundColor: style.color,
    );
  }
}
