CREATE TABLE service_model
(
    id                       VARCHAR UNIQUE,
    service_model_id         VARCHAR NOT NULL,
    service_model_name       VARCHAR NOT NULL,
    vnfd_id                  VARCHAR NOT NULL,
    package_id               VARCHAR NOT NULL,
    service_model_owner_type VARCHAR NOT NULL,
    CONSTRAINT pk_service_model PRIMARY KEY (id)
);