import 'package:flutter/material.dart';

/// Constrains and centers its child to a comfortable reading/content width.
/// Without this, list rows/forms stretch edge-to-edge on wide desktop/web
/// windows, which looks sparse and makes rows harder to scan.
class CenteredContent extends StatelessWidget {
  final Widget child;
  final double maxWidth;

  const CenteredContent({super.key, required this.child, this.maxWidth = 640});

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.topCenter,
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: maxWidth),
        child: child,
      ),
    );
  }
}
