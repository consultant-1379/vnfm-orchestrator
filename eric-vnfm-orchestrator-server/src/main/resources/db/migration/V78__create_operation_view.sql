create view lifecycle_operation_view as
select  o.*, v.vnf_instance_name, v.cluster_name, v.vnf_id, v.namespace
from app_lifecycle_operations o left outer join app_vnf_instance v
on v.vnf_id = o.vnf_instance_id order by o.state_entered_time desc;

