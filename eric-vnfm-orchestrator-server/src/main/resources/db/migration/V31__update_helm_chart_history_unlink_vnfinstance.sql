ALTER TABLE helm_chart_history DROP CONSTRAINT helm_chart_history_vnf_id_fkey;
ALTER TABLE helm_chart_history DROP COLUMN vnf_id;
