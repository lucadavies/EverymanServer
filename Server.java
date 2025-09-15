import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.json.*;

public class Server
{

    static String resp404 = "<html><body><h1>404: Page not found</h3><a href=\"/\">Return to root</a></body></html>";
    static String yesPlanApiKey = "";
    static String logPath = "server.log";
    static LocalDateTime time;
    static DateTimeFormatter logFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS");
    static DateTimeFormatter apiCallFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    static DateTimeFormatter eventDisplayFormatter = DateTimeFormatter.ofPattern("HH:mm");
    HttpServer server;

    public Server() // throws Exception
    {
        Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);
        System.out.println("Reading audio files...");
        System.out.println("Server booting...");

        try
        {
            server = HttpServer.create(new InetSocketAddress(80), 0);
        }
        catch (IOException e)
        {
            System.out.println("An error occurred creating a server instance:");
            System.out.println(e.getMessage());
            // throw new Exception(e);
        }

        server.createContext("/", new HanRoot());
        server.createContext("/res", new HanRes());
        server.createContext("/events", new HanEvents());
        server.setExecutor(null); // creates a default executor
    }

    public void start()
    {
        server.start();
        System.out.println("Server running...");
    }

    private void CreateEventsPage(ArrayList<JSONObject> events)
    {
        try
        {
            FileWriter writer = null;
            OffsetDateTime time = null;
            String today = LocalDateTime.now().format(apiCallFormatter);

            writer = new FileWriter("events.html");
            writer.append("<!DOCTYPE html>");
            writer.append("\n<html>");
            writer.append("\n  <head>");
            writer.append("\n    <style>");
            writer.append("\n      html * { font-family: Century Gothic; }");
            writer.append("\n      table, th, td { font-size: 24px; border: 1px solid black; border-collapse: collapse; }");
            writer.append("\n      th, td { padding: 15px; }");
            writer.append("\n    </style>");
            writer.append("\n  </head>");
            writer.append("\n  <body>");
            writer.append("\n    <h2>[" + today + "] Today's Events</h2>");
            writer.append("\n    <table>");
            writer.append("\n      <thead>");
            writer.append("\n        <tr>");
            writer.append("\n          <th>Name</th>");
            writer.append("\n          <th>Start</th>");
            writer.append("\n          <th>End</th>");
            writer.append("\n          <th>Location</th>");
            writer.append("\n        </tr>");
            writer.append("\n      </thead>");
            writer.append("\n      <tbody>");

            for (JSONObject event : events)
            {
                String startTime = OffsetDateTime.parse(event.getString("starttime"), DateTimeFormatter.ISO_OFFSET_DATE_TIME).format(eventDisplayFormatter);
                String endTime = OffsetDateTime.parse(event.getString("endtime"), DateTimeFormatter.ISO_OFFSET_DATE_TIME).format(eventDisplayFormatter);

                writer.append("\n        <tr>");
                writer.append("\n          <td>" + event.getString("name") + "</td>");
                writer.append("\n          <td>" + startTime + "</td>");
                writer.append("\n          <td>" + endTime + "</td>");
                writer.append("\n          <td>" + event.getJSONArray("locations").getJSONObject(0).getString("name") + "</td>");
                writer.append("\n        </tr>");
            }

            writer.append("\n      </tbody>");
            writer.append("\n    </table>");
            writer.append("\n  </body>");
            writer.append("\n</html>");

            if (writer != null)
            {
                writer.close();
            }
        }
        catch (IOException e)
        {
            System.out.println("Error creating events page.");
            System.out.println(e.getMessage());
        }
    }

    private JSONObject getEvents()
    {
        LocalDateTime today = LocalDateTime.now();
        String apiCall = "https://everymantheatre.yesplan.be/api/events/date:" + today.format(apiCallFormatter) + "?api_key=" + yesPlanApiKey;

        String s = "Getting events for today (" + today.format(apiCallFormatter) + ")";
        System.out.println(s);

        URL url = null;
        JSONObject apiResp = null;
        try
        {
            url = new URI(apiCall).toURL();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8")))
            {
                apiResp = new JSONObject(reader.readLine());
            }
            catch (IOException e)
            {
                System.out.println("Error opening stream to read HTTP response.");
                System.out.println(e.getMessage());
            }
        }
        catch (MalformedURLException e)
        {
            System.out.println("Could not create URL from URI from string: " + apiCall);
            System.out.println(e.getMessage());
        }
        catch (URISyntaxException e)
        {
            System.out.println("URI " + apiCall + " has invalid syntax.");
            System.out.println(e.getMessage());
        }

        return apiResp;
    }

    private JSONObject getLatestEvent()
    {
        JSONObject apiResp = getEvents();

        if (apiResp == null)
        {
            return null;
        }

        JSONArray events = apiResp.getJSONArray("data");
        // ArrayList<OffsetDateTime> dateTimes = new ArrayList<OffsetDateTime>();
        // JSONObject event = null;

        // for (int i = 0; i < events.length() - 1; i++)
        // {
        // event = (JSONObject) events.get(i);
        // dateTimes.add(OffsetDateTime.parse(event.getString("starttime"),
        // DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        // }

        // System.out.println(Collections.min(dateTimes));

        return events.getJSONObject(0);
    }

    private ArrayList<JSONObject> getEventsToday()
    {
        ArrayList<JSONObject> events = new ArrayList<>();
        JSONObject apiResp = getEvents();

        if (apiResp == null)
        {
            return events;
        }

        JSONArray jsonEvents = apiResp.getJSONArray("data");

        for (int i = 0; i < jsonEvents.length(); i++)
        {
            events.add(jsonEvents.getJSONObject(i));
        }

        return events;

    }
    // #region Helper Methods

    private byte[] getHTML(String name) throws IOException
    {
        byte[] html = Files.readAllBytes(new File(name).toPath());
        return html;
    }

    private byte[] getFile(String filename)
    {
        byte[] data = null;
        try
        {
            data = Files.readAllBytes(new File(filename).toPath());
        }
        catch (IOException e)
        {
            System.out.println("File \"" + filename + "\" cannot be found.");
            System.out.println(e.getMessage());
        }
        return data;
    }

    private void log(String s)
    {
        time = LocalDateTime.now();
        try
        {
            PrintWriter l = new PrintWriter(new FileOutputStream(new File(logPath), true));
            l.append("[" + time.format(logFormatter) + "]: " + s + "\n");
            l.close();
        }
        catch (IOException e)
        {
            System.out.println("Error writing to log.");
            System.out.println("Failed to log: \"" + s + "\"");
        }

    }

    private HashMap<String, String> parseForm(String data)
    {
        HashMap<String, String> form = new HashMap<String, String>();
        int index = 0;
        int prevIndex;
        while (true)
        {
            prevIndex = index;
            index = data.indexOf("form-data; name=\"", index) + 17; // start of field name
            if (index != -1 && index > prevIndex)
            {
                int start = index;
                int end = data.indexOf("\"", start); // end of field name
                String key = data.substring(start, end);
                start = end + 3; // end of field name: start of data (less leading \n\n)
                end = data.indexOf("------WebKit", end) - 1; // end of data (less trailing \n);
                String value = data.substring(start, end);
                form.put(key, value);
                index = end;
            } else
            {
                break;
            }
        }
        return form;
    }

    // #endregion

    // #region Route Handlers

    private class HanRoot implements HttpHandler
    {
        HttpExchange t;

        public void handle(HttpExchange httpEx) throws IOException
        {
            this.t = httpEx;
            String method = t.getRequestMethod();
            String decodedURI = URLDecoder.decode(t.getRequestURI().toString(), StandardCharsets.UTF_8.name());
            String headers = "";
            for (Map.Entry<String, List<String>> h : t.getRequestHeaders().entrySet())
            {
                headers += (h + "\n");
            }
            String rHeaders = "";
            for (Map.Entry<String, List<String>> h : t.getResponseHeaders().entrySet())
            {
                rHeaders += (h + "\n");
            }

            if (method.equals("GET"))
            {
                String s = t.getRemoteAddress() + "| GET: " + decodedURI + " (HanRoot)";
                log(s);
                System.out.println(s);
                get(decodedURI);
            } else if (method.equals("POST"))
            {
                String s = t.getRemoteAddress() + "| POST: " + decodedURI + " (HanRoot)";
                log(s);
                System.out.println(s);
                post(decodedURI, null);

            }
            t.close();
        }

        private void get(String uri) throws IOException
        {
            byte[] resp;
            if (uri.matches("/$"))
            {
                resp = getHTML("index.html");
                t.getResponseHeaders().set("content-type", "text/html");
                t.sendResponseHeaders(200, resp.length);
                t.getResponseBody().write(resp);
            } else if (uri.matches("/favicon.ico"))
            {
                resp = getFile("favicon.ico");
                t.getResponseHeaders().set("content-type", "attachment");
                t.sendResponseHeaders(200, resp.length);
                t.getResponseBody().write(resp);
            } else if (uri.matches("/events.html"))
            {
                resp = getFile("events.html");
                t.getResponseHeaders().set("content-type", "text/html");
                t.sendResponseHeaders(200, resp.length);
                t.getResponseBody().write(resp);
            } else if (uri.matches("/refresh"))
            {
                resp = "false".getBytes();
                t.getResponseHeaders().set("content-type", "attachment");
                t.sendResponseHeaders(200, resp.length);
                t.getResponseBody().write(resp);
            } else if (uri.matches("/transfer"))
            {
                resp = getFile("transfer.file");
                t.getResponseHeaders().set("content-type", "attachment");
                t.sendResponseHeaders(200, resp.length);
                t.getResponseBody().write(resp);
            } else
            {
                resp = resp404.getBytes();
                t.getResponseHeaders().set("content-type", "text/html");
                t.sendResponseHeaders(404, resp.length);
                t.getResponseBody().write(resp);
            }
        }

        private void post(String uri, byte[] payload) throws IOException
        {
            if (uri.matches("/$"))
            {
                // hate this but Java 8 affords not many better ways
                String data = new BufferedReader(new InputStreamReader(t.getRequestBody())).lines()
                        .collect(Collectors.joining("\n"));
                HashMap<String, String> form = parseForm(data);
                if (form.keySet().contains("cat"))
                {

                }
                byte[] resp = getHTML("index.html");
                t.getResponseHeaders().set("content-type", "text/html");
                t.sendResponseHeaders(200, resp.length);
                t.getResponseBody().write(resp);
            }
        }
    }

    private class HanRes implements HttpHandler
    {
        HttpExchange t;

        public void handle(HttpExchange httpEx) throws IOException
        {
            this.t = httpEx;
            String method = t.getRequestMethod();
            String decodedURI = URLDecoder.decode(t.getRequestURI().toString(), StandardCharsets.UTF_8.name());

            if (method.equals("GET"))
            {
                String s = t.getRemoteAddress() + "| GET: " + decodedURI + " (HanRes)";
                log(s);
                System.out.println(s);
                get(decodedURI);
            }
            t.close();
        }

        private void get(String uri) throws IOException
        {
            byte[] resp = getFile(uri.substring(1));
            if (resp != null)
            {
                t.getResponseHeaders().set("content-type", "attachment");
                t.sendResponseHeaders(200, resp.length);
                t.getResponseBody().write(resp);
            } else
            {
                resp = resp404.getBytes();
                t.getResponseHeaders().set("content-type", "text/html");
                t.sendResponseHeaders(404, resp.length);
                t.getResponseBody().write(resp);
            }
        }
    }

    private class HanEvents implements HttpHandler
    {
        HttpExchange t;

        public void handle(HttpExchange httpEx) throws IOException
        {
            CreateEventsPage(getEventsToday());

            this.t = httpEx;
            String method = t.getRequestMethod();
            String decodedURI = URLDecoder.decode(t.getRequestURI().toString(), StandardCharsets.UTF_8.name());

            if (method.equals("GET"))
            {
                String s = t.getRemoteAddress() + "| GET: " + decodedURI + " (HanEvents)";
                log(s);
                System.out.println(s);
                get(decodedURI);
            }
            t.close();
        }

        private void get(String uri) throws IOException
        {
            byte[] resp = getFile(uri.substring(1));
            if (resp != null)
            {
                t.getResponseHeaders().set("content-type", "attachment");
                t.sendResponseHeaders(200, resp.length);
                t.getResponseBody().write(resp);
            } else
            {
                resp = resp404.getBytes();
                t.getResponseHeaders().set("content-type", "text/html");
                t.sendResponseHeaders(404, resp.length);
                t.getResponseBody().write(resp);
            }
        }
    }

    // #endregion

    public static void main(String[] args) throws Exception
    {
        Server server = new Server();
        server.start();
    }

}
