import 'package:flutter/material.dart';

import '../models/payment.dart';

/// Single source of truth for how each [PaymentStatus] is presented (label,
/// color, icon) - used by the list's status badge, the detail card's chip,
/// and the list row's leading icon, so all three always stay in sync.
class PaymentStatusStyle {
  final String label;
  final Color color;
  final IconData icon;

  const PaymentStatusStyle(this.label, this.color, this.icon);
}

PaymentStatusStyle paymentStatusStyle(PaymentStatus status) => switch (status) {
      PaymentStatus.pending =>
        const PaymentStatusStyle('PENDING', Colors.orange, Icons.hourglass_top_rounded),
      PaymentStatus.confirmed =>
        const PaymentStatusStyle('CONFIRMED', Colors.green, Icons.check_circle_rounded),
      PaymentStatus.cancelled =>
        const PaymentStatusStyle('CANCELLED', Colors.red, Icons.cancel_rounded),
      PaymentStatus.unknown =>
        const PaymentStatusStyle('UNKNOWN', Colors.grey, Icons.help_rounded),
    };
