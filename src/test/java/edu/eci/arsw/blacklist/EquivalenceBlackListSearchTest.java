package edu.eci.arsw.blacklist;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


public class EquivalenceBlackListSearchTest {
    private static final String TEST_IP = "202.24.34.55";
    private static final int ALARM_THRESHOLD = 5;
    private List<BlackListProvider> providersNoIo;
    private SearchResult reference;

    @BeforeEach
    void setUp() {
        providersNoIo = ProviderFactory.create(100, false);
        reference = new SequentialBlackListSearch(providersNoIo).search(TEST_IP, ALARM_THRESHOLD);
    }

    @Test
    void virtualThreadsShouldMatchSequentialReference() {
        BlackListSearch virtual = new VirtualThreadBlackListSearch(providersNoIo);
        SearchResult result = virtual.search(TEST_IP, ALARM_THRESHOLD);

        assertEquals(reference.matchingProviderIds(), result.matchingProviderIds());
        assertEquals(reference.consultedProviders(), result.consultedProviders());
    }

    @ParameterizedTest(name = "FixedPool(size={0}) debe igualar al secuencial")
    @ValueSource(ints = {2, 4, 8})
    void fixedPoolShouldMatchSequentialReferenceForEachPoolSize(int poolSize) {
        BlackListSearch fixedPool = new FixedPoolBlackListSearch(providersNoIo, poolSize);
        SearchResult result = fixedPool.search(TEST_IP, ALARM_THRESHOLD);

        assertEquals(reference.matchingProviderIds(), result.matchingProviderIds());
        assertEquals(reference.consultedProviders(), result.consultedProviders());
    }

    @Test
    void allStrategiesShouldAgreeAlsoWithSimulatedLatency() {
        List<BlackListProvider> providersWithIo = ProviderFactory.create(100, true);
        String ip = "10.0.0.1";

        SearchResult seqResult = new SequentialBlackListSearch(providersWithIo).search(ip, ALARM_THRESHOLD);
        SearchResult fixedResult = new FixedPoolBlackListSearch(providersWithIo, 4).search(ip, ALARM_THRESHOLD);
        SearchResult virtualResult = new VirtualThreadBlackListSearch(providersWithIo).search(ip, ALARM_THRESHOLD);

        assertEquals(seqResult.matchingProviderIds(), fixedResult.matchingProviderIds());
        assertEquals(seqResult.matchingProviderIds(), virtualResult.matchingProviderIds());
        assertEquals(seqResult.consultedProviders(), fixedResult.consultedProviders());
        assertEquals(seqResult.consultedProviders(), virtualResult.consultedProviders());
    }

    @Test
    void resultsShouldBeDeterministicAcrossRepeatedRunsOfTheSameStrategy() {
        BlackListSearch virtual = new VirtualThreadBlackListSearch(providersNoIo);

        SearchResult first = virtual.search(TEST_IP, ALARM_THRESHOLD);
        SearchResult second = virtual.search(TEST_IP, ALARM_THRESHOLD);

        assertEquals(first.matchingProviderIds(), second.matchingProviderIds());
    }

    @Test
    void concurrentStrategyShouldNeverProduceARaceConditionAcrossManyRuns() {
        BlackListSearch fixedPool = new FixedPoolBlackListSearch(providersNoIo, 8);

        boolean allConsistent = IntStream.range(0, 50)
                .mapToObj(i -> fixedPool.search(TEST_IP, ALARM_THRESHOLD))
                .allMatch(r -> r.matchingProviderIds().equals(reference.matchingProviderIds()));

        assertTrue(allConsistent, "Se detectó inconsistencia entre corridas concurrentes repetidas");
    }

    @Test
    void matchingProviderIdsShouldContainNoDuplicates() {
        BlackListSearch virtual = new VirtualThreadBlackListSearch(providersNoIo);
        SearchResult result = virtual.search(TEST_IP, ALARM_THRESHOLD);

        long distinctCount = result.matchingProviderIds().stream().distinct().count();
        assertEquals(result.matchingProviderIds().size(), distinctCount,
                "matchingProviderIds contiene identificadores duplicados");
    }

    @Test
    void matchingProviderIdsShouldBeInAscendingOrder() {
        BlackListSearch fixedPool = new FixedPoolBlackListSearch(providersNoIo, 8);
        SearchResult result = fixedPool.search(TEST_IP, ALARM_THRESHOLD);

        List<Integer> sorted = result.matchingProviderIds().stream().sorted().toList();
        assertEquals(sorted, result.matchingProviderIds(),
                "matchingProviderIds no está en orden ascendente");
    }

    @Test
    void fixedPoolShouldRejectNonPositivePoolSize() {
        assertThrows(IllegalArgumentException.class,
                () -> new FixedPoolBlackListSearch(providersNoIo, 0));
    }

    @Test
    void virtualThreadShouldRejectNonPositiveAlarmThreshold() {
        BlackListSearch virtual = new VirtualThreadBlackListSearch(providersNoIo);
        assertThrows(IllegalArgumentException.class,
                () -> virtual.search(TEST_IP, 0));
    }

    @Test
    void fixedPoolShouldRejectNullIpAddress() {
        BlackListSearch fixedPool = new FixedPoolBlackListSearch(providersNoIo, 4);
        assertThrows(NullPointerException.class,
                () -> fixedPool.search(null, ALARM_THRESHOLD));
    }
}