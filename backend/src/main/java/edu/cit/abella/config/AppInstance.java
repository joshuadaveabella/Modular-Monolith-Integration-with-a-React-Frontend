package edu.cit.abella.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

// "Each time your application starts, it creates a new random UUID... and
// sends it as X-Client-Instance on every call for as long as it runs. Send
// the same header on your LegacySupply calls too." This is why this class
// lives in the shared config package rather than inside channel: the
// supplier module's LegacySupplyHttpClient needs it too, and channel/
// supplier have no dependency on each other.
@Component
public class AppInstance {

    private static final Logger log = LoggerFactory.getLogger(AppInstance.class);

    private final String instanceId = UUID.randomUUID().toString();
    private final Instant startedAt = Instant.now();

    public AppInstance() {
        // "Proof: your self-check page shows the instance ID matches your
        // app's startup log" - this is that log line.
        log.info("Application instance started: {}", instanceId);
    }

    public String getInstanceId() {
        return instanceId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public long uptimeSeconds() {
        return Instant.now().getEpochSecond() - startedAt.getEpochSecond();
    }
}
