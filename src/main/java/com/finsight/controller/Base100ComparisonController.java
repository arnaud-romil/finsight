package com.finsight.controller;

import com.finsight.dto.Base100ComparisonResponse;
import com.finsight.service.Base100ComparisonService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comparisons")
public class Base100ComparisonController {

    private final Base100ComparisonService comparisonService;

    public Base100ComparisonController(Base100ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    @GetMapping("/base-100")
    public Base100ComparisonResponse compareBase100(
            @RequestParam List<Long> symbolIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return comparisonService.compare(symbolIds, from, to);
    }
}
