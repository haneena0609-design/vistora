FROM eclipse-temurin:17-jdk

WORKDIR /app
COPY VistoraApp.java VistoraApiServer.java ./
RUN javac --add-modules jdk.httpserver VistoraApp.java VistoraApiServer.java

EXPOSE 8080
CMD ["java", "--add-modules", "jdk.httpserver", "VistoraApiServer"]
