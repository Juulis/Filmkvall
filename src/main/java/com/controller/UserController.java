package com.controller;

import com.app.DatabaseController;
import com.entities.Event;
import com.entities.Movie;
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
    private Calendar calendar;

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

        String accessToken = tokenResponse.getAccessToken();

        // Get profile info from ID token (Obtained at the last step of OAuth2)
        GoogleIdToken idToken = null;
        try {
            idToken = tokenResponse.parseIdToken();
        } catch (IOException e) {
            e.printStackTrace();
        }
        GoogleIdToken.Payload payload = idToken.getPayload();

        String email = payload.getEmail();
        String name = (String) payload.get("name");

        User user = new User();
        user.setName(name);
        user.setMail(email);

        // Use an accessToken previously gotten to call Google's API
        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
        calendar =
                new Calendar.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance(), credential)
                        .setApplicationName("Movie Nights")
                        .build();

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

    @RequestMapping(value = "/bookdate", method = POST)
    public String bookDate(@RequestBody String bookinginfo) throws IOException {

        String date = bookinginfo.split(",")[0].replace(",","");
        String movieID = bookinginfo.split(",")[1];
        OMDbController mc = new OMDbController();
        Movie movie = mc.findMovieById(movieID);
        String title = movie.getTitle();

        com.google.api.services.calendar.model.Event event = new com.google.api.services.calendar.model.Event()
                .setSummary("Filmkväll!")
                .setLocation("I soffan!")
                .setDescription("Vi ska se på "+title);

        DateTime startDateTime = new DateTime(date+"T19:00:00+01:00");
        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone("Europe/Stockholm");
        event.setStart(start);

        DateTime endDateTime = new DateTime(date+"T23:00:00+01:00");
        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone("Europe/Stockholm");
        event.setEnd(end);

        List<User> users = DatabaseController.getInstance().fetchListOfUsers();
        EventAttendee[] attendees = new EventAttendee[users.size()];
        for (int i = 0; i < users.size(); i++) {
           attendees[i] = new EventAttendee().setEmail(users.get(i).getMail());
        }
        event.setAttendees(Arrays.asList(attendees));

        String calendarId = "primary";
        event = calendar.events().insert(calendarId, event).execute();
        System.out.printf("Event created: %s\n", event.getHtmlLink());


        return "OK";
    }

}

