import 'package:flutter/material.dart';

import '../models/payment.dart';

/// Read-only card showing a payment's current state. Reused both by the
/// "View" screen and by the result view shown right after submitting a new
/// payment, so both places render status consistently.
class PaymentStatusCard extends StatelessWidget {
  final Payment payment;

  const PaymentStatusCard({super.key, required this.payment});

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.all(16),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  '${payment.amount} ${payment.currency}',
                  style: Theme.of(context).textTheme.headlineSmall,
                ),
                _StatusChip(status: payment.status),
              ],
            ),
            const SizedBox(height: 12),
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
      padding: const EdgeInsets.symmetric(vertical: 2),
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
  final PaymentStatus status;

  const _StatusChip({required this.status});

  @override
  Widget build(BuildContext context) {
    final (label, color) = switch (status) {
      PaymentStatus.pending => ('PENDING', Colors.orange),
      PaymentStatus.confirmed => ('CONFIRMED', Colors.green),
      PaymentStatus.cancelled => ('CANCELLED', Colors.red),
      PaymentStatus.unknown => ('UNKNOWN', Colors.grey),
    };
    return Chip(
      label: Text(label, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
      backgroundColor: color,
    );
  }
}
