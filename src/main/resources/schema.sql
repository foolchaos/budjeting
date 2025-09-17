CREATE TABLE IF NOT EXISTS app_request_header (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    request_year INTEGER NOT NULL
);

ALTER TABLE app_request_header
    ADD COLUMN IF NOT EXISTS request_year INTEGER;

UPDATE app_request_header
SET request_year = CAST(date_part('year', current_date) AS INTEGER)
WHERE request_year IS NULL;

ALTER TABLE app_request_header
    ALTER COLUMN request_year SET NOT NULL;

INSERT INTO app_request_header (name, request_year)
SELECT 'Заявка без названия', CAST(date_part('year', current_date) AS INTEGER)
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
END $$;

ALTER TABLE IF EXISTS app_request DROP CONSTRAINT IF EXISTS ukh9pq78ww5l63y6p9ssblwfv2o;
