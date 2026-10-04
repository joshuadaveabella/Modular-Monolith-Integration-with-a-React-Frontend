package edu.cit.abella.channel;

import edu.cit.abella.events.InventoryStockChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
class StockPublishListener {

    private static final Logger log = LoggerFactory.getLogger(StockPublishListener.class);

    private final TiangeHttpClient httpClient;
    private final TiangeProperties properties;

    // Tracks the most recent event for each product. If this event's retry
    // is still in flight when a NEWER stock change for the same product
    // arrives, this one is stale and must not send - doing so would
    // overwrite Tiangge's correct, newer figure with an outdated one.
    private final ConcurrentHashMap<String, Long> latestSeqByProduct = new ConcurrentHashMap<>();
    private final AtomicLong seqGenerator = new AtomicLong();

    StockPublishListener(TiangeHttpClient httpClient, TiangeProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    @EventListener
    void onStockChanged(InventoryStockChangedEvent event) {
        long mySeq = seqGenerator.incrementAndGet();
        latestSeqByProduct.put(event.getProductId(), mySeq);

        try {
            ChannelRetrying.call(properties.maxAttempts, () -> {
                if (latestSeqByProduct.get(event.getProductId()) != mySeq) {
                    // A newer stock value for this product has already
                    // arrived - let that one win instead.
                    return null;
                }
                httpClient.publishStock(List.of(
                        new StockUpdateRequest(event.getProductId(), event.getAvailableStock())));
                return null;
            });
        } catch (Exception e) {
            log.warn("Could not publish stock update for {} to Tiangge: {}",
                    event.getProductId(), e.getMessage());
        }
    }
}