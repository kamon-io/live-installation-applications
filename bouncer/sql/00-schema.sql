START TRANSACTION;
DROP SCHEMA IF EXISTS bouncer CASCADE;
CREATE SCHEMA bouncer;

CREATE TABLE bouncer.organizations (
  id SERIAL PRIMARY KEY,
  name TEXT
);

CREATE TABLE bouncer.keys (
  key_value VARCHAR(16) PRIMARY KEY,
  organization_id INT NOT NULL
);


CREATE ROLE bouncer_service WITH LOGIN PASSWORD 'password';
ALTER ROLE bouncer_service SET search_path TO bouncer;
GRANT USAGE ON SCHEMA bouncer TO bouncer_service;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA bouncer TO bouncer_service;
COMMIT;