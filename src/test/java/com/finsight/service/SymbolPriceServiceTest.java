package com.finsight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finsight.client.EodhdClient;
import com.finsight.dto.EodhdDailyPriceResponse;
import com.finsight.model.DailyPrice;
import com.finsight.model.NormalizedPricePoint;
import com.finsight.model.Symbol;
import com.finsight.model.SymbolPrice;
import com.finsight.repository.SymbolPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SymbolPriceServiceTest {

    @Mock
    private EodhdClient eodhdClient;

    @Mock
    private SymbolPriceRepository symbolPriceRepository;

    @InjectMocks
    private SymbolPriceService symbolPriceService;

    @Test
    void getPriceHistoryMapsApiResponsesToDailyPrices() {
        Symbol symbol = new Symbol(1L, "GOLD", "Gold", "France", "PA", "EUR", "commodity", "FR0000000001");
        LocalDate from = LocalDate.of(2026, 10, 1);

        EodhdDailyPriceResponse response = new EodhdDailyPriceResponse(
                LocalDate.of(2026, 11, 2),
                new BigDecimal("138.11"),
                new BigDecimal("140.35"),
                new BigDecimal("137.49"),
                new BigDecimal("140.35"),
                new BigDecimal("140.35"),
                35499L);

        when(eodhdClient.getDailyPrices(symbol, from)).thenReturn(List.of(response));

        List<DailyPrice> prices = symbolPriceService.getPriceHistory(symbol, from);

        assertThat(prices).hasSize(1);
        assertThat(prices.getFirst().date()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(prices.getFirst().close()).isEqualByComparingTo("140.35");
        assertThat(prices.getFirst().adjustedClose()).isEqualByComparingTo("140.35");
        assertThat(prices.getFirst().volume()).isEqualTo(35499L);
        verify(eodhdClient).getDailyPrices(symbol, from);
    }

    @Test
    void getPriceHistoryReturnsEmptyListWhenApiReturnsNoData() {
        Symbol symbol = new Symbol(2L, "AAPL", "Apple", "United States", "NASDAQ", "USD", "stock", "US0378331005");
        LocalDate from = LocalDate.of(2026, 10, 1);

        when(eodhdClient.getDailyPrices(symbol, from)).thenReturn(List.of());

        List<DailyPrice> prices = symbolPriceService.getPriceHistory(symbol, from);

        assertThat(prices).isEmpty();
        verify(eodhdClient).getDailyPrices(symbol, from);
    }

    @Test
    void calculateNormalizedPerformanceUsesFirstValidPriceAsReference() {
        List<DailyPrice> prices = List.of(
                new DailyPrice(LocalDate.of(2026, 10, 1), new BigDecimal("100.00"), new BigDecimal("100.50"),
                        new BigDecimal("99.50"),
                        new BigDecimal("100.00"), new BigDecimal("100.00"), 1000L),
                new DailyPrice(LocalDate.of(2026, 10, 2), new BigDecimal("101.00"), new BigDecimal("101.50"),
                        new BigDecimal("100.00"),
                        new BigDecimal("101.25"), new BigDecimal("101.25"), 1100L),
                new DailyPrice(LocalDate.of(2026, 10, 3), new BigDecimal("100.00"), new BigDecimal("100.50"),
                        new BigDecimal("99.50"),
                        new BigDecimal("0.00"), new BigDecimal("0.00"), 1200L),
                new DailyPrice(LocalDate.of(2026, 10, 4), new BigDecimal("-5.00"), new BigDecimal("-4.00"),
                        new BigDecimal("-6.00"),
                        new BigDecimal("-5.00"), new BigDecimal("-5.00"), 1300L),
                new DailyPrice(LocalDate.of(2026, 10, 5), new BigDecimal("110.00"), new BigDecimal("111.00"),
                        new BigDecimal("109.00"),
                        new BigDecimal("110.00"), new BigDecimal("110.00"), 1400L));

        List<NormalizedPricePoint> normalized = symbolPriceService.calculateNormalizedPerformance(prices);

        assertThat(normalized).hasSize(3);
        assertThat(normalized.getFirst().date()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(normalized.getFirst().index()).isEqualByComparingTo(new BigDecimal("100.0000000000"));
        assertThat(normalized.get(1).index()).isEqualByComparingTo(new BigDecimal("101.2500000000"));
        assertThat(normalized.get(2).index()).isEqualByComparingTo(new BigDecimal("110.0000000000"));
        assertThat(normalized.getFirst().referenceDate()).isEqualTo(LocalDate.of(2026, 10, 1));
    }

    @Test
    void normalizePerformanceAcrossMultipleInstrumentsUsesIndependentBase100() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 5);

        when(symbolPriceRepository.findBySymbolIdsAndDateBetween(List.of(1L, 2L), start, end)).thenReturn(List.of(
                new SymbolPrice(1L, "GOLD", LocalDate.of(2026, 10, 1), new BigDecimal("100.00")),
                new SymbolPrice(1L, "GOLD", LocalDate.of(2026, 10, 2), new BigDecimal("105.00")),
                new SymbolPrice(1L, "GOLD", LocalDate.of(2026, 10, 3), new BigDecimal("110.00")),
                new SymbolPrice(2L, "AAPL", LocalDate.of(2026, 10, 1), new BigDecimal("200.00")),
                new SymbolPrice(2L, "AAPL", LocalDate.of(2026, 10, 2), new BigDecimal("210.00")),
                new SymbolPrice(2L, "AAPL", LocalDate.of(2026, 10, 3), new BigDecimal("220.00"))));

        var normalized = symbolPriceService.normalizePerformance(List.of(1L, 2L), start, end);

        assertThat(normalized).hasSize(2);
        assertThat(normalized.get(0).symbolCode()).isEqualTo("GOLD");
        assertThat(normalized.get(0).points()).extracting(NormalizedPricePoint::index)
                .containsExactly(
                        new BigDecimal("100.0000000000"),
                        new BigDecimal("105.0000000000"),
                        new BigDecimal("110.0000000000"));

        assertThat(normalized.get(1).symbolCode()).isEqualTo("AAPL");
        assertThat(normalized.get(1).points()).extracting(NormalizedPricePoint::index)
                .containsExactly(
                        new BigDecimal("100.0000000000"),
                        new BigDecimal("105.0000000000"),
                        new BigDecimal("110.0000000000"));

        assertThat(normalized.get(0).referenceDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(normalized.get(1).referenceDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        verify(symbolPriceRepository).findBySymbolIdsAndDateBetween(List.of(1L, 2L), start, end);
    }

    @Test
    void calculateNormalizedPerformanceUsesIndependentReferenceDatePerInstrument() {
        List<DailyPrice> firstSeries = List.of(
                new DailyPrice(LocalDate.of(2026, 10, 01), new BigDecimal("100.00"), new BigDecimal("100.50"),
                        new BigDecimal("99.50"), new BigDecimal("100.00"), new BigDecimal("100.00"), 1000L),
                new DailyPrice(LocalDate.of(2026, 10, 02), new BigDecimal("101.00"), new BigDecimal("101.50"),
                        new BigDecimal("100.00"), new BigDecimal("101.25"), new BigDecimal("101.25"), 1100L));

        List<DailyPrice> secondSeries = List.of(
                new DailyPrice(LocalDate.of(2026, 10, 05), new BigDecimal("200.00"), new BigDecimal("205.00"),
                        new BigDecimal("199.00"), new BigDecimal("200.00"), new BigDecimal("200.00"), 1500L),
                new DailyPrice(LocalDate.of(2026, 10, 06), new BigDecimal("210.00"), new BigDecimal("215.00"),
                        new BigDecimal("209.00"), new BigDecimal("210.00"), new BigDecimal("210.00"), 1600L));

        List<NormalizedPricePoint> firstNormalized = symbolPriceService.calculateNormalizedPerformance(firstSeries);
        List<NormalizedPricePoint> secondNormalized = symbolPriceService.calculateNormalizedPerformance(secondSeries);

        assertThat(firstNormalized.getFirst().referenceDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(secondNormalized.getFirst().referenceDate()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(firstNormalized.getLast().index()).isEqualByComparingTo(new BigDecimal("101.2500000000"));
        assertThat(secondNormalized.getLast().index()).isEqualByComparingTo(new BigDecimal("105.0000000000"));
    }

    @Test
    void calculateNormalizedPerformanceStartsFromFirstValidDateWhenPeriodStartsEarlier() {
        List<DailyPrice> prices = List.of(
                new DailyPrice(LocalDate.of(2026, 10, 01), new BigDecimal("0.00"), new BigDecimal("0.10"),
                        new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), 500L),
                new DailyPrice(LocalDate.of(2026, 10, 03), new BigDecimal("100.00"), new BigDecimal("101.00"),
                        new BigDecimal("99.00"), new BigDecimal("100.00"), new BigDecimal("100.00"), 600L),
                new DailyPrice(LocalDate.of(2026, 10, 04), new BigDecimal("104.00"), new BigDecimal("105.00"),
                        new BigDecimal("103.00"), new BigDecimal("104.00"), new BigDecimal("104.00"), 700L));

        List<NormalizedPricePoint> normalized = symbolPriceService.calculateNormalizedPerformance(prices);

        assertThat(normalized).hasSize(2);
        assertThat(normalized.getFirst().date()).isEqualTo(LocalDate.of(2026, 10, 3));
        assertThat(normalized.getFirst().referenceDate()).isEqualTo(LocalDate.of(2026, 10, 3));
        assertThat(normalized.getFirst().index()).isEqualByComparingTo(new BigDecimal("100.0000000000"));
        assertThat(normalized.getLast().index()).isEqualByComparingTo(new BigDecimal("104.0000000000"));
    }

    @Test
    void calculateNormalizedPerformanceDoesNotInterpolateMissingDates() {
        List<DailyPrice> prices = List.of(
                new DailyPrice(LocalDate.of(2026, 10, 01), new BigDecimal("100.00"), new BigDecimal("100.50"),
                        new BigDecimal("99.50"), new BigDecimal("100.00"), new BigDecimal("100.00"), 1000L),
                new DailyPrice(LocalDate.of(2026, 10, 03), new BigDecimal("200.00"), new BigDecimal("201.00"),
                        new BigDecimal("199.00"), new BigDecimal("200.00"), new BigDecimal("200.00"), 1200L),
                new DailyPrice(LocalDate.of(2026, 10, 04), new BigDecimal("210.00"), new BigDecimal("211.00"),
                        new BigDecimal("209.00"), new BigDecimal("210.00"), new BigDecimal("210.00"), 1300L));

        List<NormalizedPricePoint> normalized = symbolPriceService.calculateNormalizedPerformance(prices);

        assertThat(normalized).hasSize(3);
        assertThat(normalized).extracting(NormalizedPricePoint::date)
                .containsExactly(
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 3),
                        LocalDate.of(2026, 10, 4));
        assertThat(normalized.get(1).index()).isEqualByComparingTo(new BigDecimal("200.0000000000"));
        assertThat(normalized.get(2).index()).isEqualByComparingTo(new BigDecimal("210.0000000000"));
    }

    @Test
    void calculateNormalizedPerformanceReturnsEmptyWhenNoValidPriceExists() {
        List<DailyPrice> prices = List.of(
                new DailyPrice(LocalDate.of(2026, 10, 01), new BigDecimal("0.00"), new BigDecimal("0.10"),
                        new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), 100L),
                new DailyPrice(LocalDate.of(2026, 10, 02), new BigDecimal("-1.00"), new BigDecimal("-0.50"),
                        new BigDecimal("-1.50"), new BigDecimal("-1.00"), new BigDecimal("-1.00"), 200L),
                new DailyPrice(LocalDate.of(2026, 10, 03), null, null, null, null, null, 300L));

        assertThat(symbolPriceService.calculateNormalizedPerformance(prices)).isEmpty();
    }
}
