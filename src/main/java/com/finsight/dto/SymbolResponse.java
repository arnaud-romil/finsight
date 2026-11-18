package com.finsight.dto;

public record SymbolResponse(
        Long id,
        String code,
        String name,
        String exchange,
        String currency) {
}
