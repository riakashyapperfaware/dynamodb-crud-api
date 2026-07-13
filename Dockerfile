FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/dynamodb-crud-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "app.jar", "com.dynamo.api.DynamodbCrudApiApplication"]
