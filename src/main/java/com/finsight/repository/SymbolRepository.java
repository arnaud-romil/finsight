package com.finsight.repository;

import com.finsight.model.Symbol;
import java.util.Collections;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SymbolRepository {

    private final JdbcTemplate jdbcTemplate;

    public SymbolRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Symbol> findAll() {
        String sql = "SELECT id, code, name, country, exchange, currency, type, isin FROM symbol ORDER BY code ASC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> new Symbol(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("country"),
                rs.getString("exchange"),
                rs.getString("currency"),
                rs.getString("type"),
                rs.getString("isin")));
    }

    public List<Symbol> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(", ", Collections.nCopies(ids.size(), "?"));
        String sql = "SELECT id, code, name, country, exchange, currency, type, isin "
                + "FROM symbol WHERE id IN (" + placeholders + ") ORDER BY code ASC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> new Symbol(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("country"),
                rs.getString("exchange"),
                rs.getString("currency"),
                rs.getString("type"),
                rs.getString("isin")), ids.toArray());
    }
}
