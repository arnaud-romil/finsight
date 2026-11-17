package com.finsight.dto;

import java.math.BigDecimal;
import java.util.List;

public record Base100ComparisonSeries(
        Long symbolId,
        String symbolCode,
        String name,
        String exchange,
        String currency,
        BigDecimal baseline,
        List<Base100ComparisonPoint> points) {
}
