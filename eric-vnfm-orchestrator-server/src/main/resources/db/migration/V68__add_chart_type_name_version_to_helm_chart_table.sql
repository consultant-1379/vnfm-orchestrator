-- -----------------------------------------------------
-- ENUM `chart_type_enum`
-- -----------------------------------------------------

create type chart_type_enum as ENUM('CNF', 'CRD');

Alter table helm_chart ADD helm_chart_name VARCHAR DEFAULT '', ADD helm_chart_version VARCHAR DEFAULT '', ADD helm_chart_type chart_type_enum DEFAULT 'CNF';