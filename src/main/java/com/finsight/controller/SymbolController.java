package com.finsight.controller;

import com.finsight.dto.SymbolResponse;
import com.finsight.service.SymbolService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/symbols")
public class SymbolController {

    private final SymbolService symbolService;

    public SymbolController(SymbolService symbolService) {
        this.symbolService = symbolService;
    }

    @GetMapping
    public List<SymbolResponse> findAll() {
        return symbolService.findAll().stream()
                .map(symbol -> new SymbolResponse(
                        symbol.id(),
                        symbol.code(),
                        symbol.name(),
                        symbol.exchange(),
                        symbol.currency()))
                .toList();
    }
}
