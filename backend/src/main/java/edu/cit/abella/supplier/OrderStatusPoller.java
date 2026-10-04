package edu.cit.abella.supplier;

import edu.cit.abella.events.SupplierOrderDeliveredEvent;
import edu.cit.abella.supplier.LegacySupplyHttpClient.PoOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
class OrderStatusPoller {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusPoller.class);

    private static final List<SupplierOrderStatus> OPEN_STATUSES = List.of(
            SupplierOrderStatus.SUBMITTED,
            SupplierOrderStatus.IN_PROGRESS,
            SupplierOrderStatus.SHIPPED,
            SupplierOrderStatus.UNKNOWN
    );

    private final SupplierOrderRepository orderRepository;
    private final LegacySupplyHttpClient httpClient;
    private final ApplicationEventPublisher eventPublisher;

    OrderStatusPoller(SupplierOrderRepository orderRepository,
                      LegacySupplyHttpClient httpClient,
                      ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.httpClient = httpClient;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelayString = "${legacysupply.status-poll-interval-ms:45000}")
    @Transactional
    void pollOpenOrders() {
        List<SupplierOrderEntity> open = orderRepository.findAllByStatusIn(OPEN_STATUSES).stream()
                .filter(o -> o.getPoNumber() != null)
                .toList();
        if (open.isEmpty()) return;

        log.info("Polling status for {} open supplier order(s)", open.size());

        for (SupplierOrderEntity order : open) {
            try {
                PoOutcome outcome = RetryingCaller.call(2, () -> httpClient.getStatus(order.getPoNumber()));
                SupplierOrderStatus previous = order.getStatus();
                SupplierOrderStatus updated = SupplierOrderServiceImpl.mapStatusCode(outcome.statusCode());

                order.setStatus(updated);
                orderRepository.save(order);

                if (updated == SupplierOrderStatus.DELIVERED && previous != SupplierOrderStatus.DELIVERED) {
                    eventPublisher.publishEvent(new SupplierOrderDeliveredEvent(
                            order.getId(), order.getProductId(), order.getUnits(), order.getPoNumber()));
                }

                if (updated == SupplierOrderStatus.UNKNOWN) {
                    log.warn("Supplier order {} has an unrecognized status (raw code {}), will keep polling",
                            order.getBuyerRef(), outcome.statusCode());
                }
            } catch (LsCallException e) {
                log.warn("Status check failed for {}: {} {}", order.getBuyerRef(), e.kind, e.getMessage());
            }
        }
    }
}
