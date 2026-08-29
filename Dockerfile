FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY VistoraApiServer.java .

RUN javac --add-modules jdk.httpserver VistoraApiServer.java

EXPOSE 8080

CMD ["java", "--add-modules", "jdk.httpserver", "VistoraApiServer"]
