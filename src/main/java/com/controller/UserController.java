package com.controller;

import com.app.DatabaseController;
import com.entities.Event;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.api.services.calendar.model.Events;
import com.entities.User;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class UserController {
    private static final String CLIENT_ID = "154954789740-8cn2jar3k6hj2726kla6bclqgnoo863s.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "KNW7di1KWucBWP2xc7ZfJmsn";
    private byte[] salt = "petter".getBytes();
    private boolean loggedIn = false;
    private String URL = "http://juuffy.net:8080";

    public UserController() {
    }

    @RequestMapping(value = "/adduser")
    public User adduser(@RequestParam("name") String name, @RequestParam(value = "pw", defaultValue = "1337") String pw, @RequestParam(value = "mail") String mail, List<Event> events) {
        User user = new User(name, null, createPw(pw), mail, events);
        String dbPath = "users/" + user.getMail().replace(".", ",");
        DatabaseController.getInstance().sendToDb(user, dbPath);
        return user;
    }

    public String createPw(String pw) {
        KeySpec spec = new PBEKeySpec(pw.toCharArray(), salt, 65536, 512);
        SecretKeyFactory f = null;
        try {
            f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] hash = new byte[0];
        try {
            hash = f.generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        Base64.Encoder enc = Base64.getEncoder();
        System.out.printf("salt: %s%n", enc.encodeToString(salt));
        System.out.printf("hash: %s%n", enc.encodeToString(hash));
        System.out.println();

        return enc.encodeToString(hash);
    }

//    public void movieSociety(@RequestParam("name") String name, @RequestParam("pw") String pw){
//        if(login(name,pw)){
//            loggedIn = true;
//            System.out.println("authed");
//        }else{
//            loggedIn=false;
//            System.out.println("not authed");
//        }
//    }


    @PostMapping(value = "/login")
    public @ResponseBody
    String login(@RequestBody User user) {
        System.out.println("checking user: " + user.toString());
        if (Authorize(user.getName(), user.getPw())) {
            loggedIn = true;
            System.out.println("logged in");
            return "moviesociety.html";
        } else {
            System.out.println("not logged in");
            loggedIn = false;
            return "index.html";
        }

    }

    public boolean Authorize(String username, String pw) {
        String pwFromDb = DatabaseController.getInstance().fetchPw(username);
        pw = createPw(pw);
        if (pw.equals(pwFromDb))
            return true;
        return false;
    }

    @RequestMapping(value = "/storeauthcode", method = RequestMethod.POST)
    public String storeauthcode(@RequestBody String code, @RequestHeader("X-Requested-With") String encoding) {
        if (encoding == null || encoding.isEmpty()) {
            // Without the `X-Requested-With` header, this request could be forged. Aborts.
            return "Error, wrong headers";
        }

        GoogleTokenResponse tokenResponse = null;
        try {
            tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    new NetHttpTransport(),
                    JacksonFactory.getDefaultInstance(),
                    "https://www.googleapis.com/oauth2/v4/token",
                    CLIENT_ID,
                    CLIENT_SECRET,
                    code,
                    URL)
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Store these 3in your DB
        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();
        Long expiresAt = System.currentTimeMillis() + (tokenResponse.getExpiresInSeconds() * 1000);

        // Debug purpose only
        System.out.println("accessToken: " + accessToken);
        System.out.println("refreshToken: " + refreshToken);
        System.out.println("expiresAt: " + expiresAt);


        // Get profile info from ID token (Obtained at the last step of OAuth2)
        GoogleIdToken idToken = null;
        try {
            idToken = tokenResponse.parseIdToken();
        } catch (IOException e) {
            e.printStackTrace();
        }
        GoogleIdToken.Payload payload = idToken.getPayload();

        // Use THIS ID as a key to identify a google user-account.
        String userId = payload.getSubject();

        String email = payload.getEmail();
        boolean emailVerified = Boolean.valueOf(payload.getEmailVerified());
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");
        String locale = (String) payload.get("locale");
        String familyName = (String) payload.get("family_name");
        String givenName = (String) payload.get("given_name");

        User user = new User();
        user.setName(name);
        user.setMail(email);


        // Debugging purposes, should probably be stored in the database instead (At least "givenName").
        System.out.println("userId: " + userId);
        System.out.println("email: " + email);
        System.out.println("emailVerified: " + emailVerified);
        System.out.println("name: " + name);
        System.out.println("pictureUrl: " + pictureUrl);
        System.out.println("locale: " + locale);
        System.out.println("familyName: " + familyName);
        System.out.println("givenName: " + givenName);


        // Use an accessToken previously gotten to call Google's API
        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
        Calendar calendar =
                new Calendar.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance(), credential)
                        .setApplicationName("Movie Nights")
                        .build();

/*
  List the next 10 events from the primary calendar.
    Instead of printing these with System out, you should ofcourse store them in a database or in-memory variable to use for your application.
{1}
    The most important parts are:
    event.getSummary()             // Title of calendar event
    event.getStart().getDateTime() // Start-time of event
    event.getEnd().getDateTime()   // Start-time of event
    event.getStart().getDate()     // Start-date (without time) of event
    event.getEnd().getDate()       // End-date (without time) of event
{1}
    For more methods and properties, see: Google Calendar Documentation.
*/


        List<Event> eventsFromUser = new ArrayList<>();

        DateTime now = new DateTime(System.currentTimeMillis());
        Events events = null;
        try {
            events = calendar.events().list("primary")
                    .setMaxResults(20)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<com.google.api.services.calendar.model.Event> items = events.getItems();
        if (items.isEmpty()) {
            System.out.println("No upcoming events found.");
        } else {
            System.out.println("Upcoming events");
            for (com.google.api.services.calendar.model.Event event : items) {
                DateTime start = event.getStart().getDateTime();
                if (start == null) { // If it's an all-day-event - store the date instead
                    start = event.getStart().getDate();
                }
                DateTime end = event.getEnd().getDateTime();
                if (end == null) { // If it's an all-day-event - store the date instead
                    end = event.getStart().getDate();
                }
//                System.out.printf("%s (%s) -> (%s)\n", event.getSummary(), start, end);
                eventsFromUser.add(new Event(start + "," + end));
            }
        }

        adduser(name, "", email, eventsFromUser);

        getAvailableHours();

        return "OK";
    }

    @GetMapping("/getdates")
    public List<String> getAvailableHours() {
        List<User> ul = DatabaseController.getInstance().fetchListOfUsers();
        List<String> dates = makeDates();
        List<String> busyDates = new ArrayList<>();

        //get all busy dates from all users
        for (User u : ul) {
            for (Event e : u.getEvents()) {
                if (isViableTime(e)) {
                    busyDates.add(e.getStartDate());
                }
            }
        }

        //make a list of available dates
        for (int i = 0; i < dates.size(); i++) {
            for (String bd : busyDates) {
                if (bd.equals(dates.get(i))) {
                    dates.remove(i);
                }
            }
        }
        return dates;


    }

    private boolean isViableTime(Event e) {
        LocalDate today = LocalDate.now();

        LocalDate eDate = LocalDate.parse(e.getStartDate());
        int eStartTime = Integer.parseInt(e.getStartTime().substring(0, 2));
        int eEndTime = Integer.parseInt(e.getEndTime().substring(0, 2));

        boolean isEvening = eStartTime >= 18 && eStartTime <= 21 || eEndTime >= 19;
        boolean thisOrNextMonth = today.getMonthValue() == eDate.getMonthValue() || today.plusMonths(1).getMonthValue() == eDate.getMonthValue();
        return today.getYear() == eDate.getYear() && thisOrNextMonth && isEvening;

    }

    private List<String> makeDates() {
        List<String> dates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate todayplus = LocalDate.now().plusMonths(2);
        while (today.getMonthValue() < todayplus.getMonthValue()) {
            dates.add(today.toString());
            today = today.plusDays(1);
        }
        return dates;
    }

//    @RequestMapping(value="/bookdate", method=POST)
//    public String bookDate(@RequestBody String bookinginfo){
//        // Refer to the Java quickstart on how to setup the environment:
//// https://developers.google.com/calendar/quickstart/java
//// Change the scope to CalendarScopes.CALENDAR and delete any stored
//// credentials.
//
//        com.google.api.services.calendar.model.Event event = new com.google.api.services.calendar.model.Event()
//                .setSummary("Google I/O 2015")
//                .setLocation("800 Howard St., San Francisco, CA 94103")
//                .setDescription("A chance to hear more about Google's developer products.");
//
//        DateTime startDateTime = new DateTime("2015-05-28T09:00:00-07:00");
//        EventDateTime start = new EventDateTime()
//                .setDateTime(startDateTime)
//                .setTimeZone("America/Los_Angeles");
//        event.setStart(start);
//
//        DateTime endDateTime = new DateTime("2015-05-28T17:00:00-07:00");
//        EventDateTime end = new EventDateTime()
//                .setDateTime(endDateTime)
//                .setTimeZone("America/Los_Angeles");
//        event.setEnd(end);
//
//        String[] recurrence = new String[] {"RRULE:FREQ=DAILY;COUNT=2"};
//        event.setRecurrence(Arrays.asList(recurrence));
//
//        EventAttendee[] attendees = new EventAttendee[] {
//                new EventAttendee().setEmail("lpage@example.com"),
//                new EventAttendee().setEmail("sbrin@example.com"),
//        };
//        event.setAttendees(Arrays.asList(attendees));
//
//        EventReminder[] reminderOverrides = new EventReminder[] {
//                new EventReminder().setMethod("email").setMinutes(24 * 60),
//                new EventReminder().setMethod("popup").setMinutes(10),
//        };
//        com.google.api.services.calendar.model.Event.Reminders reminders = new com.google.api.services.calendar.model.Event.Reminders()
//                .setUseDefault(false)
//                .setOverrides(Arrays.asList(reminderOverrides));
//        event.setReminders(reminders);
//
//        String calendarId = "primary";
//        event = service.events().insert(calendarId, event).execute();
//        System.out.printf("Event created: %s\n", event.getHtmlLink());
//
//
//
//
//        return "OK";
//    }

}

