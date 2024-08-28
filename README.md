# Running eric-eo-evnfm-orchestrator in intellij

## Port forward a few things from the cluster
- postgres 5432 -> 5432
- onboarding 8888 -> 9001
- message bus amqp 5672 -> 5672
- message bus ui 15672 -> 15672

*Note:* Port forward command example
```
kubectl -n <namespace> port-forward <pod name> <localhost port>:<pod port>
```

## Change a couple of things
- Change the spring profile called in `application.yaml` from `test` to `debug`
- Update the `username` & `password` for the message bus in `application-debug.yaml`.
  You can get these by `kubectl --exec...` into the messagebus
  pod and run `env | grep 'RABBITMQ_USERNAME\|RABBITMQ_PASSWORD'`

## Running the application
Go to `ApplicationServer` class, right click and choose `run`