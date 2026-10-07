package com.gk3.demo.payment.api;

import com.gk3.demo.payment.domain.Payment;
import com.gk3.demo.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String payerId,
        String payeeId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String cancelReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(), payment.getPayerId(), payment.getPayeeId(), payment.getAmount(),
                payment.getCurrency(), payment.getStatus(), payment.getCancelReason(),
                payment.getCreatedAt(), payment.getUpdatedAt());
    }
}
