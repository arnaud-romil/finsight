package com.finsight.service;

import com.finsight.client.EodhdClient;
import com.finsight.dto.EodhdDailyPriceResponse;
import com.finsight.model.DailyPrice;
import com.finsight.model.Symbol;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SymbolPriceService {

    private final EodhdClient eodhdClient;

    public SymbolPriceService(EodhdClient eodhdClient) {
        this.eodhdClient = eodhdClient;
    }

    public List<DailyPrice> getPriceHistory(Symbol symbol, LocalDate from, LocalDate to) {
        if (symbol == null) {
            throw new IllegalArgumentException("symbol must not be null");
        }
        if (from == null) {
            throw new IllegalArgumentException("from date must not be null");
        }
        if (to == null) {
            throw new IllegalArgumentException("to date must not be null");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from date must be before or equal to to date");
        }

        List<EodhdDailyPriceResponse> responses = eodhdClient.getDailyPrices(symbol, from, to);
        if (responses == null || responses.isEmpty()) {
            return List.of();
        }

        return responses.stream()
                .map(response -> new DailyPrice(
                        response.date(),
                        response.open(),
                        response.high(),
                        response.low(),
                        response.close(),
                        response.adjustedClose(),
                        response.volume()))
                .toList();
    }
}
