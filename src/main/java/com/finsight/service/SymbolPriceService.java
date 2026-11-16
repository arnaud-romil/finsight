package com.finsight.service;

import com.finsight.client.EodhdClient;
import com.finsight.dto.EodhdDailyPriceResponse;
import com.finsight.model.DailyPrice;
import com.finsight.model.NormalizedPricePoint;
import com.finsight.model.NormalizedSymbolPerformance;
import com.finsight.model.Symbol;
import com.finsight.model.SymbolPrice;
import com.finsight.repository.SymbolPriceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class SymbolPriceService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int INDEX_SCALE = 10;

    private final EodhdClient eodhdClient;
    private final SymbolPriceRepository symbolPriceRepository;

    public SymbolPriceService(EodhdClient eodhdClient, SymbolPriceRepository symbolPriceRepository) {
        this.eodhdClient = eodhdClient;
        this.symbolPriceRepository = symbolPriceRepository;
    }

    public List<DailyPrice> getPriceHistory(Symbol symbol, LocalDate from) {
        if (symbol == null) {
            throw new IllegalArgumentException("symbol must not be null");
        }
        if (from == null) {
            throw new IllegalArgumentException("from date must not be null");
        }

        List<EodhdDailyPriceResponse> responses = eodhdClient.getDailyPrices(symbol, from);
        if (responses == null || responses.isEmpty()) {
            return List.of();
        }

        return responses.stream()
                .map(response -> new DailyPrice(
                        response.date(),
                        response.open(),
                        response.high(),
                        response.low(),
                        response.close(),
                        response.adjustedClose(),
                        response.volume()))
                .toList();
    }

    public List<NormalizedPricePoint> calculateNormalizedPerformance(List<DailyPrice> prices) {
        if (prices == null || prices.isEmpty()) {
            return List.of();
        }

        List<DailyPrice> validPrices = prices.stream()
                .filter(Objects::nonNull)
                .filter(price -> price.date() != null)
                .filter(price -> price.adjustedClose() != null)
                .filter(price -> price.adjustedClose().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(DailyPrice::date))
                .toList();

        if (validPrices.isEmpty()) {
            return List.of();
        }

        DailyPrice reference = validPrices.getFirst();
        BigDecimal referencePrice = reference.adjustedClose();
        LocalDate referenceDate = reference.date();

        return validPrices.stream()
                .map(price -> {
                    BigDecimal normalizedIndex = price.adjustedClose()
                            .divide(referencePrice, INDEX_SCALE, RoundingMode.HALF_UP)
                            .multiply(ONE_HUNDRED)
                            .setScale(INDEX_SCALE, RoundingMode.HALF_UP);
                    return new NormalizedPricePoint(price.date(), normalizedIndex, referenceDate, referencePrice);
                })
                .toList();
    }

    public List<NormalizedPricePoint> calculateNormalizedPerformanceForSymbols(List<SymbolPrice> prices) {
        if (prices == null || prices.isEmpty()) {
            return List.of();
        }

        List<SymbolPrice> validPrices = prices.stream()
                .filter(Objects::nonNull)
                .filter(price -> price.date() != null)
                .filter(price -> price.adjustedClose() != null)
                .filter(price -> price.adjustedClose().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(SymbolPrice::date))
                .toList();

        if (validPrices.isEmpty()) {
            return List.of();
        }

        SymbolPrice reference = validPrices.getFirst();
        BigDecimal referencePrice = reference.adjustedClose();
        LocalDate referenceDate = reference.date();

        return validPrices.stream()
                .map(price -> {
                    BigDecimal normalizedIndex = price.adjustedClose()
                            .divide(referencePrice, INDEX_SCALE, RoundingMode.HALF_UP)
                            .multiply(ONE_HUNDRED)
                            .setScale(INDEX_SCALE, RoundingMode.HALF_UP);
                    return new NormalizedPricePoint(price.date(), normalizedIndex, referenceDate, referencePrice);
                })
                .toList();
    }

    public List<NormalizedPricePoint> normalizePerformance(List<DailyPrice> prices) {
        return calculateNormalizedPerformance(prices);
    }

    public List<NormalizedPricePoint> normalizePerformanceForSymbols(List<SymbolPrice> prices) {
        return calculateNormalizedPerformanceForSymbols(prices);
    }

    public List<NormalizedPricePoint> getNormalizedPerformance(List<DailyPrice> prices) {
        return calculateNormalizedPerformance(prices);
    }

    public List<NormalizedPricePoint> getNormalizedPerformanceForSymbols(List<SymbolPrice> prices) {
        return calculateNormalizedPerformanceForSymbols(prices);
    }

    public List<NormalizedSymbolPerformance> normalizePerformance(List<Long> symbolIds, LocalDate startDate,
            LocalDate endDate) {
        if (symbolIds == null || symbolIds.isEmpty()) {
            return List.of();
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate must not be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be before or equal to endDate");
        }

        if (symbolPriceRepository == null) {
            return List.of();
        }

        List<SymbolPrice> rows = symbolPriceRepository.findBySymbolIdsAndDateBetween(symbolIds, startDate, endDate);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        Map<Long, List<SymbolPrice>> groupedBySymbol = rows.stream()
                .filter(Objects::nonNull)
                .filter(row -> row.date() != null)
                .filter(row -> row.adjustedClose() != null)
                .filter(row -> row.adjustedClose().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.groupingBy(
                        SymbolPrice::symbolId,
                        LinkedHashMap::new,
                        Collectors.toList()));

        return symbolIds.stream()
                .filter(groupedBySymbol::containsKey)
                .map(groupedBySymbol::get)
                .map(this::normalizePerformanceForSingleSeries)
                .toList();
    }

    public List<NormalizedSymbolPerformance> normalizePerformanceForSymbols(List<Long> symbolIds, LocalDate startDate,
            LocalDate endDate) {
        return normalizePerformance(symbolIds, startDate, endDate);
    }

    public List<NormalizedSymbolPerformance> getNormalizedPerformance(List<Long> symbolIds, LocalDate startDate,
            LocalDate endDate) {
        return normalizePerformance(symbolIds, startDate, endDate);
    }

    public List<NormalizedSymbolPerformance> getNormalizedPerformanceForSymbols(List<Long> symbolIds, LocalDate startDate,
            LocalDate endDate) {
        return normalizePerformance(symbolIds, startDate, endDate);
    }

    private NormalizedSymbolPerformance normalizePerformanceForSingleSeries(List<SymbolPrice> prices) {
        List<SymbolPrice> validPrices = prices.stream()
                .filter(Objects::nonNull)
                .filter(price -> price.date() != null)
                .filter(price -> price.adjustedClose() != null)
                .filter(price -> price.adjustedClose().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(SymbolPrice::date))
                .toList();

        if (validPrices.isEmpty()) {
            return new NormalizedSymbolPerformance(null, null, null, null, List.of());
        }

        SymbolPrice reference = validPrices.getFirst();
        BigDecimal referencePrice = reference.adjustedClose();
        LocalDate referenceDate = reference.date();

        List<NormalizedPricePoint> normalizedPoints = validPrices.stream()
                .map(price -> {
                    BigDecimal normalizedIndex = price.adjustedClose()
                            .divide(referencePrice, INDEX_SCALE, RoundingMode.HALF_UP)
                            .multiply(ONE_HUNDRED)
                            .setScale(INDEX_SCALE, RoundingMode.HALF_UP);
                    return new NormalizedPricePoint(price.date(), normalizedIndex, referenceDate, referencePrice);
                })
                .toList();

        return new NormalizedSymbolPerformance(
                reference.symbolId(),
                reference.symbolCode(),
                referenceDate,
                referencePrice,
                normalizedPoints);
    }
}
