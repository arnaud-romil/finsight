package com.finsight.dto;

import java.time.LocalDate;
import java.util.List;

public record Base100ComparisonResponse(
        LocalDate from,
        LocalDate to,
        List<Base100ComparisonSeries> series,
        List<Long> ignoredSymbolIds) {
}
