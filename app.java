import java.io.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * VISTORA Hotel Management and Room Reservation System.
 * Demonstrates abstraction, inheritance, encapsulation and polymorphism.
 * Data is stored permanently in a local file named vistora-data.dat.
 */
public class VistoraApp {
    private static final Scanner INPUT = new Scanner(System.in);
    private static final HotelService service = new HotelService(new FileDatabase("vistora-data.dat"));

    public static void main(String[] args) {
        service.initialise();
        System.out.println("\n══════════════════════════════════════");
        System.out.println("        VISTORA HOTEL MANAGEMENT");
        System.out.println("══════════════════════════════════════");

        boolean running = true;
        while (running) {
            System.out.println("\n1. Search available rooms\n2. Make reservation\n3. View reservation\n4. Cancel reservation\n5. View hotel records\n0. Exit");
            switch (readInt("Choose an option: ")) {
                case 1 -> searchRooms();
                case 2 -> createReservation();
                case 3 -> viewReservation();
                case 4 -> cancelReservation();
                case 5 -> viewRecords();
                case 0 -> { service.save(); running = false; System.out.println("Thank you for choosing Vistora."); }
                default -> System.out.println("Please choose a valid option.");
            }
        }
    }

    private static void searchRooms() {
        LocalDate checkIn = readDate("Check-in date (YYYY-MM-DD): ");
        LocalDate checkOut = readDate("Check-out date (YYYY-MM-DD): ");
        if (!validDates(checkIn, checkOut)) return;
        String city = readLine("City (Mumbai / Shimla / Goa / Jaipur): ");
        List<Room> available = service.searchAvailableRooms(city, checkIn, checkOut);
        if (available.isEmpty()) { System.out.println("No rooms are available for those details."); return; }
        printRooms(available, checkIn, checkOut);
    }

    private static void createReservation() {
        System.out.println("\n--- Guest information ---");
        String name = readLine("Full name: ");
        String email = readLine("Email: ");
        String phone = readLine("Phone number: ");
        LocalDate checkIn = readDate("Check-in date (YYYY-MM-DD): ");
        LocalDate checkOut = readDate("Check-out date (YYYY-MM-DD): ");
        if (!validDates(checkIn, checkOut)) return;
        String city = readLine("City: ");
        List<Room> available = service.searchAvailableRooms(city, checkIn, checkOut);
        if (available.isEmpty()) { System.out.println("No rooms are available for these dates."); return; }
        printRooms(available, checkIn, checkOut);
        String roomNumber = readLine("Enter room number: ");
        Room room = service.findAvailableRoom(roomNumber, city, checkIn, checkOut);
        if (room == null) { System.out.println("That room is not available."); return; }

        int guests = readInt("Number of guests: ");
        boolean extraBed = yesNo("Extra bed required? (y/n): ");
        boolean airportTransfer = yesNo("Airport transfer required? (y/n): ");
        String requests = readLine("Special requests (or press Enter): ");
        try {
            Reservation reservation = service.reserve(new Guest(name, email, phone), room, checkIn, checkOut,
                    guests, extraBed, airportTransfer, requests);
            System.out.println("\nReservation confirmed!");
            System.out.println(reservation.invoice());
        } catch (IllegalArgumentException e) { System.out.println("Could not create reservation: " + e.getMessage()); }
    }

    private static void viewReservation() {
        String reference = readLine("Reservation reference: ");
        Reservation reservation = service.findReservation(reference);
        System.out.println(reservation == null ? "No reservation found." : reservation.invoice());
    }

    private static void cancelReservation() {
        String reference = readLine("Reservation reference: ");
        System.out.println(service.cancel(reference) ? "Reservation cancelled." : "No active reservation found.");
    }

    private static void viewRecords() {
        System.out.println("\n--- Vistora hotel records ---");
        System.out.printf("Rooms: %d | Active reservations: %d%n", service.roomCount(), service.activeReservationCount());
        for (Reservation reservation : service.getReservations()) System.out.println(reservation.summary());
    }

