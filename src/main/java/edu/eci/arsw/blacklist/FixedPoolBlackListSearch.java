package edu.eci.arsw.blacklist;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Concurrent implementation backed by a fixed-size platform-thread pool.
 * Every provider is consulted through its own task; results are consolidated
 * after all futures complete, so no shared mutable state is touched from
 * more than one thread.
 */
public final class FixedPoolBlackListSearch implements BlackListSearch {
    private final List<BlackListProvider> providers;
    private final int poolSize;

    public FixedPoolBlackListSearch(List<BlackListProvider> providers, int poolSize) {
        this.providers = List.copyOf(Objects.requireNonNull(providers, "providers"));
        if (poolSize <= 0) {
            throw new IllegalArgumentException("poolSize must be greater than zero");
        }
        this.poolSize = poolSize;
    }

    @Override
    public SearchResult search(String ipAddress, int alarmThreshold) {
        Objects.requireNonNull(ipAddress, "ipAddress");
        if (alarmThreshold <= 0) {
            throw new IllegalArgumentException("alarmThreshold must be greater than zero");
        }

        long startedAt = System.nanoTime();
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        try {
            List<Future<Integer>> futures = new ArrayList<>(providers.size());
            for (BlackListProvider provider : providers) {
                futures.add(executor.submit(matchTask(provider, ipAddress)));
            }

            List<Integer> matches = new ArrayList<>();
            for (Future<Integer> future : futures) {
                Integer matchedId = awaitResult(future);
                if (matchedId != null) {
                    matches.add(matchedId);
                }
            }

            Collections.sort(matches);
            Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);
            return new SearchResult(ipAddress, matches, providers.size(), elapsed);
        } finally {
            executor.shutdown();
        }
    }

    private static Callable<Integer> matchTask(BlackListProvider provider, String ipAddress) {
        return () -> provider.isBlacklisted(ipAddress) ? provider.id() : null;
    }

    private static Integer awaitResult(Future<Integer> future) {
        try {
            return future.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Blacklist search was interrupted", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException("Blacklist provider consultation failed", ex.getCause());
        }
    }
}
