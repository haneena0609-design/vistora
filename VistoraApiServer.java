import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/** Lightweight REST API that connects the VISTORA HTML frontend to the Java OOP models. */
public class VistoraApiServer {
    private static final VistoraApp.HotelService service = new VistoraApp.HotelService(new VistoraApp.FileDatabase("vistora-data.dat"));

    public static void main(String[] args) throws IOException {
        service.initialise();
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/health", VistoraApiServer::health);
        server.createContext("/api/rooms", VistoraApiServer::rooms);
        server.createContext("/api/reservations", VistoraApiServer::reservations);
        server.setExecutor(null);
        server.start();
        System.out.println("Vistora API running at http://localhost:" + port + "/api/health");
    }

    private static void health(HttpExchange ex) throws IOException { reply(ex, 200, "{\"status\":\"online\",\"service\":\"Vistora Java API\"}"); }

    private static void rooms(HttpExchange ex) throws IOException {
        if (!method(ex, "GET")) return;
        Map<String,String> q = query(ex.getRequestURI().getRawQuery());
        String city = q.getOrDefault("city", "");
        LocalDate in, out;
        try { in=LocalDate.parse(q.getOrDefault("checkIn", "")); out=LocalDate.parse(q.getOrDefault("checkOut", "")); }
        catch (Exception e) { reply(ex,400,"{\"error\":\"Use checkIn and checkOut in YYYY-MM-DD format.\"}"); return; }
        if (!out.isAfter(in)) { reply(ex,400,"{\"error\":\"Check-out must be after check-in.\"}"); return; }
        List<VistoraApp.Room> list = city.isBlank() ? service.getRooms().stream().filter(r -> service.findAvailableRoom(r.getNumber(), r.getCity(), in, out) != null).toList() : service.searchAvailableRooms(city,in,out);
        StringBuilder json=new StringBuilder("{\"rooms\":[");
        for(int i=0;i<list.size();i++){ VistoraApp.Room r=list.get(i); if(i>0)json.append(','); json.append("{\"number\":\"").append(escape(r.getNumber())).append("\",\"hotel\":\"").append(escape(r.getHotelName())).append("\",\"city\":\"").append(escape(r.getCity())).append("\",\"category\":\"").append(escape(r.category())).append("\",\"capacity\":").append(r.getCapacity()).append(",\"nightlyRate\":").append(r.nightlyRate()).append('}'); }
        reply(ex,200,json.append("]}").toString());
    }

    private static void reservations(HttpExchange ex) throws IOException {
        if (!method(ex,"POST")) return;
        Map<String,String> b = flatJson(readBody(ex));
        try {
            String roomNumber=required(b,"roomNumber"), city=required(b,"city");
            LocalDate in=LocalDate.parse(required(b,"checkIn")), out=LocalDate.parse(required(b,"checkOut"));
            VistoraApp.Room room=service.findAvailableRoom(roomNumber,city,in,out);
            if(room==null){reply(ex,409,"{\"error\":\"This room is no longer available for those dates.\"}");return;}
            VistoraApp.Guest guest=new VistoraApp.Guest(required(b,"guestName"),required(b,"email"),b.getOrDefault("phone", ""));
            VistoraApp.Reservation r=service.reserve(guest,room,in,out,Integer.parseInt(b.getOrDefault("guests","1")),Boolean.parseBoolean(b.getOrDefault("extraBed","false")),Boolean.parseBoolean(b.getOrDefault("airportTransfer","false")),b.getOrDefault("requests", ""));
            reply(ex,201,"{\"message\":\"Reservation created\",\"reference\":\""+r.getReference()+"\",\"invoice\":\""+escape(r.invoice())+"\"}");
        } catch (IllegalArgumentException e) { reply(ex,400,"{\"error\":\""+escape(e.getMessage())+"\"}"); }
        catch (Exception e) { reply(ex,400,"{\"error\":\"Invalid reservation data.\"}"); }
    }

    private static boolean method(HttpExchange ex,String expected) throws IOException { if(ex.getRequestMethod().equalsIgnoreCase("OPTIONS")){cors(ex);ex.sendResponseHeaders(204,-1);return false;} if(!ex.getRequestMethod().equals(expected)){reply(ex,405,"{\"error\":\"Method not allowed\"}");return false;}return true; }
    private static void reply(HttpExchange ex,int status,String body) throws IOException { cors(ex);byte[] bytes=body.getBytes(StandardCharsets.UTF_8);ex.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");ex.sendResponseHeaders(status,bytes.length);try(OutputStream os=ex.getResponseBody()){os.write(bytes);} }
    private static void cors(HttpExchange ex){ex.getResponseHeaders().set("Access-Control-Allow-Origin","*");ex.getResponseHeaders().set("Access-Control-Allow-Methods","GET, POST, OPTIONS");ex.getResponseHeaders().set("Access-Control-Allow-Headers","Content-Type");}
    private static String readBody(HttpExchange ex) throws IOException {return new String(ex.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);}
    private static String required(Map<String,String> data,String key){String v=data.get(key);if(v==null||v.isBlank())throw new IllegalArgumentException(key+" is required.");return v;}
    private static Map<String,String> query(String raw){Map<String,String> m=new HashMap<>();if(raw==null)return m;for(String item:raw.split("&")){String[] p=item.split("=",2);if(p.length==2)m.put(java.net.URLDecoder.decode(p[0],StandardCharsets.UTF_8),java.net.URLDecoder.decode(p[1],StandardCharsets.UTF_8));}return m;}
    // Flat JSON is sufficient for frontend form values. A production project should use Jackson/Gson.
    private static Map<String,String> flatJson(String json){Map<String,String> m=new HashMap<>();java.util.regex.Matcher x=java.util.regex.Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*(?:\\\"((?:\\\\.|[^\\\"])*)\\\"|([^,}]+))").matcher(json);while(x.find()){String v=x.group(2)!=null?x.group(2).replace("\\\\\"","\"").replace("\\\\n","\n"):x.group(3).trim();m.put(x.group(1),v);}return m;}
    private static String escape(String text){return text.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","");}
}
