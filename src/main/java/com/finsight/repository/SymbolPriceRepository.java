package com.finsight.repository;

import com.finsight.model.DailyPrice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SymbolPriceRepository {

    private final JdbcTemplate jdbcTemplate;

    public SymbolPriceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Long symbolId, DailyPrice price) {
        String sql = "INSERT INTO symbol_price (symbol_id, price_date, open, high, low, close, adjusted_close, volume) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(
                sql,
                symbolId,
                price.date(),
                price.open(),
                price.high(),
                price.low(),
                price.close(),
                price.adjustedClose(),
                price.volume());
    }
}
