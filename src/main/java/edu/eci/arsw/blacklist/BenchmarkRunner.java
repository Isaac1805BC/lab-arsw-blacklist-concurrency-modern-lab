package edu.eci.arsw.blacklist;

import java.util.List;
import java.util.Locale;

public final class BenchmarkRunner {

    private static final int PROVIDER_COUNT = 100;
    private static final int ALARM_THRESHOLD = 5;

    private BenchmarkRunner() {
    }

    public static void main(String[] args) {
        BenchmarkConfig config = parseArgs(args);

        List<BlackListProvider> providers = ProviderFactory.create(PROVIDER_COUNT, config.simulateIo());
        BlackListSearch search = createStrategy(config.strategy(), providers, config.poolSize());

        printHeader(config);

        SearchResult referenceResult = search.search(config.ipAddress(), ALARM_THRESHOLD);

        System.out.println();
        System.out.println("Warm-up runs (excluded from measurements):");
        for (int i = 1; i <= config.warmups(); i++) {
            SearchResult warmupResult = search.search(config.ipAddress(), ALARM_THRESHOLD);
            validateEquivalence(referenceResult, warmupResult, "warm-up " + i);
            System.out.printf(Locale.US, "  warm-up %d elapsed: %.3f ms%n",
                    i, toMillis(warmupResult));
        }

        System.out.println();
        System.out.println("Measured runs:");
        double totalMs = 0.0;
        double minMs = Double.MAX_VALUE;
        double maxMs = Double.MIN_VALUE;

        String scenario = config.simulateIo() ? "IO" : "NOIO";
        String poolSizeLabel = config.strategy() == Strategy.FIXED
                ? String.valueOf(config.poolSize())
                : "-";

        System.out.println();
        System.out.println("scenario,strategy,pool_size,run,elapsed_ms,matches,consulted_providers");

        for (int run = 1; run <= config.measuredRuns(); run++) {
            SearchResult result = search.search(config.ipAddress(), ALARM_THRESHOLD);
            validateEquivalence(referenceResult, result, "measured run " + run);

            double elapsedMs = toMillis(result);
            totalMs += elapsedMs;
            minMs = Math.min(minMs, elapsedMs);
            maxMs = Math.max(maxMs, elapsedMs);

            System.out.printf(Locale.US, "%s,%s,%s,%d,%.3f,%d,%d%n",
                    scenario,
                    config.strategy(),
                    poolSizeLabel,
                    run,
                    elapsedMs,
                    result.matchingProviderIds().size(),
                    result.consultedProviders());
        }

        double avgMs = totalMs / config.measuredRuns();

        System.out.println();
        System.out.println("Summary:");
        System.out.printf(Locale.US, "  Average: %.3f ms%n", avgMs);
        System.out.printf(Locale.US, "  Minimum: %.3f ms%n", minMs);
        System.out.printf(Locale.US, "  Maximum: %.3f ms%n", maxMs);
        System.out.printf(Locale.US, "  Matches: %s%n", referenceResult.matchingProviderIds());
        System.out.printf(Locale.US, "  Consulted providers: %d%n", referenceResult.consultedProviders());
        System.out.printf(Locale.US, "  Trustworthy: %s%n", referenceResult.isTrustworthy(ALARM_THRESHOLD));
    }

    private static void printHeader(BenchmarkConfig config) {
        System.out.printf(Locale.US, "Strategy: %s%n", config.strategy());
        if (config.strategy() == Strategy.FIXED) {
            System.out.printf(Locale.US, "Pool size: %d%n", config.poolSize());
        }
        System.out.printf(Locale.US, "IP: %s%n", config.ipAddress());
        System.out.printf(Locale.US, "Simulate I/O: %s%n", config.simulateIo());
        System.out.printf(Locale.US, "Warm-ups: %d%n", config.warmups());
        System.out.printf(Locale.US, "Measured runs: %d%n", config.measuredRuns());
    }

    private static double toMillis(SearchResult result) {
        return result.elapsed().toNanos() / 1_000_000.0;
    }

    private static void validateEquivalence(SearchResult reference, SearchResult candidate, String label) {
        boolean sameMatches = reference.matchingProviderIds().equals(candidate.matchingProviderIds());
        boolean sameConsulted = reference.consultedProviders() == candidate.consultedProviders();
        if (!sameMatches || !sameConsulted) {
            throw new IllegalStateException(
                    "Equivalence check failed at " + label
                            + ". Expected matches=" + reference.matchingProviderIds()
                            + " consulted=" + reference.consultedProviders()
                            + " but got matches=" + candidate.matchingProviderIds()
                            + " consulted=" + candidate.consultedProviders());
        }
    }

    private static BlackListSearch createStrategy(Strategy strategy, List<BlackListProvider> providers, int poolSize) {
        return switch (strategy) {
            case SEQUENTIAL -> new SequentialBlackListSearch(providers);
            case FIXED -> new FixedPoolBlackListSearch(providers, poolSize);
            case VIRTUAL -> new VirtualThreadBlackListSearch(providers);
        };
    }

    private static BenchmarkConfig parseArgs(String[] args) {
        if (args.length < 5) {
            throw new IllegalArgumentException(
                    "Usage: <strategy> <ipAddress> <simulateIo> <warmups> <measuredRuns> [poolSize]"
                            + System.lineSeparator()
                            + "strategy must be one of SEQUENTIAL, FIXED, VIRTUAL"
                            + System.lineSeparator()
                            + "poolSize is required only when strategy is FIXED");
        }

        Strategy strategy;
        try {
            strategy = Strategy.valueOf(args[0].toUpperCase(Locale.US));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid strategy '" + args[0] + "'. Expected SEQUENTIAL, FIXED, or VIRTUAL.", ex);
        }

        String ipAddress = args[1];
        boolean simulateIo = Boolean.parseBoolean(args[2]);

        int warmups = parsePositiveOrZero(args[3], "warmups");
        int measuredRuns = parsePositive(args[4], "measuredRuns");

        int poolSize = 0;
        if (strategy == Strategy.FIXED) {
            if (args.length < 6) {
                throw new IllegalArgumentException("poolSize is required when strategy is FIXED");
            }
            poolSize = parsePositive(args[5], "poolSize");
        }

        return new BenchmarkConfig(strategy, ipAddress, simulateIo, warmups, measuredRuns, poolSize);
    }

    private static int parsePositive(String value, String fieldName) {
        int parsed = parseInt(value, fieldName);
        if (parsed <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
        return parsed;
    }

    private static int parsePositiveOrZero(String value, String fieldName) {
        int parsed = parseInt(value, fieldName);
        if (parsed < 0) {
            throw new IllegalArgumentException(fieldName + " must be zero or greater");
        }
        return parsed;
    }

    private static int parseInt(String value, String fieldName) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid integer: '" + value + "'", ex);
        }
    }

    private enum Strategy {
        SEQUENTIAL, FIXED, VIRTUAL
    }

    private record BenchmarkConfig(
            Strategy strategy,
            String ipAddress,
            boolean simulateIo,
            int warmups,
            int measuredRuns,
            int poolSize) {
    }
}