# es-graduation-project
Elasticsearch capstone project from  GridU course ['Software engineer to Search engineer'](https://learning.griddynamics.com/#/online-courses/5f89cc97-defe-4d9e-83e0-3d536eae6510)


## Tool versions
- Java 17
- Spring boot 2.5.7
- Elasticsearch / Kibana - 7.17.28 (ports 9200 / 5601)
- JUnit 4.12
>- Docker compose file: [elasticsearch-kibana-compose.yaml ](elasticsearch-kibana-compose.yaml)

## Quick Start

```shell
 docker compose -f elasticsearch-kibana-compose.yaml up
```

> Open [Kibana](http://localhost:5601/)
> 
> Check [Elasticsearch](http://localhost:9200/)

### Build the project
```shell
 mvn clean package
```

### Run app (with indexing)

```shell
 java -jar target/es-graduation-project-1.0.jar recreateIndex
```
or
```shell
 mvn spring-boot:run -Dspring-boot.run.arguments="recreateIndex"
```
