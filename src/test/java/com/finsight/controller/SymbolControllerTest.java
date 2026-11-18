package com.finsight.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.finsight.model.Symbol;
import com.finsight.service.SymbolService;
import java.util.List;
import org.junit.jupiter.api.Test;

class SymbolControllerTest {

    @Test
    void returnsDashboardSymbols() {
        SymbolService symbolService = mock(SymbolService.class);
        when(symbolService.findAll()).thenReturn(List.of(
                new Symbol(1L, "GOLD", "Gold", "France", "PA", "USD", "ETF", "FR0001")));

        SymbolController controller = new SymbolController(symbolService);

        assertThat(controller.findAll()).singleElement().satisfies(symbol -> {
            assertThat(symbol.id()).isEqualTo(1L);
            assertThat(symbol.code()).isEqualTo("GOLD");
            assertThat(symbol.name()).isEqualTo("Gold");
            assertThat(symbol.exchange()).isEqualTo("PA");
            assertThat(symbol.currency()).isEqualTo("USD");
        });
    }
}
