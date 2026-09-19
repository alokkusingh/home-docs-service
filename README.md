# Home API Service 
Home Stack API Service

## Build
### Set JAVA_HOME (in case mvn run through terminal)
```shell
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
```
### Build Application image
```shell
protoc --proto_path=src --java_out=src/main/java src/main/resources/proto/getInvestmentsResponse.proto
protoc --proto_path=src --java_out=src/main/java src/main/resources/proto/getInvestmentsRorMetricsResponse.proto
protoc --proto_path=src --java_out=src/main/java src/main/resources/proto/getRawInvestmentsResponse.proto
```
```shell
 docker build --progress=plain -f Dockerfile -t alokkusingh/home-docs-service:latest -t alokkusingh/home-docs-service:1.0.0 .
```
```shell
docker push alokkusingh/home-docs-service:latest
```
```shell
docker push alokkusingh/home-docs-service:1.0.0
```
```shell
docker run -p 8081:8081 --rm --name home-docs-service alokkusingh/home-docs-service
```
### Manual commands - go inside and run binary manually
```shell
docker run -it --entrypoint /bin/bash -p 8082:8081 --rm --name home-docs-service alokkusingh/home-docs-service
```
```shell
./home-docs-service --java.security.egd=file:/dev/urandom --spring.profiles.active=prod --spring.datasource.url=jdbc:mysql://192.168.1.200:32306/home-stack \
--spring.datasource.hikari.minimum-idle=5 --spring.datasource.hikari.connection-timeout=20000 --spring.datasource.hikari.maximum-pool-size=10 \
--spring.datasource.hikari.idle-timeout=10000 --spring.datasource.hikari.max-lifetime=1000 --spring.datasource.hikari.auto-commit=true
```
```shell
docker run -p 8081:8081 --rm --name home-docs-service alokkusingh/home-docs-service --java.security.egd=file:/dev/urandom --spring.profiles.active=prod \
--spring.datasource.url=jdbc:mysql://192.168.1.200:32306/home-stack --spring.datasource.hikari.minimum-idle=5 --spring.datasource.hikari.connection-timeout=20000 \
--spring.datasource.hikari.maximum-pool-size=10 --spring.datasource.hikari.idle-timeout=10000 --spring.datasource.hikari.max-lifetime=1000 \
--spring.datasource.hikari.auto-commit=true
```