package com.example.budget.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RequestDataMigration implements ApplicationRunner {

    private static final String REQUEST_TABLE = "app_request";
    private static final String REQUEST_HEADER_TABLE = "app_request_header";
    private static final String REQUEST_FK_NAME = "fk_app_request_request_header";
    private static final String DEFAULT_REQUEST_NAME = "Заявка без названия";

    private final JdbcTemplate jdbcTemplate;

    public RequestDataMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        createRequestHeaderTableIfNeeded();
        Long defaultRequestId = ensureDefaultRequestExists();

        if (!tableExists(REQUEST_TABLE)) {
            return;
        }

        addRequestIdColumnIfMissing();
        backfillMissingRequestLinks(defaultRequestId);
        enforceRequestIdNotNull();
        ensureForeignKeyConstraint();
    }

    private void createRequestHeaderTableIfNeeded() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS app_request_header (
                    id BIGSERIAL PRIMARY KEY,
                    name VARCHAR(255) NOT NULL
                )
                """);
    }

    private Long ensureDefaultRequestExists() {
        List<Long> existing = jdbcTemplate.queryForList(
                "SELECT id FROM " + REQUEST_HEADER_TABLE + " ORDER BY id LIMIT 1",
                Long.class
        );
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        return jdbcTemplate.queryForObject(
                "INSERT INTO " + REQUEST_HEADER_TABLE + " (name) VALUES (?) RETURNING id",
                Long.class,
                DEFAULT_REQUEST_NAME
        );
    }

    private void addRequestIdColumnIfMissing() {
        if (!columnExists(REQUEST_TABLE, "request_id")) {
            jdbcTemplate.execute("ALTER TABLE " + REQUEST_TABLE + " ADD COLUMN request_id BIGINT");
        }
    }

    private void backfillMissingRequestLinks(Long defaultRequestId) {
        if (defaultRequestId == null || !columnExists(REQUEST_TABLE, "request_id")) {
            return;
        }
        jdbcTemplate.update(
                "UPDATE " + REQUEST_TABLE + " SET request_id = ? WHERE request_id IS NULL",
                defaultRequestId
        );
    }

    private void enforceRequestIdNotNull() {
        if (!columnExists(REQUEST_TABLE, "request_id")) {
            return;
        }
        String isNullable = jdbcTemplate.queryForObject(
                """
                        SELECT is_nullable
                        FROM information_schema.columns
                        WHERE table_schema = current_schema()
                          AND table_name = ?
                          AND column_name = 'request_id'
                        """,
                String.class,
                REQUEST_TABLE
        );
        if (isNullable != null && !"NO".equalsIgnoreCase(isNullable)) {
            jdbcTemplate.execute("ALTER TABLE " + REQUEST_TABLE + " ALTER COLUMN request_id SET NOT NULL");
        }
    }

    private void ensureForeignKeyConstraint() {
        if (!columnExists(REQUEST_TABLE, "request_id")) {
            return;
        }
        Boolean exists = jdbcTemplate.queryForObject(
                """
                        SELECT EXISTS (
                            SELECT 1
                            FROM information_schema.table_constraints tc
                            WHERE tc.table_schema = current_schema()
                              AND tc.table_name = ?
                              AND tc.constraint_name = ?
                        )
                        """,
                Boolean.class,
                REQUEST_TABLE,
                REQUEST_FK_NAME
        );
        if (!Boolean.TRUE.equals(exists)) {
            jdbcTemplate.execute("""
                    ALTER TABLE " + REQUEST_TABLE + "
                    ADD CONSTRAINT " + REQUEST_FK_NAME + "
                    FOREIGN KEY (request_id) REFERENCES " + REQUEST_HEADER_TABLE + "(id)
                    """);
        }
    }

    private boolean tableExists(String tableName) {
        Boolean exists = jdbcTemplate.queryForObject(
                """
                        SELECT EXISTS (
                            SELECT 1
                            FROM information_schema.tables
                            WHERE table_schema = current_schema()
                              AND table_name = ?
                        )
                        """,
                Boolean.class,
                tableName
        );
        return Boolean.TRUE.equals(exists);
    }

    private boolean columnExists(String tableName, String columnName) {
        Boolean exists = jdbcTemplate.queryForObject(
                """
                        SELECT EXISTS (
                            SELECT 1
                            FROM information_schema.columns
                            WHERE table_schema = current_schema()
                              AND table_name = ?
                              AND column_name = ?
                        )
                        """,
                Boolean.class,
                tableName,
                columnName
        );
        return Boolean.TRUE.equals(exists);
    }
}
