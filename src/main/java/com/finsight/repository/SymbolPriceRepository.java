package com.finsight.repository;

import com.finsight.model.DailyPrice;
import com.finsight.model.SymbolPrice;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

    public List<SymbolPrice> findBySymbolIdsAndDateBetween(List<Long> symbolIds, LocalDate startDate,
            LocalDate endDate) {
        if (symbolIds == null || symbolIds.isEmpty()) {
            return List.of();
        }

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate must not be null");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be before or equal to endDate");
        }

        String placeholders = String.join(", ", Collections.nCopies(symbolIds.size(), "?"));
        String sql = "SELECT sp.symbol_id, s.code AS symbol_code, sp.price_date, sp.adjusted_close "
                + "FROM symbol_price sp "
                + "JOIN symbol s ON s.id = sp.symbol_id "
                + "WHERE sp.symbol_id IN (" + placeholders + ") "
                + "AND sp.price_date BETWEEN ? AND ? "
                + "ORDER BY sp.symbol_id ASC, sp.price_date ASC";

        List<Object> params = new ArrayList<>();
        params.addAll(symbolIds);
        params.add(startDate);
        params.add(endDate);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SymbolPrice(
                        rs.getLong("symbol_id"),
                        rs.getString("symbol_code"),
                        rs.getObject("price_date", LocalDate.class),
                        rs.getBigDecimal("adjusted_close")),
                params.toArray());
    }

    public LocalDate findMostRecentDateBySymbolId(Long symbolId) {
        String sql = "SELECT MAX(price_date) FROM symbol_price WHERE symbol_id = ?";

        return jdbcTemplate.queryForObject(sql, LocalDate.class, symbolId);
    }

    public Optional<SymbolPrice> findMostRecentPriceBySymbolId(Long symbolId) {
        String sql = "SELECT sp.symbol_id, s.code AS symbol_code, sp.price_date, sp.adjusted_close "
                + "FROM symbol_price sp "
                + "JOIN symbol s ON s.id = sp.symbol_id "
                + "WHERE sp.symbol_id = ? "
                + "AND sp.price_date = (SELECT MAX(price_date) FROM symbol_price WHERE symbol_id = ?) "
                + "ORDER BY sp.price_date DESC "
                + "LIMIT 1";

        List<SymbolPrice> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SymbolPrice(
                        rs.getLong("symbol_id"),
                        rs.getString("symbol_code"),
                        rs.getObject("price_date", LocalDate.class),
                        rs.getBigDecimal("adjusted_close")),
                symbolId, symbolId);

        return results.stream().findFirst();
    }

    public Optional<LocalDate> findLatestDateBySymbolId(Long symbolId) {
        if (symbolId == null) {
            throw new IllegalArgumentException("symbolId must not be null");
        }

        LocalDate latestDate = jdbcTemplate.queryForObject(
                "SELECT MAX(price_date) FROM symbol_price WHERE symbol_id = ?",
                (rs, rowNum) -> rs.getObject(1, LocalDate.class),
                symbolId);
        return Optional.ofNullable(latestDate);
    }
}
