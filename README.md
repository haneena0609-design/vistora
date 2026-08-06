# VISTORA Java Backend and API

This is a Java OOP backend for the VISTORA hotel reservation project. It includes a console application and a small REST API that the HTML frontend can call.

## Run the console application

1. Install a JDK (Java 17 or newer).
2. Open a terminal in this folder.
3. Run:

```powershell
javac VistoraApp.java
java VistoraApp
```

`vistora-data.dat` is created automatically after the first run. It stores the rooms and all reservation records permanently.

## Connect it to the website locally

1. Make sure `VistoraApp.java` and `VistoraApiServer.java` are in the same folder.
2. Run the API:

```powershell
javac --add-modules jdk.httpserver VistoraApp.java VistoraApiServer.java
java --add-modules jdk.httpserver VistoraApiServer
```

3. Keep that terminal running. The API is then available at `http://localhost:8080/api`.
4. Open `index.html` in the browser, make a reservation, and it will be saved by Java into `vistora-data.dat`.

For the Vercel website, deploy this backend separately to a Java-capable host. Then update `VISTORA_API_URL` near the bottom of `index.html` from `http://localhost:8080/api` to your hosted URL followed by `/api`.

## OOP concepts demonstrated

- **Abstraction:** `Room` is an abstract superclass.
- **Inheritance:** `StandardRoom`, `DeluxeRoom`, and `Suite` inherit from `Room`.
- **Polymorphism:** each room class calculates its own nightly price through `nightlyRate()`.
- **Encapsulation:** guest and reservation details are kept in controlled model objects.
- **Persistent storage:** `FileDatabase` writes and retrieves data through Java object serialization.
