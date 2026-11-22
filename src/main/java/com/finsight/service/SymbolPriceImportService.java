package com.finsight.service;

import com.finsight.model.DailyPrice;
import com.finsight.model.Symbol;
import com.finsight.repository.SymbolPriceRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class SymbolPriceImportService {

    private static final Logger log = LoggerFactory.getLogger(SymbolPriceImportService.class);

    private final SymbolService symbolService;
    private final SymbolPriceService symbolPriceService;
    private final SymbolPriceRepository symbolPriceRepository;
    private final Clock clock;
    private final LocalDate initialImportDate;

    public SymbolPriceImportService(
            SymbolService symbolService,
            SymbolPriceService symbolPriceService,
            SymbolPriceRepository symbolPriceRepository,
            Clock clock,
            @Value("${eodhd.import.initial-date:2000-01-01}") String initialImportDate) {
        this.symbolService = symbolService;
        this.symbolPriceService = symbolPriceService;
        this.symbolPriceRepository = symbolPriceRepository;
        this.clock = clock;
        this.initialImportDate = parseInitialImportDate(initialImportDate);
    }

    private LocalDate parseInitialImportDate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("eodhd.import.initial-date must not be blank");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(value);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("eodhd.import.initial-date must use ISO-8601 format", ex);
        }
        LocalDate today = LocalDate.now(clock);
        if (date.isAfter(today)) {
            throw new IllegalArgumentException("eodhd.import.initial-date must not be after today");
        }
        return date;
    }

    public void importDailyPrices() {
        LocalDate to = LocalDate.now(clock);
        List<Symbol> symbols = symbolService.findAll();

        log.info("Début de l'importation des cours. Nombre de symboles récupérés : {}", symbols.size());

        for (Symbol symbol : symbols) {
            try {
                Optional<LocalDate> latestDate = symbolPriceRepository.findLatestDateBySymbolId(symbol.id());
                LocalDate from = latestDate
                        .map(date -> date.plusDays(1))
                        .orElse(initialImportDate);

                if (from.isAfter(to)) {
                    log.info("Import déjà à jour pour le symbole {}.", symbol.code());
                    continue;
                }

                log.info("Récupération des cours du symbole {} du {} au {}.", symbol.code(), from, to);
                List<DailyPrice> prices = symbolPriceService.getPriceHistory(symbol, from, to);
                if (prices == null || prices.isEmpty()) {
                    log.info("Aucun cours retourné pour le symbole {}.", symbol.code());
                    continue;
                }

                log.info("{} cours reçus pour le symbole {}.", prices.size(), symbol.code());
                for (DailyPrice price : prices) {
                    savePrice(symbol, price);
                }
            } catch (RuntimeException ex) {
                log.error("Erreur lors de la récupération des cours pour le symbole {}.", symbol.code(), ex);
            }
        }

        log.info("Fin de l'importation des cours.");
    }

    private void savePrice(Symbol symbol, DailyPrice price) {
        try {
            symbolPriceRepository.save(symbol.id(), price);
            log.info("Cours enregistré pour le symbole {} à la date {}.", symbol.code(), price.date());
        } catch (DuplicateKeyException _) {
            log.info(
                    "Cours déjà présent pour le symbole {} à la date {}. Insertion ignorée.",
                    symbol.code(),
                    price.date());
        } catch (RuntimeException ex) {
            log.error(
                    "Erreur lors de l'enregistrement du cours du symbole {} à la date {}.",
                    symbol.code(),
                    price.date(),
                    ex);
        }
    }
}
