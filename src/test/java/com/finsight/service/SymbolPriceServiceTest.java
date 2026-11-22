package com.finsight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finsight.client.EodhdClient;
import com.finsight.dto.EodhdDailyPriceResponse;
import com.finsight.model.DailyPrice;
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
        LocalDate to = LocalDate.of(2026, 11, 15);

        EodhdDailyPriceResponse response = new EodhdDailyPriceResponse(
                LocalDate.of(2026, 11, 2),
                new BigDecimal("138.11"),
                new BigDecimal("140.35"),
                new BigDecimal("137.49"),
                new BigDecimal("140.35"),
                new BigDecimal("140.35"),
                35499L);

        when(eodhdClient.getDailyPrices(symbol, from, to)).thenReturn(List.of(response));

        List<DailyPrice> prices = symbolPriceService.getPriceHistory(symbol, from, to);

        assertThat(prices).hasSize(1);
        assertThat(prices.getFirst().date()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(prices.getFirst().close()).isEqualByComparingTo("140.35");
        assertThat(prices.getFirst().adjustedClose()).isEqualByComparingTo("140.35");
        assertThat(prices.getFirst().volume()).isEqualTo(35499L);
        verify(eodhdClient).getDailyPrices(symbol, from, to);
    }

    @Test
    void getPriceHistoryReturnsEmptyListWhenApiReturnsNoData() {
        Symbol symbol = new Symbol(2L, "AAPL", "Apple", "United States", "NASDAQ", "USD", "stock", "US0378331005");
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 11, 15);

        when(eodhdClient.getDailyPrices(symbol, from, to)).thenReturn(List.of());

        List<DailyPrice> prices = symbolPriceService.getPriceHistory(symbol, from, to);

        assertThat(prices).isEmpty();
        verify(eodhdClient).getDailyPrices(symbol, from, to);
    }
}
