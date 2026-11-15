package com.finsight.config;

import com.finsight.client.EodhdClient;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class EodhdConfiguration {

    @Bean
    public RestClient.Builder eodhdRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient eodhdRestClient(
            RestClient.Builder eodhdRestClientBuilder,
            @Value("${eodhd.api.base-url}") String baseUrl) {
        return eodhdRestClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public EodhdClient eodhdClient(
            RestClient eodhdRestClient,
            @Value("${eodhd.api.token}") String apiToken) {
        return new EodhdClient(eodhdRestClient, apiToken);
    }
}
