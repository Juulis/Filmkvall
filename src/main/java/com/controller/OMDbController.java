package com.controller;

import com.entities.Movie;
import com.entities.MovieFromOMDB;
import com.entities.SearchResult;
import com.app.DatabaseController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import java.util.Arrays;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class OMDbController {


    public OMDbController() {
    }

    @RequestMapping("/api/movie")
    public Movie findMovieById(@RequestParam(value = "id") String id) {

        //check if call exists in db, then get data from db instead.
        MovieFromOMDB movie = DatabaseController.getInstance().fetchMovie(id);
        if (movie != null) {
            System.out.println("fetching movie from db");
            return movie;
        }

        System.out.println("fetching from OMDB");
        RestTemplate restTemplate = new RestTemplate();
        String call = String.format("http://www.omdbapi.com/?i=%s&apikey=a5fc9d5d", id);
        MovieFromOMDB movieFromOMDB = restTemplate.getForObject(call, MovieFromOMDB.class);
        DatabaseController.getInstance().sendToDb(movieFromOMDB, "movies/" + id);
        return movieFromOMDB;
    }

    @RequestMapping("/api/movies")
    public Movie[] findMoviesByTitle(@RequestParam(value = "t") String t) {
        RestTemplate restTemplate = new RestTemplate();
        String call = String.format("http://www.omdbapi.com/?s=%s&apikey=a5fc9d5d", t);

        //check if call exists in db, then get data from db instead. if not, then save search in db
        Movie[] movies = DatabaseController.getInstance().fetchMovies(t);
        if (movies != null) {
            System.out.println("found");
            return movies;
        } else {
            System.out.println("fetching from OMDB");
            SearchResult movielist = restTemplate.getForObject(call, SearchResult.class);
            List<Movie> moviesToDb = Arrays.asList(movielist.getSearchArray());
            DatabaseController.getInstance().sendToDb(moviesToDb, "search/" + t);

            return movielist.getSearchArray();
        }
    }


}
