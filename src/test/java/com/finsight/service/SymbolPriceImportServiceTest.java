package com.finsight.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finsight.model.DailyPrice;
import com.finsight.model.Symbol;
import com.finsight.repository.SymbolPriceRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class SymbolPriceImportServiceTest {

    @Mock
    private SymbolService symbolService;

    @Mock
    private SymbolPriceService symbolPriceService;

    @Mock
    private SymbolPriceRepository symbolPriceRepository;

    private Clock clock;

    private SymbolPriceImportService service;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneId.of("UTC"));
        service = new SymbolPriceImportService(
                symbolService,
                symbolPriceService,
                symbolPriceRepository,
                clock,
                "2000-01-01");
        when(symbolPriceRepository.findLatestDateBySymbolId(org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(Optional.empty());
    }

    @Test
    void importDailyPricesImportsAllPricesForEachSymbol() {
        Symbol gold = new Symbol(1L, "GOLD", "Gold", "France", "PA", "EUR", "commodity", "FR0000000001");
        Symbol apple = new Symbol(2L, "AAPL", "Apple", "United States", "NASDAQ", "USD", "stock", "US0378331005");
        LocalDate initialDate = LocalDate.of(2000, 1, 1);
        LocalDate to = LocalDate.of(2026, 11, 15);

        DailyPrice goldPrice1 = new DailyPrice(LocalDate.of(2026, 11, 15), new BigDecimal("138.11"),
                new BigDecimal("140.35"), new BigDecimal("137.49"), new BigDecimal("140.35"), new BigDecimal("140.35"),
                35499L);
        DailyPrice goldPrice2 = new DailyPrice(LocalDate.of(2026, 11, 16), new BigDecimal("139.00"),
                new BigDecimal("141.00"), new BigDecimal("138.80"), new BigDecimal("140.10"), new BigDecimal("140.10"),
                40000L);
        DailyPrice applePrice = new DailyPrice(LocalDate.of(2026, 11, 15), new BigDecimal("201.10"),
                new BigDecimal("203.50"), new BigDecimal("200.80"), new BigDecimal("202.00"), new BigDecimal("202.00"),
                120000L);

        when(symbolService.findAll()).thenReturn(List.of(gold, apple));
        when(symbolPriceService.getPriceHistory(gold, initialDate, to)).thenReturn(List.of(goldPrice1, goldPrice2));
        when(symbolPriceService.getPriceHistory(apple, initialDate, to)).thenReturn(List.of(applePrice));

        service.importDailyPrices();

        verify(symbolPriceService).getPriceHistory(gold, initialDate, to);
        verify(symbolPriceService).getPriceHistory(apple, initialDate, to);
        verify(symbolPriceRepository).save(gold.id(), goldPrice1);
        verify(symbolPriceRepository).save(gold.id(), goldPrice2);
        verify(symbolPriceRepository).save(apple.id(), applePrice);
    }

    @Test
    void importDailyPricesSkipsSymbolsWithoutPrices() {
        Symbol symbol = new Symbol(10L, "MSFT", "Microsoft", "United States", "NASDAQ", "USD", "stock", "US5949181045");
        LocalDate initialDate = LocalDate.of(2000, 1, 1);
        LocalDate to = LocalDate.of(2026, 11, 15);

        when(symbolService.findAll()).thenReturn(List.of(symbol));
        when(symbolPriceService.getPriceHistory(symbol, initialDate, to)).thenReturn(List.of());

        service.importDailyPrices();

        verify(symbolPriceService).getPriceHistory(symbol, initialDate, to);
        verify(symbolPriceRepository, never()).save(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void importDailyPricesContinuesAfterDuplicateKey() {
        Symbol symbol = new Symbol(3L, "GLD", "Gold ETF", "United States", "NYSEARCA", "USD", "etf", "US38259P5089");
        LocalDate initialDate = LocalDate.of(2000, 1, 1);
        LocalDate to = LocalDate.of(2026, 11, 15);

        DailyPrice first = new DailyPrice(LocalDate.of(2026, 11, 15), new BigDecimal("180.00"),
                new BigDecimal("181.00"), new BigDecimal("179.00"), new BigDecimal("180.50"), new BigDecimal("180.50"),
                1000L);
        DailyPrice second = new DailyPrice(LocalDate.of(2026, 11, 16), new BigDecimal("181.00"),
                new BigDecimal("182.00"), new BigDecimal("180.50"), new BigDecimal("181.50"), new BigDecimal("181.50"),
                2000L);

        when(symbolService.findAll()).thenReturn(List.of(symbol));
        when(symbolPriceService.getPriceHistory(symbol, initialDate, to)).thenReturn(List.of(first, second));
        doThrow(new DuplicateKeyException("dup")).when(symbolPriceRepository).save(symbol.id(), first);

        assertThatCode(() -> service.importDailyPrices()).doesNotThrowAnyException();

        verify(symbolPriceRepository).save(symbol.id(), first);
        verify(symbolPriceRepository).save(symbol.id(), second);
    }

    @Test
    void importDailyPricesContinuesAfterApiFailureForOneSymbol() {
        Symbol okSymbol = new Symbol(4L, "NVDA", "NVIDIA", "United States", "NASDAQ", "USD", "stock", "US67066G1040");
        Symbol failingSymbol = new Symbol(5L, "META", "Meta", "United States", "NASDAQ", "USD", "stock",
                "US30303M1027");
        LocalDate initialDate = LocalDate.of(2000, 1, 1);
        LocalDate to = LocalDate.of(2026, 11, 15);

        DailyPrice price = new DailyPrice(LocalDate.of(2026, 11, 15), new BigDecimal("200.00"),
                new BigDecimal("205.00"), new BigDecimal("198.00"), new BigDecimal("201.50"), new BigDecimal("201.50"),
                123L);

        when(symbolService.findAll()).thenReturn(List.of(okSymbol, failingSymbol));
        when(symbolPriceService.getPriceHistory(okSymbol, initialDate, to)).thenReturn(List.of(price));
        when(symbolPriceService.getPriceHistory(failingSymbol, initialDate, to))
                .thenThrow(new RuntimeException("api failure"));

        assertThatCode(() -> service.importDailyPrices()).doesNotThrowAnyException();

        verify(symbolPriceRepository).save(okSymbol.id(), price);
    }

    @Test
    void importDailyPricesUsesConfiguredInitialDateAndClockDate() {
        Symbol symbol = new Symbol(6L, "IBM", "IBM", "United States", "NYSE", "USD", "stock", "US4592001014");
        LocalDate initialDate = LocalDate.of(2000, 1, 1);
        LocalDate to = LocalDate.of(2026, 11, 15);

        when(symbolService.findAll()).thenReturn(List.of(symbol));
        when(symbolPriceService.getPriceHistory(symbol, initialDate, to)).thenReturn(List.of());

        service.importDailyPrices();

        verify(symbolPriceService).getPriceHistory(symbol, initialDate, to);
    }

    @Test
    void importDailyPricesStartsAfterLatestStoredDate() {
        Symbol symbol = new Symbol(7L, "MSFT", "Microsoft", "United States", "NASDAQ", "USD", "stock", "US5949181045");

        when(symbolService.findAll()).thenReturn(List.of(symbol));
        when(symbolPriceRepository.findLatestDateBySymbolId(symbol.id()))
                .thenReturn(Optional.of(LocalDate.of(2026, 11, 10)));
        when(symbolPriceService.getPriceHistory(
                symbol,
                LocalDate.of(2026, 11, 11),
                LocalDate.of(2026, 11, 15)))
                .thenReturn(List.of());

        service.importDailyPrices();

        verify(symbolPriceService).getPriceHistory(
                symbol,
                LocalDate.of(2026, 11, 11),
                LocalDate.of(2026, 11, 15));
    }

    @Test
    void importDailyPricesSkipsSymbolAlreadyAtToday() {
        Symbol symbol = new Symbol(8L, "IBM", "IBM", "United States", "NYSE", "USD", "stock", "US4592001014");

        when(symbolService.findAll()).thenReturn(List.of(symbol));
        when(symbolPriceRepository.findLatestDateBySymbolId(symbol.id()))
                .thenReturn(Optional.of(LocalDate.of(2026, 11, 15)));

        service.importDailyPrices();

        verify(symbolPriceService, never()).getPriceHistory(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }
}

@ExtendWith(MockitoExtension.class)
class SymbolPriceStartupRunnerTest {

    @Mock
    private SymbolPriceImportService importService;

    @Test
    void startupRunnerInvokesImportDailyPrices() {
        SymbolPriceStartupRunner runner = new SymbolPriceStartupRunner(importService, "test-token");
        ApplicationArguments args = new DefaultApplicationArguments(new String[0]);

        runner.run(args);

        verify(importService).importDailyPrices();
    }
}
