package com.ecom.payment.service;

import com.ecom.events.InventoryReservedEvent;
import com.ecom.events.PaymentCompletedEvent;
import com.ecom.events.PaymentFailedEvent;
import com.ecom.payment.kafka.PaymentEventProducer;
import com.ecom.payment.model.Payment;
import com.ecom.payment.model.PaymentStatus;
import com.ecom.payment.model.ProcessedEvent;
import com.ecom.payment.repository.PaymentRepository;
import com.ecom.payment.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer producer;
    private final ProcessedEventRepository processedEventRepository;

    @Value("${payment.fail-above-amount:1000}")
    private BigDecimal failAboveAmount;

    @Transactional
    public void process(InventoryReservedEvent event) {
        if (event.getEventId() != null && processedEventRepository.existsById(event.getEventId())) {
            log.warn("Duplicate event ignored: eventId={}", event.getEventId());
            return;
        }

        boolean success = event.getAmount().compareTo(failAboveAmount) <= 0;

        Payment payment = Payment.builder()
                .orderId(event.getOrderId())
                .amount(event.getAmount())
                .status(success ? PaymentStatus.COMPLETED : PaymentStatus.FAILED)
                .build();
        Payment saved = paymentRepository.save(payment);

        if (success) {
            log.info("Payment COMPLETED for order {}: amount={}", event.getOrderId(), event.getAmount());
            producer.publishCompleted(new PaymentCompletedEvent(
                    UUID.randomUUID().toString(),
                    saved.getOrderId(), saved.getId(), saved.getAmount()));
        } else {
            String reason = "Amount " + event.getAmount() + " exceeds limit " + failAboveAmount;
            log.warn("Payment FAILED for order {}: {}", event.getOrderId(), reason);
            producer.publishFailed(new PaymentFailedEvent(
                    UUID.randomUUID().toString(),
                    saved.getOrderId(),
                    event.getProductId(),
                    event.getQuantity(),
                    reason
            ));
        }

        if (event.getEventId() != null) {
            processedEventRepository.save(new ProcessedEvent(event.getEventId(), "InventoryReservedEvent"));
        }
    }
}
