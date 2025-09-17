package com.example.budget.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RequestPositionSchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RequestPositionSchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public RequestPositionSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("app_request")) {
            log.debug("Skipping request position schema migration because app_request table is missing");
            return;
        }

        dropSingleColumnUniqueConstraints("app_request", "contract_id");
        dropSingleColumnUniqueConstraints("app_request", "counterparty_id");
    }

    private boolean tableExists(String tableName) {
        try {
            String regclass = jdbcTemplate.queryForObject("SELECT to_regclass(?)", String.class, tableName);
            return regclass != null;
        } catch (DataAccessException ex) {
            log.warn("Failed to inspect table {}: {}", tableName, ex.getMessage());
            return false;
        }
    }

    private void dropSingleColumnUniqueConstraints(String tableName, String columnName) {
        String sql = """
                SELECT con.conname AS constraint_name
                FROM pg_constraint con
                JOIN pg_attribute att ON att.attrelid = con.conrelid
                    AND att.attnum = ANY (con.conkey)
                WHERE con.conrelid = to_regclass(?)
                  AND con.contype = 'u'
                  AND array_length(con.conkey, 1) = 1
                  AND att.attname = ?
                """;

        List<String> constraintNames = jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getString("constraint_name"),
                tableName, columnName);

        if (constraintNames.isEmpty()) {
            log.debug("No single-column unique constraints found for {}.{}", tableName, columnName);
            return;
        }

        for (String constraintName : constraintNames) {
            String dropSql = "ALTER TABLE " + tableName + " DROP CONSTRAINT " + quoteIdentifier(constraintName);
            try {
                jdbcTemplate.execute(dropSql);
                log.info("Dropped unique constraint {} on {}.{}", constraintName, tableName, columnName);
            } catch (DataAccessException ex) {
                log.warn("Failed to drop constraint {} on {}.{}: {}", constraintName, tableName, columnName, ex.getMessage());
            }
        }
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
