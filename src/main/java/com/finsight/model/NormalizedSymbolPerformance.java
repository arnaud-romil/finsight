package com.finsight.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record NormalizedSymbolPerformance(
        Long symbolId,
        String symbolCode,
        LocalDate referenceDate,
        BigDecimal referencePrice,
        List<NormalizedPricePoint> points) {
}
