package com.finsight.service;

import com.finsight.dto.Base100ComparisonPoint;
import com.finsight.dto.Base100ComparisonResponse;
import com.finsight.dto.Base100ComparisonSeries;
import com.finsight.model.Symbol;
import com.finsight.model.SymbolPrice;
import com.finsight.repository.SymbolPriceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class Base100ComparisonService {

    private static final int MAX_SYMBOLS = 50;
    private static final int CALCULATION_SCALE = 12;
    private static final int RESULT_SCALE = 6;

    private final SymbolService symbolService;
    private final SymbolPriceRepository symbolPriceRepository;

    public Base100ComparisonService(
            SymbolService symbolService,
            SymbolPriceRepository symbolPriceRepository) {
        this.symbolService = symbolService;
        this.symbolPriceRepository = symbolPriceRepository;
    }

    public Base100ComparisonResponse compare(List<Long> symbolIds, LocalDate from, LocalDate to) {
        if (symbolIds == null || symbolIds.isEmpty()) {
            throw new IllegalArgumentException("symbolIds must not be empty");
        }
        if (symbolIds.size() > MAX_SYMBOLS) {
            throw new IllegalArgumentException("A maximum of " + MAX_SYMBOLS + " symbols is allowed");
        }
        if (symbolIds.stream().anyMatch(id -> id == null)) {
            throw new IllegalArgumentException("symbolIds must not contain null values");
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to dates must not be null");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from date must be before or equal to to date");
        }

        List<Long> distinctIds = symbolIds.stream().distinct().toList();
        List<Symbol> symbols = symbolService.findByIds(distinctIds);
        List<SymbolPrice> prices = symbolPriceRepository.findBySymbolIdsAndDateBetween(distinctIds, from, to);

        Map<Long, Symbol> symbolsById = symbols.stream()
                .collect(Collectors.toMap(Symbol::id, symbol -> symbol));

        Map<Long, List<SymbolPrice>> pricesBySymbol = new LinkedHashMap<>();
        for (SymbolPrice price : prices) {
            pricesBySymbol.computeIfAbsent(price.symbolId(), ignored -> new ArrayList<>()).add(price);
        }

        List<Base100ComparisonSeries> series = new ArrayList<>();
        List<Long> ignoredSymbolIds = new ArrayList<>();

        for (Long symbolId : distinctIds) {
            Symbol symbol = symbolsById.get(symbolId);
            if (symbol == null) {
                ignoredSymbolIds.add(symbolId);
                continue;
            }

            List<SymbolPrice> symbolPrices = pricesBySymbol.get(symbolId);
            if (symbolPrices == null || symbolPrices.isEmpty()) {
                ignoredSymbolIds.add(symbolId);
                continue;
            }

            symbolPrices.sort((left, right) -> left.date().compareTo(right.date()));
            BigDecimal baseline = symbolPrices.getFirst().adjustedClose();
            if (baseline == null || baseline.compareTo(BigDecimal.ZERO) <= 0) {
                ignoredSymbolIds.add(symbolId);
                continue;
            }

            List<Base100ComparisonPoint> points = symbolPrices.stream()
                    .filter(price -> price.adjustedClose() != null)
                    .filter(price -> price.adjustedClose().compareTo(BigDecimal.ZERO) > 0)
                    .map(price -> new Base100ComparisonPoint(
                            price.date(),
                            price.adjustedClose(),
                            calculateBase100(price.adjustedClose(), baseline)))
                    .toList();

            if (points.isEmpty()) {
                ignoredSymbolIds.add(symbolId);
                continue;
            }

            series.add(new Base100ComparisonSeries(
                    symbol.id(),
                    symbol.code(),
                    symbol.name(),
                    symbol.exchange(),
                    symbol.currency(),
                    baseline,
                    points));
        }

        return new Base100ComparisonResponse(from, to, series, ignoredSymbolIds);
    }

    private BigDecimal calculateBase100(BigDecimal current, BigDecimal baseline) {
        return BigDecimal.valueOf(100)
                .multiply(current)
                .divide(baseline, CALCULATION_SCALE, RoundingMode.HALF_UP)
                .setScale(RESULT_SCALE, RoundingMode.HALF_UP);
    }
}
