CREATE TABLE IF NOT EXISTS app_request_header (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    request_year INTEGER NOT NULL
);

@@

ALTER TABLE app_request_header
    ADD COLUMN IF NOT EXISTS request_year INTEGER;

@@

UPDATE app_request_header
SET request_year = COALESCE(request_year, EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER);

@@

ALTER TABLE app_request_header
    ALTER COLUMN request_year SET NOT NULL;

@@

INSERT INTO app_request_header (name, request_year)
SELECT 'Заявка без названия', EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER
WHERE NOT EXISTS (SELECT 1 FROM app_request_header);

@@

ALTER TABLE app_request_header
    ADD COLUMN IF NOT EXISTS cfo_id BIGINT;

@@

DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    FOR constraint_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY (con.conkey)
        WHERE rel.relname = 'app_request_header'
          AND con.contype = 'u'
          AND att.attname = 'cfo_id'
    LOOP
        EXECUTE format('ALTER TABLE app_request_header DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END
$$;

@@

DO $$
DECLARE
    index_name TEXT;
BEGIN
    FOR index_name IN
        SELECT indexname
        FROM pg_indexes
        WHERE schemaname = current_schema()
          AND tablename = 'app_request_header'
          AND indexdef ILIKE 'CREATE UNIQUE%'
          AND POSITION('(cfo_id' IN indexdef) > 0
    LOOP
        EXECUTE format('DROP INDEX IF EXISTS %I', index_name);
    END LOOP;
END
$$;

@@

DO $$
DECLARE
    default_cfo_id BIGINT;
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'cfo'
    ) THEN
        SELECT id
        INTO default_cfo_id
        FROM cfo
        ORDER BY id
        LIMIT 1;

        IF default_cfo_id IS NOT NULL THEN
            UPDATE app_request_header
            SET cfo_id = default_cfo_id
            WHERE cfo_id IS NULL;
        END IF;
    END IF;
END
$$;

@@

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'app_request_header'
          AND column_name = 'cfo_id'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
            FROM app_request_header
            WHERE cfo_id IS NULL
        ) THEN
            ALTER TABLE app_request_header
                ALTER COLUMN cfo_id SET NOT NULL;
        END IF;
    END IF;
END
$$;

@@

DO $$
DECLARE
    fk_exists BOOLEAN;
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'cfo'
    ) THEN
        SELECT EXISTS (
            SELECT 1
            FROM pg_constraint con
            JOIN pg_class rel ON rel.oid = con.conrelid
            WHERE rel.relname = 'app_request_header'
              AND con.contype = 'f'
              AND array_length(con.conkey, 1) = 1
              AND con.conkey[1] = (
                  SELECT attnum
                  FROM pg_attribute
                  WHERE attrelid = rel.oid
                    AND attname = 'cfo_id'
              )
        )
        INTO fk_exists;

        IF NOT fk_exists THEN
            EXECUTE 'ALTER TABLE app_request_header ADD CONSTRAINT fk_app_request_header_cfo FOREIGN KEY (cfo_id) REFERENCES cfo(id)';
        END IF;
    END IF;
END
$$;

@@

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

@@
