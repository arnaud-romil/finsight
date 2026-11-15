package com.finsight.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyPrice(
        LocalDate date,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal adjustedClose,
        Long volume) {
}
