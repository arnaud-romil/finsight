package com.finsight.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NormalizedPricePoint(
        LocalDate date,
        BigDecimal index,
        LocalDate referenceDate,
        BigDecimal referencePrice) {
}
