package com.finsight.service;

import com.finsight.client.EodhdClient;
import com.finsight.dto.EodhdDailyPriceResponse;
import com.finsight.model.DailyPrice;
import com.finsight.model.NormalizedPricePoint;
import com.finsight.model.Symbol;
import com.finsight.model.SymbolPrice;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class SymbolPriceService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int INDEX_SCALE = 10;

    private final EodhdClient eodhdClient;

    public SymbolPriceService(EodhdClient eodhdClient) {
        this.eodhdClient = eodhdClient;
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
}
