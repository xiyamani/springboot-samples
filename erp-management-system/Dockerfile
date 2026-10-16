FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/erp-training-1.0.0.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java","-Dfile.encoding=UTF-8","-jar","app.jar","--server.address=0.0.0.0"]
