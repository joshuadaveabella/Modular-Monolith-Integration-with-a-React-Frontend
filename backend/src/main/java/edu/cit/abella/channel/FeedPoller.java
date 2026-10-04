package edu.cit.abella.channel;

import edu.cit.abella.shop.OrderAlreadyCancelledException;
import edu.cit.abella.shop.OrderRequest;
import edu.cit.abella.shop.OrderResponse;
import edu.cit.abella.shop.OrderService;
import edu.cit.abella.shop.OrderSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Task 4/5: reads the feed every few seconds, creates/decides orders and
// confirms cancellations. Every event is handled through the SAME
// OrderService used by the React UI - this class never reserves stock or
// touches an order row directly.
//
// Idempotency has two independent layers, deliberately overlapping:
//   1. ChannelProcessedEventEntity - skip an eventId we've already fully
//      handled (our side effect done AND Tiangge successfully told).
//   2. ChannelOrderMappingEntity - before creating a new order for an
//      ORDER_PLACED event, check whether this tiangeOrderId already maps
//      to one. This is what guarantees "exactly one order per Tiangge
//      order" even if layer 1 were somehow bypassed.
//
// The cursor only advances past an event once it is FULLY handled. If
// anything fails partway (our order creation succeeds but telling Tiangge
// the decision doesn't), the cursor stays put, the event is re-fetched
// next poll, and layer 2 above prevents a duplicate order on that retry.
@Component
class FeedPoller {

    private static final Logger log = LoggerFactory.getLogger(FeedPoller.class);

    private final ChannelFeedCursorRepository cursorRepository;
    private final ChannelProcessedEventRepository processedEventRepository;
    private final ChannelOrderMappingRepository orderMappingRepository;
    private final TiangeHttpClient httpClient;
    private final TiangeProperties properties;
    private final OrderService orderService;

    FeedPoller(ChannelFeedCursorRepository cursorRepository,
              ChannelProcessedEventRepository processedEventRepository,
              ChannelOrderMappingRepository orderMappingRepository,
              TiangeHttpClient httpClient,
              TiangeProperties properties,
              OrderService orderService) {
        this.cursorRepository = cursorRepository;
        this.processedEventRepository = processedEventRepository;
        this.orderMappingRepository = orderMappingRepository;
        this.httpClient = httpClient;
        this.properties = properties;
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${tiangge.feed-poll-interval-ms:5000}")
    void poll() {
        Long cursor = currentCursor();

        FeedResponse response;
        try {
            response = ChannelRetrying.call(properties.maxAttempts,
                    () -> httpClient.getFeed(cursor, properties.feedPageLimit));
        } catch (TiangeCallException e) {
            log.warn("Feed poll failed: {} {}", e.kind, e.getMessage());
            return; // cursor untouched - retried on the next scheduled run
        }

        if (response == null || response.events == null || response.events.isEmpty()) {
            return;
        }

        for (FeedEvent event : response.events) {
            boolean handled = handleEventSafely(event);
            if (!handled) {
                // Stop processing this batch here. Everything after this
                // event in the page will be re-fetched next poll since the
                // cursor hasn't moved past it.
                return;
            }
            advanceCursor(event.seq);
        }
    }

    private boolean handleEventSafely(FeedEvent event) {
        if (event.eventId != null && processedEventRepository.existsByEventId(event.eventId)) {
            return true; // already fully handled - just advance past it
        }

        try {
            if ("ORDER_PLACED".equals(event.type)) {
                handleOrderPlaced(event);
            } else if ("ORDER_CANCELLED".equals(event.type)) {
                handleOrderCancelled(event);
            } else {
                log.warn("Unrecognized feed event type '{}' (eventId {}) - marking seen and moving on",
                        event.type, event.eventId);
            }
            markProcessed(event);
            return true;
        } catch (Exception e) {
            log.error("Failed to fully process event {} ({}): {}", event.eventId, event.type, e.getMessage(), e);
            return false;
        }
    }

    private void handleOrderPlaced(FeedEvent event) {
        Optional<ChannelOrderMappingEntity> existing = orderMappingRepository.findById(event.orderId);

        if (existing.isPresent()) {
            // We created the order on an earlier attempt but didn't finish
            // telling Tiangge about it. Re-derive the decision from our own
            // order's current status rather than deciding again.
            ChannelOrderMappingEntity mapping = existing.get();
            if (!mapping.isDecisionConfirmed()) {
                OrderSummary order = orderService.getOrderSummary(mapping.getShopOrderId());
                sendDecisionAndMark(mapping, toDecision(order.getStatus()), order.getReason());
            }
            return;
        }

        List<OrderRequest.LineItem> items = new ArrayList<>();
        if (event.lines != null) {
            for (FeedLine line : event.lines) {
                OrderRequest.LineItem item = new OrderRequest.LineItem();
                item.setProductId(line.sellerSku); // sellerSku IS our own productId
                item.setQuantity(line.qty);
                items.add(item);
            }
        }

        // allowBackorder=true is the only thing distinguishing a Tiangge
        // order from a React order at this call site.
        OrderResponse response = orderService.placeOrder(items, true);

        ChannelOrderMappingEntity mapping = new ChannelOrderMappingEntity(event.orderId, response.getOrderId());
        orderMappingRepository.save(mapping);

        sendDecisionAndMark(mapping, toDecision(response.getStatus()), response.getReason());
    }

    private void sendDecisionAndMark(ChannelOrderMappingEntity mapping, String decision, String reason) {
        ChannelRetrying.call(properties.maxAttempts, () -> {
            httpClient.decide(mapping.getTiangeOrderId(), decision, String.valueOf(mapping.getShopOrderId()), reason);
            return null;
        });
        mapping.setDecisionConfirmed(true);
        orderMappingRepository.save(mapping);
    }

    private String toDecision(String orderStatus) {
        return switch (orderStatus) {
            case "CONFIRMED" -> "ACCEPTED";
            case "BACKORDERED" -> "BACKORDERED";
            default -> "REJECTED";
        };
    }

    private void handleOrderCancelled(FeedEvent event) {
        ChannelOrderMappingEntity mapping = orderMappingRepository.findById(event.orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "No local order mapped for Tiangge order " + event.orderId));

        if (mapping.isCancellationConfirmed()) {
            return;
        }

        try {
            orderService.cancelOrder(mapping.getShopOrderId());
        } catch (OrderAlreadyCancelledException alreadyDone) {
            // Idempotent - someone (or a previous attempt) already cancelled
            // it. Proceed to confirm with Tiangge regardless.
        }
        // Any other exception (e.g. order still BACKORDERED, not yet
        // CONFIRMED) is allowed to propagate - this event is retried on the
        // next poll rather than silently dropped.

        ChannelRetrying.call(properties.maxAttempts, () -> {
            httpClient.confirmCancellation(mapping.getTiangeOrderId(), true);
            return null;
        });
        mapping.setCancellationConfirmed(true);
        orderMappingRepository.save(mapping);
    }

    private void markProcessed(FeedEvent event) {
        if (event.eventId == null) {
            return;
        }
        processedEventRepository.save(new ChannelProcessedEventEntity(event.eventId, event.seq, event.type));
    }

    private Long currentCursor() {
        return cursorRepository.findById(1L).map(ChannelFeedCursorEntity::getLastSeq).orElse(null);
    }

    private void advanceCursor(Long seq) {
        if (seq == null) {
            return;
        }
        ChannelFeedCursorEntity cursor = cursorRepository.findById(1L).orElseGet(() -> new ChannelFeedCursorEntity(1L));
        cursor.setLastSeq(seq);
        cursorRepository.save(cursor);
    }
}
