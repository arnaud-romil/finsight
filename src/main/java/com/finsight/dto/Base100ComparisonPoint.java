package com.finsight.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Base100ComparisonPoint(
        LocalDate date,
        BigDecimal adjustedClose,
        BigDecimal base100) {
}
