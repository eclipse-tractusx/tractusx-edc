# Sample Multi Tenancy

Build:
```shell
./mvnw -pl samples/sample-multi-tenancy -am package
```

Run:
```shell
java -jar -Dedc.tenants.path=samples/sample-multi-tenancy/tenants.properties samples/sample-multi-tenancy/target/sample-multi-tenancy.jar
```

Create an asset on `first` tenant:
```shell
curl -X POST http://localhost:18181/api/assets \
    --header 'X-Api-Key: password' \
    --header 'Content-Type: application/json' \
    --data '{
             "asset": {
                "properties": {
                        "asset:prop:id": "1",
                        "asset:prop:description": "Product EDC Demo Asset"
                    }
                },
                "dataAddress": {
                    "properties": {
                        "type": "HttpData",
                        "baseUrl": "https://jsonplaceholder.typicode.com/todos/1"
                    }
                }
            }'
```

Get `first` tenant assets:
```
curl -X GET http://localhost:18181/api/assets
[{"createdAt":1666075635217,"properties":{"asset:prop:description":"Product EDC Demo Asset","asset:prop:id":"1"},"id":"1"}]
```

`second` and `third` tenants will have no assets:
```shell
curl -X GET http://localhost:28181/api/assets
[]

curl -X GET http://localhost:38181/api/assets
[]
```
