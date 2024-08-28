CREATE OR REPLACE FUNCTION updateClusterConfigState() RETURNS TRIGGER AS $cluster_usage_state$
   DECLARE
     clusterConfigCount integer;
   BEGIN
     raise notice 'Operation is: %', TG_OP;
     CASE TG_OP
     WHEN 'INSERT' THEN
       raise notice 'VnfInstanceId is : %', NEW.instance_id;
       update APP_CLUSTER_CONFIG_FILE set CONFIG_FILE_STATUS = 'IN_USE' where CONFIG_FILE_NAME = NEW.CONFIG_FILE_NAME;
	   RETURN NEW;
     WHEN 'DELETE' THEN
       select count(*) into clusterConfigCount from cluster_config_instances where CONFIG_FILE_NAME = OLD.CONFIG_FILE_NAME;
         if (clusterConfigCount = 0) THEN
           update APP_CLUSTER_CONFIG_FILE set CONFIG_FILE_STATUS = 'NOT_IN_USE' where CONFIG_FILE_NAME = OLD.CONFIG_FILE_NAME;
           raise notice 'Cluster config count is : %', clusterConfigCount;
         end if;
		 RETURN OLD;
         ELSE
           RAISE EXCEPTION 'Unknown Operation: "%".', TG_OP;
		   RETURN NULL;
     END CASE;
   END;
$cluster_usage_state$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS cluster_usage_state ON cluster_config_instances;

CREATE TRIGGER cluster_usage_state AFTER INSERT OR DELETE ON cluster_config_instances
FOR EACH ROW EXECUTE PROCEDURE updateClusterConfigState();
