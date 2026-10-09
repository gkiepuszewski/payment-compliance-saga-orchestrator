/// Base URL of `payment-service`'s REST API.
///
/// Overridable at build/run time without touching source, e.g.:
///   flutter run --dart-define=PAYMENT_SERVICE_BASE_URL=http://localhost:8081
const String paymentServiceBaseUrl = String.fromEnvironment(
  'PAYMENT_SERVICE_BASE_URL',
  defaultValue: 'http://localhost:8081',
);
