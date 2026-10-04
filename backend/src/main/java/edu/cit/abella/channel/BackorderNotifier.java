package edu.cit.abella.channel;

import edu.cit.abella.events.BackorderResolvedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class BackorderNotifier {

    private static final Logger log = LoggerFactory.getLogger(BackorderNotifier.class);

    private final ChannelOrderMappingRepository orderMappingRepository;
    private final TiangeHttpClient httpClient;
    private final TiangeProperties properties;

    BackorderNotifier(ChannelOrderMappingRepository orderMappingRepository,
                      TiangeHttpClient httpClient, TiangeProperties properties) {
        this.orderMappingRepository = orderMappingRepository;
        this.httpClient = httpClient;
        this.properties = properties;
    }

    @EventListener
    void onBackorderResolved(BackorderResolvedEvent event) {
        Optional<ChannelOrderMappingEntity> mapping = orderMappingRepository.findByShopOrderId(event.getOrderId());
        if (mapping.isEmpty()) {
            return; // this order didn't originate from Tiangge - nothing to tell it
        }

        String status = "CONFIRMED".equals(event.getResolvedStatus()) ? "ACCEPTED" : "CANCELLED";

        try {
            ChannelRetrying.call(properties.maxAttempts, () -> {
                httpClient.resolve(mapping.get().getTiangeOrderId(), status);
                return null;
            });
        } catch (Exception e) {
            // Known gap given the lab's time limit: unlike reorders (Lab 3,
            // backed by a durable PENDING queue), a failed resolution
            // notification here is only logged, not retried on a later
            // schedule. See README for the reasoning and what a fuller
            // implementation would add.
            log.warn("Could not notify Tiangge of backorder resolution for order {}: {}",
                    event.getOrderId(), e.getMessage());
        }
    }
}
