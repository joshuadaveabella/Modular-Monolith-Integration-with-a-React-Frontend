package edu.cit.abella.supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

final class RetryingCaller {

    private static final Logger log = LoggerFactory.getLogger(RetryingCaller.class);
    private static final long[] BACKOFF_MS = {200, 500, 1000};

    private RetryingCaller() {
    }

    static <T> T call(int maxAttempts, Supplier<T> action) {
        LsCallException lastFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (LsCallException e) {
                lastFailure = e;
                if (!e.isRetryable() || attempt == maxAttempts) {
                    throw e;
                }
                long delay = BACKOFF_MS[Math.min(attempt - 1, BACKOFF_MS.length - 1)];
                log.warn("LegacySupply call failed ({}), attempt {}/{}, retrying in {} ms",
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
