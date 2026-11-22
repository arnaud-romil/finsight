package com.finsight.client;

import com.finsight.dto.EodhdDailyPriceResponse;
import com.finsight.model.Symbol;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class EodhdClient {

    private final RestClient restClient;
    private final String apiToken;

    public EodhdClient(RestClient restClient, String apiToken) {
        if (restClient == null) {
            throw new IllegalArgumentException("restClient must not be null");
        }
        this.restClient = restClient;
        this.apiToken = apiToken == null ? "" : apiToken;
    }

    public List<EodhdDailyPriceResponse> getDailyPrices(Symbol symbol, LocalDate from, LocalDate to) {
        if (symbol == null) {
            throw new IllegalArgumentException("symbol must not be null");
        }
        return getDailyPrices(symbol.code(), symbol.exchange(), from, to);
    }

    public List<EodhdDailyPriceResponse> getDailyPrices(String code, String exchange, LocalDate from, LocalDate to) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        if (exchange == null || exchange.isBlank()) {
            throw new IllegalArgumentException("exchange must not be blank");
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

        String symbol = code + "." + exchange;

        if (apiToken.isBlank()) {
            throw new IllegalStateException("EODHD API token is not configured");
        }

        try {
            EodhdDailyPriceResponse[] response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/eod/{symbol}")
                            .queryParam("api_token", apiToken)
                            .queryParam("from", from)
                            .queryParam("to", to)
                            .queryParam("period", "d")
                            .queryParam("fmt", "json")
                            .build(symbol))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (request, httpResponse) -> {
                                throw new IllegalStateException(
                                        "EODHD API request failed with status " + httpResponse.getStatusCode());
                            })
                    .body(EodhdDailyPriceResponse[].class);

            return response == null ? List.of() : Arrays.asList(response);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Unable to fetch EODHD price history for symbol " + symbol, ex);
        }
    }
}
