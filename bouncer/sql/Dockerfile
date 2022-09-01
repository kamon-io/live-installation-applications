FROM postgres:11.1
ADD "00-schema.sql" "/docker-entrypoint-initdb.d/00-schema.sql"
ADD "01-organizations.sql" "/docker-entrypoint-initdb.d/01-organizations.sql"
ADD "02-keys.sql" "/docker-entrypoint-initdb.d/02-keys.sql"
EXPOSE 5432