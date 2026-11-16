package com.finsight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finsight.client.EodhdClient;
import com.finsight.dto.EodhdDailyPriceResponse;
import com.finsight.model.DailyPrice;
import com.finsight.model.NormalizedPricePoint;
import com.finsight.model.Symbol;
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
}
