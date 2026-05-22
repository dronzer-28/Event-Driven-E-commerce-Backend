package com.ecom.inventory.service;

import com.ecom.events.InventoryFailedEvent;
import com.ecom.events.InventoryReservedEvent;
import com.ecom.events.OrderCreatedEvent;
import com.ecom.events.PaymentFailedEvent;
import com.ecom.inventory.kafka.InventoryEventProducer;
import com.ecom.inventory.model.InventoryItem;
import com.ecom.inventory.model.ProcessedEvent;
import com.ecom.inventory.repository.InventoryRepository;
import com.ecom.inventory.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryEventProducer producer;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public void reserve(OrderCreatedEvent event) {
        if (event.getEventId() != null && processedEventRepository.existsById(event.getEventId())) {
            log.warn("Duplicate event ignored: eventId={}", event.getEventId());
            return;
        }

        Optional<InventoryItem> maybe = inventoryRepository.findByProductIdForUpdate(event.getProductId());

        if (maybe.isEmpty()) {
            producer.publishFailed(new InventoryFailedEvent(
                    UUID.randomUUID().toString(),
                    event.getOrderId(), event.getProductId(), "Product not found"));
            if (event.getEventId() != null) {
                processedEventRepository.save(new ProcessedEvent(event.getEventId(), "OrderCreatedEvent"));
            }
            return;
        }

        InventoryItem item = maybe.get();
        if (item.getAvailableQty() < event.getQuantity()) {
            producer.publishFailed(new InventoryFailedEvent(
                    UUID.randomUUID().toString(),
                    event.getOrderId(), event.getProductId(),
                    "Insufficient stock (have=" + item.getAvailableQty() + ", need=" + event.getQuantity() + ")"));
            if (event.getEventId() != null) {
                processedEventRepository.save(new ProcessedEvent(event.getEventId(), "OrderCreatedEvent"));
            }
            return;
        }

        item.setAvailableQty(item.getAvailableQty() - event.getQuantity());
        inventoryRepository.save(item);
        log.info("Reserved {} of {}: remaining={}", event.getQuantity(), event.getProductId(), item.getAvailableQty());

        producer.publishReserved(new InventoryReservedEvent(
                UUID.randomUUID().toString(),
                event.getOrderId(),
                event.getProductId(),
                event.getQuantity(),
                event.getAmount()
        ));

        if (event.getEventId() != null) {
            processedEventRepository.save(new ProcessedEvent(event.getEventId(), "OrderCreatedEvent"));
        }
    }

    @Transactional
    public void release(PaymentFailedEvent event) {
        if (event.getEventId() != null && processedEventRepository.existsById(event.getEventId())) {
            log.warn("Duplicate event ignored: eventId={}", event.getEventId());
            return;
        }

        Optional<InventoryItem> maybe = inventoryRepository.findByProductIdForUpdate(event.getProductId());
        if (maybe.isEmpty()) {
            log.warn("Cannot release stock, product missing: {}", event.getProductId());
            return;
        }
        InventoryItem item = maybe.get();
        item.setAvailableQty(item.getAvailableQty() + event.getQuantity());
        inventoryRepository.save(item);
        log.info("Released {} of {} (orderId={}): available={}",
                event.getQuantity(), event.getProductId(), event.getOrderId(), item.getAvailableQty());

        if (event.getEventId() != null) {
            processedEventRepository.save(new ProcessedEvent(event.getEventId(), "PaymentFailedEvent"));
        }
    }
}
