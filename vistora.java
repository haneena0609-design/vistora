import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Guest {
    String name;
    String email;
    String phone;

    Guest(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }
}

abstract class Room {
    String roomType;

    Room(String roomType) {
        this.roomType = roomType;
    }

    abstract double getPricePerNight();

    double calculateRoomCost(long nights) {
        return getPricePerNight() * nights;
    }
}

class StandardRoom extends Room {
    StandardRoom() {
        super("Standard Room");
    }

    double getPricePerNight() {
        return 4500;
    }
}

class DeluxeRoom extends Room {
    DeluxeRoom() {
        super("Deluxe Room");
    }

    double getPricePerNight() {
        return 7200;
    }
}

class Suite extends Room {
    Suite() {
        super("Vistora Suite");
    }

    double getPricePerNight() {
        return 12500;
    }
}

class Reservation {
    Guest guest;
    Room room;
    String hotel;
    String city;
    LocalDate checkIn;
    LocalDate checkOut;
    boolean extraBed;
    String reference;

    Reservation(Guest guest, Room room, String hotel, String city,
                LocalDate checkIn, LocalDate checkOut, boolean extraBed) {
        this.guest = guest;
        this.room = room;
        this.hotel = hotel;
        this.city = city;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.extraBed = extraBed;
        this.reference = "VST-" +
                UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    long calculateNights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    double calculateExtraBedCost() {
        return extraBed ? 1200 * calculateNights() : 0;
    }

    double calculateTax() {
        return (room.calculateRoomCost(calculateNights())
                + calculateExtraBedCost()) * 0.12;
    }

    double calculateTotal() {
        return room.calculateRoomCost(calculateNights())
                + calculateExtraBedCost()
                + calculateTax();
    }

    void saveReservation() throws IOException {
        FileWriter file = new FileWriter("vistora-reservations.txt", true);

        file.write(
            "Reference: " + reference +
            " | Guest: " + guest.name +
            " | Hotel: " + hotel +
            " | City: " + city +
            " | Room: " + room.roomType +
            " | Check-in: " + checkIn +
            " | Check-out: " + checkOut +
            " | Total: Rs. " + calculateTotal() +
            "\n"
        );

        file.close();
    }
}

public class VistoraApiServer {
    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(
            System.getenv().getOrDefault("PORT", "8080")
        );

        HttpServer server = HttpServer.create(
            new InetSocketAddress(port), 0
        );

        server.createContext("/", VistoraApiServer::home);
        server.createContext("/api/health", VistoraApiServer::health);
        server.createContext("/api/reservations",
                VistoraApiServer::createReservation);

        server.start();

        System.out.println(
            "Vistora API running on http://localhost:" + port
        );
    }

    static void home(HttpExchange exchange) throws IOException {
        sendResponse(exchange, 200,
            "{\"message\":\"Vistora Java API is online\"}");
    }

    static void health(HttpExchange exchange) throws IOException {
        sendResponse(exchange, 200,
            "{\"status\":\"online\",\"service\":\"Vistora Java API\"}");
    }

    static void createReservation(HttpExchange exchange)
            throws IOException {

        if (exchange.getRequestMethod().equals("OPTIONS")) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!exchange.getRequestMethod().equals("POST")) {
            sendResponse(exchange, 405,
                "{\"error\":\"Only POST requests are allowed\"}");
            return;
        }

        try {
            String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
            );

            Map<String, String> data = readJson(body);

            Guest guest = new Guest(
                data.get("guestName"),
                data.get("email"),
                data.get("phone")
            );

            Room room;

            if (data.get("roomType").equals("Standard Room")) {
                room = new StandardRoom();
            } else if (data.get("roomType").equals("Vistora Suite")) {
                room = new Suite();
            } else {
                room = new DeluxeRoom();
            }

            LocalDate checkIn = LocalDate.parse(data.get("checkIn"));
            LocalDate checkOut = LocalDate.parse(data.get("checkOut"));

            if (!checkOut.isAfter(checkIn)) {
                sendResponse(exchange, 400,
                    "{\"error\":\"Check-out must be after check-in.\"}");
                return;
            }

            Reservation reservation = new Reservation(
                guest,
                room,
                data.get("hotel"),
                data.get("city"),
                checkIn,
                checkOut,
                Boolean.parseBoolean(data.get("extraBed"))
            );

            reservation.saveReservation();

            String response =
                "{\"message\":\"Reservation saved successfully\"," +
                "\"reference\":\"" + reservation.reference + "\"," +
                "\"total\":" + reservation.calculateTotal() + "}";

            sendResponse(exchange, 201, response);

        } catch (Exception error) {
            sendResponse(exchange, 400,
                "{\"error\":\"Please enter valid reservation details.\"}");
        }
    }

    static Map<String, String> readJson(String json) {
        Map<String, String> data = new HashMap<>();

        Pattern pattern = Pattern.compile(
            "\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\""
        );

        Matcher matcher = pattern.matcher(json);

        while (matcher.find()) {
            data.put(matcher.group(1), matcher.group(2));
        }

        return data;
    }

    static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set(
            "Access-Control-Allow-Origin", "*"
        );
        exchange.getResponseHeaders().set(
            "Access-Control-Allow-Methods", "POST, GET, OPTIONS"
        );
        exchange.getResponseHeaders().set(
            "Access-Control-Allow-Headers", "Content-Type"
        );
    }

    static void sendResponse(HttpExchange exchange, int status,
                             String response) throws IOException {
        addCorsHeaders(exchange);

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
            "Content-Type", "application/json"
        );

        exchange.sendResponseHeaders(status, bytes.length);

        OutputStream output = exchange.getResponseBody();
        output.write(bytes);
        output.close();
    }
}