    private static void printRooms(List<Room> rooms, LocalDate in, LocalDate out) {
        long nights = ChronoUnit.DAYS.between(in, out);
        System.out.printf("\nAvailable rooms for %d night(s):%n", nights);
        for (Room room : rooms) System.out.printf("%s | %s | ₹%,.0f total before extras%n", room, room.baseCost(nights));
    }
    private static boolean validDates(LocalDate in, LocalDate out) { if (!out.isAfter(in)) { System.out.println("Check-out must be after check-in."); return false; } return true; }
    private static String readLine(String prompt) { System.out.print(prompt); return INPUT.nextLine().trim(); }
    private static int readInt(String prompt) { while (true) try { return Integer.parseInt(readLine(prompt)); } catch (NumberFormatException e) { System.out.println("Enter a whole number."); } }
    private static LocalDate readDate(String prompt) { while (true) try { return LocalDate.parse(readLine(prompt)); } catch (Exception e) { System.out.println("Use YYYY-MM-DD."); } }
    private static boolean yesNo(String prompt) { return readLine(prompt).equalsIgnoreCase("y"); }

    // Abstraction: every room has common state but its own nightly price.
    static abstract class Room implements Serializable {
        private final String number, hotelName, city;
        private final int capacity;
        protected Room(String number, String hotelName, String city, int capacity) { this.number = number; this.hotelName = hotelName; this.city = city; this.capacity = capacity; }
        public String getNumber() { return number; }
        public String getHotelName() { return hotelName; }
        public String getCity() { return city; }
        public int getCapacity() { return capacity; }
        public abstract double nightlyRate();
        public abstract String category();
        public double baseCost(long nights) { return nightlyRate() * nights; }
        @Override public String toString() { return number + " | " + hotelName + ", " + city + " | " + category() + " | sleeps " + capacity + " | ₹" + String.format("%,.0f", nightlyRate()) + "/night"; }
    }
    static final class StandardRoom extends Room { StandardRoom(String n, String h, String c) { super(n,h,c,2); } public double nightlyRate() { return 4500; } public String category() { return "Standard Room"; } }
    static final class DeluxeRoom extends Room { DeluxeRoom(String n, String h, String c) { super(n,h,c,3); } public double nightlyRate() { return 7200; } public String category() { return "Deluxe Room"; } }
    static final class Suite extends Room { Suite(String n, String h, String c) { super(n,h,c,4); } public double nightlyRate() { return 12500; } public String category() { return "Vistora Suite"; } }

    // Encapsulation: guest details can only be read through this controlled model.
    static final class Guest implements Serializable {
        private final String name, email, phone;
        Guest(String name, String email, String phone) { if (name.isBlank() || email.isBlank()) throw new IllegalArgumentException("Name and email are required."); this.name=name; this.email=email; this.phone=phone; }
        public String getName() { return name; } public String getEmail() { return email; } public String getPhone() { return phone; }
    }

    static final class Reservation implements Serializable {
        private final String reference; private final Guest guest; private final Room room; private final LocalDate checkIn, checkOut;
        private final int guests; private final boolean extraBed, airportTransfer; private final String requests; private boolean cancelled;
        Reservation(Guest g, Room r, LocalDate in, LocalDate out, int guests, boolean bed, boolean transfer, String requests) {
            if (guests < 1 || guests > r.getCapacity()) throw new IllegalArgumentException("Guest count exceeds room capacity.");
            this.reference="VST-" + UUID.randomUUID().toString().substring(0,6).toUpperCase(); this.guest=g; this.room=r; this.checkIn=in; this.checkOut=out; this.guests=guests; this.extraBed=bed; this.airportTransfer=transfer; this.requests=requests; }
        public String getReference() { return reference; } public Room getRoom() { return room; } public boolean isCancelled() { return cancelled; }
        public boolean overlaps(LocalDate in, LocalDate out) { return !cancelled && in.isBefore(checkOut) && out.isAfter(checkIn); }
        public void cancel() { cancelled=true; }
        private long nights() { return ChronoUnit.DAYS.between(checkIn,checkOut); }
        private double extras() { return (extraBed ? 1200*nights() : 0) + (airportTransfer ? 1800 : 0); }
        private double tax() { return (room.baseCost(nights()) + extras()) * .12; }
        public String invoice() { double base=room.baseCost(nights()), total=base+extras()+tax(); return String.format("%n──────── VISTORA INVOICE ────────%nReference: %s%nGuest: %s%nHotel: %s%nRoom: %s%nStay: %s to %s (%d night(s))%nGuests: %d%nRoom charge: ₹%,.0f%nExtras: ₹%,.0f%nTax (12%%): ₹%,.0f%nTOTAL: ₹%,.0f%nStatus: %s%nRequests: %s%n─────────────────────────────────",reference,guest.getName(),room.getHotelName(),room.category(),checkIn,checkOut,nights(),guests,base,extras(),tax(),total,cancelled?"Cancelled":"Confirmed",requests.isBlank()?"None":requests); }
        public String summary() { return reference + " | " + guest.getName() + " | " + room.getHotelName() + " | " + checkIn + " to " + checkOut + " | " + (cancelled?"Cancelled":"Confirmed"); }
    }

