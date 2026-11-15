package com.finsight.model;

public record Symbol(
        Long id,
        String code,
        String name,
        String country,
        String exchange,
        String currency,
        String type,
        String isin) {
}
