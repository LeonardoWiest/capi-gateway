# CAPI Gateway
###### Light API Gateway implemented with Apache Camel

## Supports:
* Easy deployment of Swagger and Open API endpoints.
* Authorization (for the moment only supporting Auth0).
* Easy to block your API after N failed attempts. (you can define blocking strategies)
* REST endpoint to manage your API's.
* Automatically creates metrics for Prometheus (Verb granularity)
* Automatically creates zipkin traceability.
* Automatically creates a Grafana dashboard with panels per verb (only count total for the moment)
* Throttling (You can apply per API or per API Path)

## What we want to support:
* Key Manager (still researching over the best option).
* Decent UI for management.
* Provide tools for easy installation 

## What we use:
* Apache Camel
* Spring boot
* Hazelcast distributed cache
* Prometheus
* Grafana
* Zipkin
* Mongo

## Example of an API definition

    {
        "_id" : "02c04c49-3304-4dec-b5d4-446327f688f0",
        "endpoint" : "localhost:8080",
        "endpointType" : "HTTP",
        "name" : "YOUR-API",
        "secured" : true,
        "jwsEndpoint" : "https://youruser.auth0.com/.well-known/jwks.json",
        "context" : "your-api",
        "swagger" : true,
        "swaggerEndpoint" : "http://localhost:8080/v2/api-docs",
        "audience" : [ 
            "your-audience"
        ],
        "blockIfInError" : true,
        "maxAllowedFailedCalls" : 10,
        "unblockAfter" : true,
        "unblockAfterMinutes" : 2,
        "throttlingPolicy" : {
            "maxCallsAllowed" : "100",
            "periodForMaxCalls" : "60000",
            "applyPerPath" : true
        }
    }

With the following configuration your service will be available at: http://localhost:8380/gateway/your-api/

The following configuration will be applied:
* secured: true - Meaning, that the CAPI Gateway expects a Bearer token sign by your account at auth0 with your audience in the token claims.
* blockIfInError: true - Means that for instance if you send more than 10 times (maxAllowedFailedCalls) the wrong token your API will be suspended for 2 minutes (unblockAfterMinutes).
* throttlingPolicy.maxCallsAllowed: 100 / throttlingPolicy.periodForMaxCalls - You can only call your API/Path 100 times per minute.
* throttlingPolicy.applyPerPath: true - If true the policy will be applied by path and NOT the total amount for the API.

You can define your own paths, in case you dont have a Swagger Endpoint (Swagger 2/Open API), so if swagger: false, then CAPI will look for a list of PATH like the below example:

    {
        "_id" : "91ab7422-7d37-454b-9b33-6f3e345c8b66",
        "endpoint" : "localhost:8080",
        "endpointType" : "HTTPS",
        "name" : "YOUR-CUSTOM-API",
        "secured" : false,
        "context" : "your-custom-api",
        "blockIfInError" : false
        "paths" : [ 
            {
            "verb" : "GET",
            "path" : "/services/path"
            },
            {
            "verb" : "POST",
            "path" : "/services/path"
            }
        ],
        "swagger" : false
    }

## Play with CAPI Gateway
* Clone the project or just copy the docker-compose file.
* Execute
    $ sudo docker-compose up -d
* Go to: http://localhost:8080/swagger-ui.html
* Publish your first API: 
    curl -X POST "http://localhost:8080/route/simple-rest" -H "accept: application/json" -H "Content-Type: application/json" -d "<your-api>" (see Example of an API definition)
* Imagine that your context was: test and one of your GET path was /user you can then test: http://localhost:8380/gateway/test/user

Docker compose will create instances of Grafana, Prometheus and Zipkin, but if you wish to use already existing instances you just need to change this environment variables:

* api.gateway.prometheus.endpoint=http://prometheus:9090
* api.gateway.zipkin.endpoint=http://zipkin:9411/api/v2/spans
* api.gateway.grafana.endpoint=http://localhost:8080/grafana


## Installation

    sudo docker run -it --rm --name capi-gateway-build -v "$PWD":/usr/src/capi -v "$HOME/.m2":/root/.m2 -v /root/.docker:/root/.docker -w /usr/src/capi maven:3.3-jdk-8 mvn compile -DskipTests jib:build

## Run it

    sudo docker-compose up -d    