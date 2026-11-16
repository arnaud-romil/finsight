package com.finsight.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SymbolPrice(
        Long symbolId,
        String symbolCode,
        LocalDate date,
        BigDecimal adjustedClose) {
}
