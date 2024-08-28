update app_vnf_instance
set supported_operations =
'[
    {
      "operationName": "instantiate",
      "supported": true,
      "errorMessage": null
    },
    {
      "operationName": "terminate",
      "supported": true,
      "errorMessage": null
    },
    {
      "operationName": "heal",
      "supported": true,
      "errorMessage": null
    },
    {
      "operationName": "change_package",
      "supported": true,
      "errorMessage": null
    },
    {
      "operationName": "scale",
      "supported": true,
      "errorMessage": null
    },
      {
    "supported": false,
    "errorMessage": null,
    "operationName": "rollback"
    },
    {
      "supported": true,
      "errorMessage": null,
      "operationName": "modify_information"
    },
    {
      "supported": true,
      "errorMessage": null,
      "operationName": "sync"
    }
  ]'
where supported_operations is null;
