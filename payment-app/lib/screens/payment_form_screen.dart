import 'package:flutter/material.dart';

import '../api/payment_api_client.dart';
import '../models/payment.dart';
import '../widgets/payment_status_card.dart';

/// New payment form. On successful submit, the form is replaced in-place by
/// a status view of the just-created payment (always PENDING right after
/// creation - the saga resolves it to CONFIRMED/CANCELLED asynchronously).
/// No auto-polling: the user presses "Refresh" to re-check the status, or
/// "Back" to return to the list (the list is not auto-refreshed either).
class PaymentFormScreen extends StatefulWidget {
  const PaymentFormScreen({super.key});

  @override
  State<PaymentFormScreen> createState() => _PaymentFormScreenState();
}

class _PaymentFormScreenState extends State<PaymentFormScreen> {
  final _formKey = GlobalKey<FormState>();
  final _payerIdController = TextEditingController();
  final _payeeIdController = TextEditingController();
  final _amountController = TextEditingController();
  final _currencyController = TextEditingController(text: 'USD');

  final PaymentApiClient _apiClient = PaymentApiClient();

  bool _submitting = false;
  bool _refreshing = false;
  String? _submitError;
  Payment? _result;

  @override
  void dispose() {
    _payerIdController.dispose();
    _payeeIdController.dispose();
    _amountController.dispose();
    _currencyController.dispose();
    _apiClient.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    setState(() {
      _submitting = true;
      _submitError = null;
    });
    try {
      final payment = await _apiClient.createPayment(
        payerId: _payerIdController.text.trim(),
        payeeId: _payeeIdController.text.trim(),
        amount: _amountController.text.trim(),
        currency: _currencyController.text.trim().toUpperCase(),
      );
      setState(() {
        _result = payment;
        _submitting = false;
      });
    } on PaymentApiException catch (e) {
      setState(() {
        _submitError = '${e.statusCode}: ${e.detail}';
        _submitting = false;
      });
    } catch (e) {
      setState(() {
        _submitError = 'Could not reach payment-service: $e';
        _submitting = false;
      });
    }
  }

  Future<void> _refreshResult() async {
    final current = _result;
    if (current == null) {
      return;
    }
    setState(() => _refreshing = true);
    try {
      final payment = await _apiClient.getPayment(current.id);
      setState(() {
        _result = payment;
        _refreshing = false;
      });
    } catch (e) {
      setState(() => _refreshing = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Refresh failed: $e')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final result = _result;
    return Scaffold(
      appBar: AppBar(title: Text(result == null ? 'New payment' : 'Payment submitted')),
      body: result == null ? _buildForm() : _buildResult(result),
    );
  }

  Widget _buildForm() {
    return Form(
      key: _formKey,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          TextFormField(
            controller: _payerIdController,
            decoration: const InputDecoration(labelText: 'Payer ID'),
            validator: (value) =>
                (value == null || value.trim().isEmpty) ? 'Payer ID is required' : null,
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _payeeIdController,
            decoration: const InputDecoration(labelText: 'Payee ID'),
            validator: (value) =>
                (value == null || value.trim().isEmpty) ? 'Payee ID is required' : null,
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _amountController,
            decoration: const InputDecoration(labelText: 'Amount'),
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            validator: (value) {
              final parsed = double.tryParse((value ?? '').trim());
              if (parsed == null || parsed < 0.01) {
                return 'Enter an amount of at least 0.01';
              }
              return null;
            },
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _currencyController,
            decoration: const InputDecoration(labelText: 'Currency (ISO 4217, e.g. USD)'),
            maxLength: 3,
            textCapitalization: TextCapitalization.characters,
            validator: (value) =>
                (value == null || value.trim().length != 3) ? 'Enter a 3-letter currency code' : null,
          ),
          if (_submitError != null) ...[
            const SizedBox(height: 8),
            Text(_submitError!, style: const TextStyle(color: Colors.red)),
          ],
          const SizedBox(height: 16),
          FilledButton(
            onPressed: _submitting ? null : _submit,
            child: _submitting
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Text('Submit payment'),
          ),
        ],
      ),
    );
  }

  Widget _buildResult(Payment payment) {
    return Column(
      children: [
        Expanded(
          child: ListView(
            children: [PaymentStatusCard(payment: payment)],
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
                  onPressed: _refreshing ? null : _refreshResult,
                  child: _refreshing
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
    );
  }
}
