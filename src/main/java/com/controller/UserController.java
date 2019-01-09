package com.controller;

import com.app.DatabaseController;
import com.entities.Event;
import com.entities.UserList;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.Json;
import com.google.api.client.json.JsonParser;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Data;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Events;
import com.entities.User;
import com.google.gson.Gson;
import org.apache.tomcat.util.json.JSONParser;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

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
                eventsFromUser.add(new Event(start+","+end));
            }
        }

        adduser(name, "", email, eventsFromUser);

        getAvailableHours();

        return "OK";
    }

    @GetMapping("/getusers")
    public String getAvailableHours() {
        //get all events from all users and put it in a string well formatted
        //YYYY-MM-DD-HH
        //then find available 3 hour windows between 18-22



        List<User> ul = DatabaseController.getInstance().fetchListOfUsers();


        System.out.println(ul.size());

        System.out.println(DatabaseController.getInstance().fetchEvents());
        LocalDate today = LocalDate.now();


        String[] availableDates = new String[14];

        for (int i = 0; i < availableDates.length; i++) {
            availableDates[i]=today.plusDays(i).toString();
            System.out.println(availableDates[i].toString());
        }



        return null;



    }

}
