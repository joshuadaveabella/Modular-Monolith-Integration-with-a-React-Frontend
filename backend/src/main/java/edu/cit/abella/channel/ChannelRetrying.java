package edu.cit.abella.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

// Same shape as supplier's RetryingCaller (Lab 3). Kept as a small separate
// copy rather than shared, since the two packages' exception types differ
// and neither module should depend on the other's internals.
final class ChannelRetrying {

    private static final Logger log = LoggerFactory.getLogger(ChannelRetrying.class);
    private static final long[] BACKOFF_MS = {200, 500, 1000};

    private ChannelRetrying() {
    }

    static <T> T call(int maxAttempts, Supplier<T> action) {
        TiangeCallException lastFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (TiangeCallException e) {
                lastFailure = e;
                if (!e.isRetryable() || attempt == maxAttempts) {
                    throw e;
                }
                long delay = BACKOFF_MS[Math.min(attempt - 1, BACKOFF_MS.length - 1)];
                log.warn("Tiangge call failed ({}), attempt {}/{}, retrying in {} ms",
                        e.kind, attempt, maxAttempts, delay);
                sleep(delay);
            }
        }
        throw lastFailure;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while backing off before retry", e);
        }
    }
}
