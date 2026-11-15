package com.finsight;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import com.finsight.model.Symbol;
import com.finsight.service.SymbolService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(FinsightApplicationTests.TestcontainersConfiguration.class)
class FinsightApplicationTests {

        @Autowired
        private DataSource dataSource;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Autowired
        private SymbolService symbolService;

        @Test
        void contextLoads() {
        }

        @Test
        void connectsToDatabaseAndExecutesQuery() throws SQLException {
                try (Connection connection = dataSource.getConnection();
                                Statement statement = connection.createStatement();
                                ResultSet resultSet = statement.executeQuery("SELECT 1")) {
                        assertTrue(connection.isValid(1));
                        assertTrue(resultSet.next());
                        assertEquals(1, resultSet.getInt(1));
                }
        }

        @Test
        void flywayCreatesSymbolTable() {
                Integer tableCount = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                                                + "WHERE TABLE_SCHEMA = CURRENT_SCHEMA() AND LOWER(TABLE_NAME) = 'symbol'",
                                Integer.class);

                assertEquals(1, tableCount);
        }

        @Test
        void flywayInsertsSevenInitialSymbols() {
                Integer symbolCount = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM symbol",
                                Integer.class);

                assertEquals(7, symbolCount);
        }

        @Test
        void flywayRecordsVersionOneMigration() {
                Integer migrationCount = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM \"flyway_schema_history\" "
                                                + "WHERE \"version\" = '1' AND \"success\" = TRUE",
                                Integer.class);

                assertEquals(1, migrationCount);
        }

        @Test
        void retrievesAllSymbols() {
                java.util.List<Symbol> symbols = symbolService.findAll();

                assertEquals(7, symbols.size());
                assertEquals("C3M", symbols.getFirst().code());
                assertEquals("PAEEM", symbols.getLast().code());
        }

        @Test
        void flywayCreatesSymbolPriceTable() {
                Integer tableCount = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                                                + "WHERE TABLE_SCHEMA = CURRENT_SCHEMA() AND LOWER(TABLE_NAME) = 'symbol_price'",
                                Integer.class);

                assertEquals(1, tableCount);
        }

        @Test
        void flywayCreatesExpectedSymbolPriceColumns() {
                Integer columnCount = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                                                + "WHERE TABLE_SCHEMA = CURRENT_SCHEMA() AND LOWER(TABLE_NAME) = 'symbol_price'",
                                Integer.class);

                assertEquals(9, columnCount);
        }

        @Test
        void insertsPriceForExistingSymbolAndPreservesNumericPrecision() {
                jdbcTemplate.update(
                                "INSERT INTO symbol_price (symbol_id, price_date, open, high, low, close, adjusted_close, volume) "
                                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                                1L,
                                java.sql.Date.valueOf("2026-10-01"),
                                new BigDecimal("146.32000000"),
                                new BigDecimal("146.85000000"),
                                new BigDecimal("145.59000000"),
                                new BigDecimal("146.57000000"),
                                new BigDecimal("146.57000000"),
                                17360L);

                BigDecimal storedOpen = jdbcTemplate.queryForObject(
                                "SELECT open FROM symbol_price WHERE symbol_id = 1 AND price_date = ?",
                                BigDecimal.class,
                                java.sql.Date.valueOf("2026-10-01"));

                assertEquals(new BigDecimal("146.32000000"), storedOpen);
        }

        @Test
        void rejectsPriceForNonExistingSymbol() {
                DataIntegrityViolationException exception = org.junit.jupiter.api.Assertions.assertThrows(
                                DataIntegrityViolationException.class,
                                () -> jdbcTemplate.update(
                                                "INSERT INTO symbol_price (symbol_id, price_date, open, high, low, close, adjusted_close, volume) "
                                                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                                                999L,
                                                java.sql.Date.valueOf("2026-10-02"),
                                                new BigDecimal("10.00"),
                                                new BigDecimal("11.00"),
                                                new BigDecimal("9.00"),
                                                new BigDecimal("10.50"),
                                                new BigDecimal("10.50"),
                                                1000L));

                assertTrue(exception.getMessage() != null && !exception.getMessage().isBlank());
        }

        @Test
        void rejectsDuplicatePriceForSameSymbolAndDate() {
                jdbcTemplate.update(
                                "INSERT INTO symbol_price (symbol_id, price_date, open, high, low, close, adjusted_close, volume) "
                                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                                1L,
                                java.sql.Date.valueOf("2026-10-03"),
                                new BigDecimal("100.00"),
                                new BigDecimal("105.00"),
                                new BigDecimal("99.00"),
                                new BigDecimal("103.00"),
                                new BigDecimal("103.00"),
                                5000L);

                org.junit.jupiter.api.Assertions.assertThrows(
                                DataIntegrityViolationException.class,
                                () -> jdbcTemplate.update(
                                                "INSERT INTO symbol_price (symbol_id, price_date, open, high, low, close, adjusted_close, volume) "
                                                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                                                1L,
                                                java.sql.Date.valueOf("2026-10-03"),
                                                new BigDecimal("101.00"),
                                                new BigDecimal("106.00"),
                                                new BigDecimal("100.00"),
                                                new BigDecimal("104.00"),
                                                new BigDecimal("104.00"),
                                                6000L));
        }

        @Test
        void allowsMultipleDatesForSameSymbol() {
                jdbcTemplate.update(
                                "INSERT INTO symbol_price (symbol_id, price_date, open, high, low, close, adjusted_close, volume) "
                                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                                1L,
                                java.sql.Date.valueOf("2026-10-04"),
                                new BigDecimal("101.00"),
                                new BigDecimal("106.00"),
                                new BigDecimal("100.00"),
                                new BigDecimal("104.00"),
                                new BigDecimal("104.00"),
                                6000L);

                jdbcTemplate.update(
                                "INSERT INTO symbol_price (symbol_id, price_date, open, high, low, close, adjusted_close, volume) "
                                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                                1L,
                                java.sql.Date.valueOf("2026-10-05"),
                                new BigDecimal("102.00"),
                                new BigDecimal("107.00"),
                                new BigDecimal("101.00"),
                                new BigDecimal("105.00"),
                                new BigDecimal("105.00"),
                                7000L);

                Integer count = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM symbol_price WHERE symbol_id = 1",
                                Integer.class);

                assertEquals(2, count);
        }

        @TestConfiguration(proxyBeanMethods = false)
        static class TestcontainersConfiguration {

                @Bean
                @ServiceConnection
                PostgreSQLContainer<?> postgresContainer() {
                        return new PostgreSQLContainer<>("postgres:17-alpine");
                }
        }
}
