package com.ecom.order.service;

import com.ecom.events.OrderCancelledEvent;
import com.ecom.events.OrderConfirmedEvent;
import com.ecom.events.OrderCreatedEvent;
import com.ecom.order.dto.CreateOrderRequest;
import com.ecom.order.kafka.OrderEventProducer;
import com.ecom.order.model.Order;
import com.ecom.order.model.OrderStatus;
import com.ecom.order.model.ProcessedEvent;
import com.ecom.order.repository.OrderRepository;
import com.ecom.order.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public Order placeOrder(CreateOrderRequest req) {
        Order order = Order.builder()
                .productId(req.getProductId())
                .quantity(req.getQuantity())
                .amount(req.getAmount())
                .status(OrderStatus.PENDING)
                .build();

        Order saved = orderRepository.save(order);
        log.info("Saved order id={} status={}", saved.getId(), saved.getStatus());

        orderEventProducer.publishOrderCreated(new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                saved.getId(),
                saved.getProductId(),
                saved.getQuantity(),
                saved.getAmount(),
                saved.getCreatedAt()
        ));

        return saved;
    }

    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional
    public void confirm(String eventId, Long orderId) {
        if (eventId != null && processedEventRepository.existsById(eventId)) {
            log.warn("Duplicate event ignored: eventId={}", eventId);
            return;
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("confirm: order {} not found", orderId);
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("confirm: order {} already in terminal state {}", orderId, order.getStatus());
            return;
        }
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        log.info("Order {} CONFIRMED", orderId);
        orderEventProducer.publishOrderConfirmed(new OrderConfirmedEvent(
                UUID.randomUUID().toString(), orderId));

        if (eventId != null) {
            processedEventRepository.save(new ProcessedEvent(eventId, "PaymentCompletedEvent"));
        }
    }

    @Transactional
    public void cancel(String eventId, Long orderId, String reason) {
        if (eventId != null && processedEventRepository.existsById(eventId)) {
            log.warn("Duplicate event ignored: eventId={}", eventId);
            return;
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("cancel: order {} not found", orderId);
            return;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("cancel: order {} already in terminal state {}", orderId, order.getStatus());
            return;
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order {} CANCELLED: {}", orderId, reason);
        orderEventProducer.publishOrderCancelled(new OrderCancelledEvent(
                UUID.randomUUID().toString(), orderId, reason));

        if (eventId != null) {
            processedEventRepository.save(new ProcessedEvent(eventId, "OrderCancelEvent"));
        }
    }
}
