package com.finsight.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SymbolPriceStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SymbolPriceStartupRunner.class);

    private final SymbolPriceImportService importService;
    private final String apiToken;

    public SymbolPriceStartupRunner(
            SymbolPriceImportService importService,
            @Value("${eodhd.api.token:}") String apiToken) {
        this.importService = importService;
        this.apiToken = apiToken;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (apiToken == null || apiToken.isBlank()) {
            log.info("Aucun token EODHD configuré. Import des cours au démarrage ignoré.");
            return;
        }

        importService.importDailyPrices();
    }
}
