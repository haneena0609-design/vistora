# VISTORA Java Backend

This is a console-based Java OOP backend for the VISTORA hotel reservation project.

## Run it

1. Install a JDK (Java 17 or newer).
2. Open a terminal in this folder.
3. Run:

```powershell
javac VistoraApp.java
java VistoraApp
```

`vistora-data.dat` is created automatically after the first run. It stores the rooms and all reservation records permanently.

## OOP concepts demonstrated

- **Abstraction:** `Room` is an abstract superclass.
- **Inheritance:** `StandardRoom`, `DeluxeRoom`, and `Suite` inherit from `Room`.
- **Polymorphism:** each room class calculates its own nightly price through `nightlyRate()`.
- **Encapsulation:** guest and reservation details are kept in controlled model objects.
- **Persistent storage:** `FileDatabase` writes and retrieves data through Java object serialization.
