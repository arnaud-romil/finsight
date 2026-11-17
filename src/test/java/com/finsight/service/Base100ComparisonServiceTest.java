package com.finsight.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.finsight.dto.Base100ComparisonResponse;
import com.finsight.model.Symbol;
import com.finsight.model.SymbolPrice;
import com.finsight.repository.SymbolPriceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Base100ComparisonServiceTest {

    @Mock
    private SymbolService symbolService;

    @Mock
    private SymbolPriceRepository symbolPriceRepository;

    @Test
    void normalizesEachInstrumentIndependentlyToBase100() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        Symbol gold = symbol(1L, "GOLD", "Gold");
        Symbol apple = symbol(2L, "AAPL", "Apple");

        when(symbolService.findByIds(List.of(1L, 2L))).thenReturn(List.of(gold, apple));
        when(symbolPriceRepository.findBySymbolIdsAndDateBetween(List.of(1L, 2L), from, to))
                .thenReturn(List.of(
                        new SymbolPrice(1L, "GOLD", LocalDate.of(2026, 1, 2), new BigDecimal("100")),
                        new SymbolPrice(1L, "GOLD", LocalDate.of(2026, 1, 3), new BigDecimal("110")),
                        new SymbolPrice(2L, "AAPL", LocalDate.of(2026, 1, 2), new BigDecimal("200")),
                        new SymbolPrice(2L, "AAPL", LocalDate.of(2026, 1, 3), new BigDecimal("180"))));

        Base100ComparisonResponse response = service().compare(List.of(1L, 2L), from, to);

        assertThat(response.series()).hasSize(2);
        assertThat(response.series().get(0).points().get(0).base100()).isEqualByComparingTo("100.000000");
        assertThat(response.series().get(0).points().get(1).base100()).isEqualByComparingTo("110.000000");
        assertThat(response.series().get(1).points().get(1).base100()).isEqualByComparingTo("90.000000");
    }

    @Test
    void ignoresUnknownAndUnpricedSymbols() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);

        when(symbolService.findByIds(List.of(1L, 2L))).thenReturn(List.of(symbol(1L, "GOLD", "Gold")));
        when(symbolPriceRepository.findBySymbolIdsAndDateBetween(List.of(1L, 2L), from, to)).thenReturn(List.of());

        Base100ComparisonResponse response = service().compare(List.of(1L, 2L), from, to);

        assertThat(response.series()).isEmpty();
        assertThat(response.ignoredSymbolIds()).containsExactly(1L, 2L);
    }

    @Test
    void rejectsInvalidDateRange() {
        LocalDate from = LocalDate.of(2026, 2, 1);
        LocalDate to = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(() -> service().compare(List.of(1L), from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("from date must be before or equal to to date");
    }

    @Test
    void deduplicatesRequestedSymbolIds() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);

        when(symbolService.findByIds(List.of(1L))).thenReturn(List.of(symbol(1L, "GOLD", "Gold")));
        when(symbolPriceRepository.findBySymbolIdsAndDateBetween(List.of(1L), from, to)).thenReturn(List.of());

        Base100ComparisonResponse response = service().compare(List.of(1L, 1L), from, to);

        assertThat(response.ignoredSymbolIds()).containsExactly(1L);
    }

    private Base100ComparisonService service() {
        return new Base100ComparisonService(symbolService, symbolPriceRepository);
    }

    private Symbol symbol(Long id, String code, String name) {
        return new Symbol(id, code, name, "France", "PA", "EUR", "ETF", "FR0000000001");
    }
}
