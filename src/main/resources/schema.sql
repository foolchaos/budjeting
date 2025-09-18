CREATE TABLE IF NOT EXISTS app_request_header (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    request_year INTEGER NOT NULL
);

ALTER TABLE app_request_header
    ADD COLUMN IF NOT EXISTS request_year INTEGER;

UPDATE app_request_header
SET request_year = COALESCE(request_year, EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER);

ALTER TABLE app_request_header
    ALTER COLUMN request_year SET NOT NULL;

INSERT INTO app_request_header (name, request_year)
SELECT 'Заявка без названия', EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER
WHERE NOT EXISTS (SELECT 1 FROM app_request_header);

DO $$
DECLARE
    default_request_id BIGINT;
BEGIN
    SELECT id
    INTO default_request_id
    FROM app_request_header
    ORDER BY id
    LIMIT 1;

    IF EXISTS (
        SELECT 1
        FROM pg_catalog.pg_tables
        WHERE schemaname = current_schema()
          AND tablename = 'app_request'
    ) THEN
        EXECUTE 'ALTER TABLE app_request ADD COLUMN IF NOT EXISTS request_id BIGINT';

        EXECUTE 'UPDATE app_request SET request_id = $1 WHERE request_id IS NULL'
            USING default_request_id;

        BEGIN
            EXECUTE 'ALTER TABLE app_request ALTER COLUMN request_id SET NOT NULL';
        EXCEPTION
            WHEN undefined_column THEN
                NULL;
        END;

        BEGIN
            EXECUTE 'ALTER TABLE app_request ADD CONSTRAINT fk_app_request_request_header FOREIGN KEY (request_id) REFERENCES app_request_header (id)';
        EXCEPTION
            WHEN duplicate_object THEN
                NULL;
            WHEN undefined_column THEN
                NULL;
        END;
    END IF;
END
$$;

ALTER TABLE app_request_header
    ADD COLUMN IF NOT EXISTS cfo_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_schema = current_schema()
          AND table_name = 'app_request_header'
          AND constraint_name = 'uk_app_request_header_cfo'
    ) THEN
        EXECUTE 'ALTER TABLE app_request_header ADD CONSTRAINT uk_app_request_header_cfo UNIQUE (cfo_id)';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_schema = current_schema()
          AND table_name = 'app_request_header'
          AND constraint_name = 'fk_app_request_header_cfo'
    ) THEN
        EXECUTE 'ALTER TABLE app_request_header ADD CONSTRAINT fk_app_request_header_cfo FOREIGN KEY (cfo_id) REFERENCES cfo (id)';
    END IF;
END
$$;
