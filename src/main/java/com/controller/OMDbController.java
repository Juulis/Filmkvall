package com.controller;

import com.app.NotFoundException;
import com.entities.Movie;
import com.entities.MovieFromOMDB;
import com.entities.SearchResult;
import com.app.DatabaseController;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class OMDbController {


    public OMDbController() {
    }

    @RequestMapping(value = "/api/movie", method = GET)
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
        movie = restTemplate.getForObject(call, MovieFromOMDB.class);
        if (movie.getTitle() == null)
            throw new NotFoundException();
        DatabaseController.getInstance().sendToDb(movie, "movies/" + id);
        return movie;
    }

    @RequestMapping(value = "/api/movies", method = GET)
    @ResponseBody
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
            if (movielist.getSearchArray() == null) {
                System.out.println("list empty");
                throw new NotFoundException();
            }
            System.out.println("found in OMDB");
            List<Movie> moviesToDb = Arrays.asList(movielist.getSearchArray());
            DatabaseController.getInstance().sendToDb(moviesToDb, "search/" + t);
            return movielist.getSearchArray();
        }
    }
}
