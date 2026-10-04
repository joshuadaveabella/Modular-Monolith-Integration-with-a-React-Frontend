package edu.cit.abella.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// initialDelay equal to the heartbeat interval means this job's first
// automatic firing happens one full interval after startup - well after
// TiangeStartup's explicit first heartbeat, so the two can never race.
@Component
class HeartbeatScheduler {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatScheduler.class);

    private final TiangeHttpClient httpClient;
    private final TiangeProperties properties;

    HeartbeatScheduler(TiangeHttpClient httpClient, TiangeProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    @Scheduled(
            initialDelayString = "${tiangge.heartbeat-interval-ms:30000}",
            fixedDelayString = "${tiangge.heartbeat-interval-ms:30000}")
    void sendHeartbeat() {
        try {
            ChannelRetrying.call(properties.maxAttempts, () -> {
                httpClient.heartbeat();
                return null;
            });
        } catch (Exception e) {
            // A single missed heartbeat should not stop the scheduler -
            // the next one fires in heartbeatIntervalMs regardless.
            log.warn("Heartbeat failed: {}", e.getMessage());
        }
    }
}
