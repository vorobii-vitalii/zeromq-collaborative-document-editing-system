# Collaborative document editing system

## Technology stack of backend system:
1. Java 21
2. ZeroMQ
3. FlatBuffer
4. Reactor Netty
5. Testcontainers
6. Mongo database
7. ELK stack + APM (for metrics, traces etc)
8. JUnit, Mockito, AssertJ for unit tests
9. Dagger (for dependency injection)
10. Docker/Docker-Compose for local deployment
11. Micrometer

## Technology stack of web client
1. Typescript
2. React
3. Jest
4. FlatBuffer
5. diff-match-patch (efficient library by Google for text comparison)

### Start backend system locally:
```shell
./graldlew build
docker-compose up --build
```

### Start web client (application is available on port 3000)
```shell
cd web-client
npm start
```

### If replica set is not initialized, do it manually by executing these commands
```shell
docker exec -it $mongoContainerId bash
mongosh --port=30001
rs.initiate()
```

### Generate FlatBuffer schema classes for Web application
```shell
cd /home/vitaliivorobii/workspace/zeromq-document-editing-system/web-client/src
 ~/flatbuffers/flatc --ts ../../document-server/src/main/flatbuffers/document_server.fbs
```

### Kibana
http://localhost:5601/

