package com.finsight.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.finsight.dto.EodhdDailyPriceResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

class EodhdClientTest {

    @Test
    void getDailyPricesUsesExpectedUrlAndDeserializesResponse() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory("https://eodhd.com/api"));

        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        RestClient restClient = RestClient.create(restTemplate);
        EodhdClient client = new EodhdClient(restClient, "test-token");

        server.expect(
                requestTo("https://eodhd.com/api/eod/GOLD.PA?api_token=test-token&from=2026-10-01&period=d&fmt=json"))
                .andExpect(header("Accept", "application/json"))
                .andExpect(queryParam("api_token", "test-token"))
                .andExpect(queryParam("from", "2026-10-01"))
                .andExpect(queryParam("period", "d"))
                .andExpect(queryParam("fmt", "json"))
                .andRespond(withSuccess("""
                        [
                          {
                            "date": "2026-11-02",
                            "open": 138.11,
                            "high": 140.35,
                            "low": 137.49,
                            "close": 140.35,
                            "adjusted_close": 140.35,
                            "volume": 35499
                          },
                          {
                            "date": "2026-11-03",
                            "open": 138.17,
                            "high": 140.4,
                            "low": 137.88,
                            "close": 139.11,
                            "adjusted_close": 139.11,
                            "volume": 13122
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<EodhdDailyPriceResponse> prices = client.getDailyPrices("GOLD", "PA", LocalDate.of(2026, 10, 1));

        assertThat(prices).hasSize(2);
        assertThat(prices.getFirst().date()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(prices.getFirst().adjustedClose()).isEqualByComparingTo("140.35");
        assertThat(prices.get(1).volume()).isEqualTo(13122L);
        server.verify();
    }
}
