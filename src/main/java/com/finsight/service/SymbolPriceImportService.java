package com.finsight.service;

import com.finsight.model.DailyPrice;
import com.finsight.model.Symbol;
import com.finsight.repository.SymbolPriceRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class SymbolPriceImportService {

    private static final Logger log = LoggerFactory.getLogger(SymbolPriceImportService.class);

    private final SymbolService symbolService;
    private final SymbolPriceService symbolPriceService;
    private final SymbolPriceRepository symbolPriceRepository;
    private final Clock clock;

    public SymbolPriceImportService(
            SymbolService symbolService,
            SymbolPriceService symbolPriceService,
            SymbolPriceRepository symbolPriceRepository,
            Clock clock) {
        this.symbolService = symbolService;
        this.symbolPriceService = symbolPriceService;
        this.symbolPriceRepository = symbolPriceRepository;
        this.clock = clock;
    }

    public void importDailyPrices() {
        LocalDate from = LocalDate.now(clock);
        List<Symbol> symbols = symbolService.findAll();

        log.info("Début de l'importation quotidienne des cours. Nombre de symboles récupérés : {}", symbols.size());

        for (Symbol symbol : symbols) {
            try {
                log.info("Début du traitement du symbole {}.", symbol.code());

                List<DailyPrice> prices = symbolPriceService.getPriceHistory(symbol, from);
                if (prices == null || prices.isEmpty()) {
                    log.info("Aucun cours retourné pour le symbole {}.", symbol.code());
                    continue;
                }

                log.info("{} cours reçus pour le symbole {}.", prices.size(), symbol.code());

                for (DailyPrice price : prices) {
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
            } catch (RuntimeException ex) {
                log.error("Erreur lors de la récupération des cours pour le symbole {}.", symbol.code(), ex);
            }
        }

        log.info("Fin de l'importation quotidienne.");
    }
}