    static final class HotelService {
        private final FileDatabase database; private List<Room> rooms = new ArrayList<>(); private List<Reservation> reservations = new ArrayList<>();
        HotelService(FileDatabase database) { this.database=database; }
        void initialise() { HotelData saved=database.load(); if (saved != null) { rooms=saved.rooms; reservations=saved.reservations; } else { seedRooms(); save(); } }
        private void seedRooms() { String[][] hotels={{"Vistora Grand","Mumbai","M"},{"Vistora Hills","Shimla","S"},{"Vistora Coast","Goa","G"},{"Vistora Haveli","Jaipur","J"}}; for(String[] h:hotels){ rooms.add(new StandardRoom(h[2]+"-101",h[0],h[1])); rooms.add(new DeluxeRoom(h[2]+"-201",h[0],h[1])); rooms.add(new Suite(h[2]+"-301",h[0],h[1])); } }
        List<Room> searchAvailableRooms(String city, LocalDate in, LocalDate out) { return rooms.stream().filter(r->r.getCity().equalsIgnoreCase(city.trim())).filter(r->findAvailableRoom(r.getNumber(),city,in,out)!=null).toList(); }
        Room findAvailableRoom(String number,String city,LocalDate in,LocalDate out) { return rooms.stream().filter(r->r.getNumber().equalsIgnoreCase(number)&&r.getCity().equalsIgnoreCase(city.trim())).filter(r->reservations.stream().noneMatch(x->x.getRoom().getNumber().equalsIgnoreCase(r.getNumber())&&x.overlaps(in,out))).findFirst().orElse(null); }
        Reservation reserve(Guest guest,Room room,LocalDate in,LocalDate out,int guests,boolean bed,boolean transfer,String requests) { if(findAvailableRoom(room.getNumber(),room.getCity(),in,out)==null) throw new IllegalArgumentException("Room is no longer available."); Reservation r=new Reservation(guest,room,in,out,guests,bed,transfer,requests);reservations.add(r);save();return r; }
        Reservation findReservation(String reference) { return reservations.stream().filter(r->r.getReference().equalsIgnoreCase(reference)).findFirst().orElse(null); }
        boolean cancel(String reference) { Reservation r=findReservation(reference);if(r==null||r.isCancelled())return false;r.cancel();save();return true; }
        int roomCount(){return rooms.size();} int activeReservationCount(){return (int)reservations.stream().filter(r->!r.isCancelled()).count();} List<Reservation> getReservations(){return List.copyOf(reservations);} void save(){database.save(new HotelData(rooms,reservations));}
    }
    static final class HotelData implements Serializable { final List<Room> rooms; final List<Reservation> reservations; HotelData(List<Room> rooms,List<Reservation> reservations){this.rooms=rooms;this.reservations=reservations;} }
    static final class FileDatabase {
        private final String fileName; FileDatabase(String fileName){this.fileName=fileName;}
        void save(HotelData data){try(ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(fileName))){out.writeObject(data);}catch(IOException e){System.out.println("Warning: data could not be saved: "+e.getMessage());}}
        HotelData load(){File file=new File(fileName);if(!file.exists())return null;try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(file))){return (HotelData)in.readObject();}catch(IOException|ClassNotFoundException e){System.out.println("Saved data could not be read; starting clean.");return null;}}
    }
}
