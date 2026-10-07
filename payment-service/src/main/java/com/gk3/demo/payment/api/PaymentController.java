package com.gk3.demo.payment.api;

import com.gk3.demo.payment.domain.Payment;
import com.gk3.demo.payment.domain.PaymentRepository;
import com.gk3.demo.payment.domain.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    public PaymentController(PaymentService paymentService, PaymentRepository paymentRepository) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = paymentService.createPending(
                request.payerId(), request.payeeId(), request.amount(), request.currency());
        return PaymentResponse.from(payment);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> get(@PathVariable UUID id) {
        return paymentRepository.findById(id)
                .map(PaymentResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<PaymentResponse> list() {
        return paymentRepository.findAll().stream().map(PaymentResponse::from).toList();
    }
}
